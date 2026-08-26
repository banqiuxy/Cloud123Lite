package com.banqiu.thirdparty123pan.di

import com.banqiu.thirdparty123pan.data.api.ApiService
import com.banqiu.thirdparty123pan.data.prefs.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 双 Session 架构（API.md §3）：
 * - API Session：业务接口，携带 authorization/loginuuid，支持域名容灾
 * - Transfer Session：CDN 下载 / S3 上传直连，不携带鉴权头，大连接池
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    const val HOST_PRIMARY = "www.123pan.cn"
    const val HOST_FALLBACK = "api.123278.com"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        // API 请求模型依赖默认字段（driveId=0/type=0/duplicate=0 等），
        // 必须显式编码，否则服务端会报“请输入DriveId”或“非法请求”。
        encodeDefaults = true
    }

    @Provides
    @Singleton
    @ApiHttpClient
    fun provideOkHttpClient(sessionManager: SessionManager): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .cookieJar(PersistentCookieJar(sessionManager))
            .addInterceptor(DomainFailoverInterceptor())
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logging)
            .build()
    }

    /** 传输专用连接池：pool_connections=16, pool_maxsize=32（对应 API.md §3） */
    @Provides
    @Singleton
    @TransferHttpClient
    fun provideTransferClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(@ApiHttpClient client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.123pan.cn/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)
}

/**
 * 认证拦截器（API.md §2 / §5.1）：
 * - 自动注入 authorization / loginuuid / Cookie / 客户端模拟头
 * - 自动捕获响应 Set-Cookie 并持久化（Python 客户端由 requests.Session 管理 Cookie）
 * - login.123pan.com 二维码接口使用专用头（跳过 Android 模拟头）
 */
class AuthInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val host = original.url.host
        val isLoginDomain = host == "login.123pan.com"

        // Do not set Accept-Encoding manually. OkHttp only transparently decompresses gzip
        // responses when it owns header negotiation; otherwise Retrofit sees raw bytes.
        val builder = original.newBuilder()
            .header("content-type", "application/json")

        if (isLoginDomain) {
            builder.header("platform", "web")
            builder.header("app-version", "3")
        } else {
            builder.header("platform", "android")
            builder.header("devicename", "Xiaomi")
            builder.header("app-version", "61")
            builder.header("x-app-version", "2.4.0")
        }

        val loginUuid = sessionManager.loginUuid
        if (loginUuid.isNotEmpty()) builder.header("loginuuid", loginUuid)

        val authorization = sessionManager.authorization
        if (authorization.isNotEmpty()) builder.header("authorization", authorization)

        // Cookie 仅由 PersistentCookieJar 根据服务器真实 Set-Cookie 管理。
        // 参考项目使用 requests.Session；不要手动拼接 token Cookie，避免覆盖
        // 或阻断服务器实际下发的会话 Cookie。

        val osVersion = sessionManager.osVersion
        val deviceType = sessionManager.deviceType
        if (osVersion.isNotEmpty()) builder.header("osversion", osVersion)
        if (deviceType.isNotEmpty()) builder.header("devicetype", deviceType)
        if (osVersion.isNotEmpty()) {
            builder.header("user-agent", "123pan/v2.4.0($osVersion;Xiaomi)")
        }

        val response = chain.proceed(builder.build())

        // 使用 OkHttp 官方 Cookie 解析器处理 Set-Cookie，兼容 Domain/Path/Expires
        // 以及一个响应头内包含多个 Cookie 的情况。
        val parsedCookies = Cookie.parseAll(response.request.url, response.headers)
        if (parsedCookies.isNotEmpty()) {
            sessionManager.mergeCookies(parsedCookies.map { "${it.name}=${it.value}" })
        } else {
            // 兼容部分服务端只返回标准 Set-Cookie 头的情况
            val setCookies = response.headers("Set-Cookie")
            if (setCookies.isNotEmpty()) sessionManager.mergeCookies(setCookies)
        }
        return response
    }
}

/**
 * 域名容灾切换（API.md §4）：
 * 仅对 www.123pan.cn 且路径含 /api/ 的请求生效；首次连接失败自动切换 api.123278.com
 */
class DomainFailoverInterceptor : Interceptor {

    @Volatile
    private var useFallback = false

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val shouldFailover = request.url.host == NetworkModule.HOST_PRIMARY &&
                request.url.encodedPath.contains("/api/")

        if (shouldFailover) {
            if (useFallback) {
                request = request.newBuilder()
                    .url(request.url.newBuilder().host(NetworkModule.HOST_FALLBACK).build())
                    .build()
                return chain.proceed(request)
            }
            try {
                return chain.proceed(request)
            } catch (e: IOException) {
                useFallback = true
                val fallback = request.newBuilder()
                    .url(request.url.newBuilder().host(NetworkModule.HOST_FALLBACK).build())
                    .build()
                return chain.proceed(fallback)
            }
        }
        return chain.proceed(request)
    }
}