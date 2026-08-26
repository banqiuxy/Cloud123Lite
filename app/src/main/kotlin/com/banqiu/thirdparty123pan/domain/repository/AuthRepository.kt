package com.banqiu.thirdparty123pan.domain.repository

import com.banqiu.thirdparty123pan.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(passport: String, password: String): User
    suspend fun loginWithQrToken(token: String): User
    fun importToken(token: String)
    suspend fun getUserInfo(): User
    suspend fun logout()
    val currentUser: Flow<User?>
} 