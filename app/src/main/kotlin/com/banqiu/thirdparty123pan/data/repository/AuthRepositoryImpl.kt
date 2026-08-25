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

        // 参考实现（123panNextGen/123pan）：authorization = "Bearer " + token。
        // data.token 写入 Cookie；data.authorization（若有）作为请求头，缺失或格式不符时补 Bearer 前缀。
        val cookieToken = data.token
            ?: throw ApiException(resp.code, "登录响应缺少 token")
        val rawAuth = data.authorization
        val authorization = when {
            rawAuth.isNullOrBlank() -> "Bearer $cookieToken"
            rawAuth.startsWith("Bearer", ignoreCase = true) -> rawAuth
            else -> "Bearer $rawAuth"
        }
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
        sessionManager.authorization = authorization
        sessionManager.setLoginCookies(token = cookieToken)
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