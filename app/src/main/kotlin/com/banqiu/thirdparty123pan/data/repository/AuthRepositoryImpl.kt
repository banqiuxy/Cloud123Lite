package com.banqiu.thirdparty123pan.data.repository

import com.banqiu.thirdparty123pan.data.api.ApiService
import com.banqiu.thirdparty123pan.data.model.*
import com.banqiu.thirdparty123pan.data.prefs.SessionManager
import com.banqiu.thirdparty123pan.domain.model.User
import com.banqiu.thirdparty123pan.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser

    override suspend fun login(passport: String, password: String): User {
        sessionManager.ensureDeviceFingerprint()
        val resp = api.signIn(SignInRequest(passport = passport, password = password))
        val data = checkSuccess(resp, resp.data) ?: throw ApiException(resp.code, resp.message)

        // 参考项目固定使用 data.token 构造 Bearer authorization，
        // 不使用响应中可能不存在或格式不稳定的 authorization 字段。
        val cookieToken = data.token
            ?: throw ApiException(resp.code, "登录响应缺少 token")
        val authorization = "Bearer $cookieToken"
        saveSession(authorization, cookieToken)
        return refreshUser()
    }

    override suspend fun loginWithQrToken(token: String): User {
        sessionManager.ensureDeviceFingerprint()
        // 二维码接口只提供 token，按 API.md §2.5 作为后续会话令牌使用。
        saveSession(authorization = "Bearer $token", cookieToken = token)
        return refreshUser()
    }

    override fun importToken(token: String) {
        sessionManager.ensureDeviceFingerprint()
        saveSession(authorization = "Bearer $token", cookieToken = token)
    }

    private fun saveSession(authorization: String, cookieToken: String) {
        // authorization 使用 Bearer <token>，Cookie 由真实 Set-Cookie 进入 CookieJar。
        sessionManager.authorization = authorization
    }

    override suspend fun getUserInfo(): User {
        if (!sessionManager.isLoggedIn) throw ApiException(-1, "未登录")
        val resp = api.userInfo()
        val data = checkSuccess(resp, resp.data) ?: throw ApiException(resp.code, resp.message)
        val user = User(
            uid = data.uid ?: 0L,
            nickname = data.nickname ?: "用户${data.uid ?: ""}",
            avatar = data.avatar,
            vipLevel = data.vip?.level ?: 0,
            vipName = data.vip?.name ?: if ((data.vip?.level ?: 0) > 0) "VIP" else "普通用户",
            totalSpace = data.space,
            usedSpace = data.useSpace
        )
        _currentUser.value = user
        return user
    }

    private suspend fun refreshUser(): User = try {
        getUserInfo()
    } catch (e: Exception) {
        User(uid = 0, nickname = "已登录")
    }

    override suspend fun logout() {
        sessionManager.clearSession()
        _currentUser.value = null
    }

    private fun <T> checkSuccess(resp: ApiResponse<T>, data: T?): T? {
        if (!resp.isSuccess) throw ApiException(resp.code, resp.message.ifBlank { "请求失败 (${resp.code})" })
        return data
    }
}