package com.banqiu.thirdparty123pan.data.transfer

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * 下载链接重写机制（API.md §6）：
 * - 将 CDN 下载链接重写为 web-pro2.123952.com 代理格式，绕过 5113/5114 流量超限
 * - download-v2 格式直接 base64 解码 params 得到真实链接
 * - HTML href / JSON 重定向解析
 */
object DownloadUrlRewriter {

    private const val PROXY_HOST = "https://web-pro2.123952.com"

    private val hrefRegex = Regex("href=['\"](https?://[^'\"]+)['\"]")
    private val json = Json { ignoreUnknownKeys = true }

    /** 重写为代理格式 */
    fun rewrite(url: String): String {
        val encoded = Base64.encodeToString(url.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        return "$PROXY_HOST/download-v2/?params=$encoded&is_s3=0"
    }

    /**
     * 解析最终可直连的下载链接：
     * 1. download-v2 格式 → base64 解码 params
     * 2. 否则 GET 请求探测（HTML href / JSON 重定向）
     */
    suspend fun resolve(rawUrl: String, client: OkHttpClient): String {
        if (rawUrl.contains("download-v2")) {
            val params = rawUrl.substringAfter("params=").substringBefore("&")
            return try {
                String(Base64.decode(params, Base64.URL_SAFE or Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e: Exception) {
                rawUrl
            }
        }

        val request = Request.Builder()
            .url(rawUrl)
            .header("Range", "bytes=0-0")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return rawUrl
                val body = response.body?.string() ?: return rawUrl

                // JSON 重定向
                if (body.trimStart().startsWith("{")) {
                    try {
                        val obj = json.parseToJsonElement(body).jsonObject
                        val redirect = obj["url"]?.jsonPrimitive?.content
                            ?: obj["DownloadUrl"]?.jsonPrimitive?.content
                            ?: obj["downloadUrl"]?.jsonPrimitive?.content
                        if (!redirect.isNullOrEmpty()) return redirect
                    } catch (e: Exception) {
                        // fall through
                    }
                }

                // HTML 中提取 href
                val match = hrefRegex.find(body)
                if (match != null) return match.groupValues[1]

                return rawUrl
            }
        } catch (e: IOException) {
            return rawUrl
        }
    }
}