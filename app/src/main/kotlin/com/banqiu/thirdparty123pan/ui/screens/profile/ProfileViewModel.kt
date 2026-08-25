package com.banqiu.thirdparty123pan.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banqiu.thirdparty123pan.domain.model.User
import com.banqiu.thirdparty123pan.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val loading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val loggingOut: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        refreshUser()
    }

    fun refreshUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val user = authRepository.getUserInfo()
                _uiState.update { it.copy(loading = false, user = user, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "获取用户信息失败") }
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loggingOut = true) }
            authRepository.logout()
            _uiState.update { it.copy(loggingOut = false) }
            onDone()
        }
    }
}