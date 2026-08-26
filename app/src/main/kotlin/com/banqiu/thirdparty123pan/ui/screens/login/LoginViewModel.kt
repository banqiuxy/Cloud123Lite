package com.banqiu.thirdparty123pan.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banqiu.thirdparty123pan.data.api.ApiService
import com.banqiu.thirdparty123pan.data.model.QrGenerateData
import com.banqiu.thirdparty123pan.data.model.QrResultData
import com.banqiu.thirdparty123pan.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QrStatus { IDLE, LOADING, WAITING, SCANNED, CONFIRMED, EXPIRED, ERROR }

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val qrUniId: String? = null,
    val qrContent: String? = null,
    val qrStatus: QrStatus = QrStatus.IDLE,
    val qrStatusText: String = "",
    val success: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private var qrPollJob: Job? = null

    /** 账号密码登录 */
    fun login(passport: String, password: String) {
        if (passport.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "请输入账号和密码") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                authRepository.login(passport.trim(), password)
                _uiState.update { it.copy(loading = false, success = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loading = false, error = e.message ?: "登录失败，请检查网络")
                }
            }
        }
    }

    /** 获取二维码并开始轮询 */
    fun startQrLogin() {
        qrPollJob?.cancel()
        _uiState.update { it.copy(qrStatus = QrStatus.LOADING, error = null) }
        viewModelScope.launch {
            try {
                val resp = api.qrGenerate()
                val data: QrGenerateData = resp.data ?: run {
                    _uiState.update { it.copy(qrStatus = QrStatus.ERROR, qrStatusText = resp.message.ifBlank { "获取二维码失败" }) }
                    return@launch
                }
                val uniId = data.uniId ?: run {
                    _uiState.update { it.copy(qrStatus = QrStatus.ERROR, qrStatusText = "二维码响应异常") }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        qrUniId = uniId,
                        qrContent = data.url ?: uniId,
                        qrStatus = QrStatus.WAITING,
                        qrStatusText = "请使用 123云盘 App 扫码登录"
                    )
                }
                pollQrResult(uniId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(qrStatus = QrStatus.ERROR, qrStatusText = e.message ?: "获取二维码失败")
                }
            }
        }
    }

    private fun pollQrResult(uniId: String) {
        qrPollJob = viewModelScope.launch {
            var expired = false
            while (!expired) {
                delay(2000)
                try {
                    val resp = api.qrResult(uniId)
                    if (resp.code == 200) {
                        // 已确认登录，token 在 data.token
                        val token = resp.data?.token
                        if (!token.isNullOrEmpty()) {
                            _uiState.update { it.copy(qrStatus = QrStatus.CONFIRMED, qrStatusText = "登录成功") }
                            authRepository.loginWithQrToken(token)
                            _uiState.update { it.copy(success = true) }
                            return@launch
                        }
                        expired = true
                        _uiState.update { it.copy(qrStatus = QrStatus.EXPIRED, qrStatusText = "二维码已失效，请刷新") }
                    } else {
                        val data: QrResultData? = resp.data
                        when (data?.loginStatus ?: 0) {
                            1 -> _uiState.update { it.copy(qrStatus = QrStatus.SCANNED, qrStatusText = "已扫码，请在手机上确认") }
                            2 -> _uiState.update { it.copy(qrStatus = QrStatus.ERROR, qrStatusText = "已拒绝登录") }
                            3 -> {
                                // 确认登录但 code 非 200：继续轮询拿 token
                                _uiState.update { it.copy(qrStatus = QrStatus.SCANNED, qrStatusText = "确认中…") }
                            }
                            4 -> {
                                expired = true
                                _uiState.update { it.copy(qrStatus = QrStatus.EXPIRED, qrStatusText = "二维码已过期，请刷新") }
                            }
                            else -> Unit
                        }
                    }
                } catch (e: Exception) {
                    // 网络抖动继续轮询，不中断
                }
            }
        }
    }

    fun refreshQr() {
        startQrLogin()
    }

    fun stopQrPolling() {
        qrPollJob?.cancel()
    }

    /** 导入 Cookie/Token 登录 */
    fun importToken(raw: String) {
        if (raw.isBlank()) {
            _uiState.update { it.copy(error = "请输入 Token 或 Cookie") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                // 支持直接粘贴 Cookie（如 "authorization=xxx"）或纯 Token
                val token = extractToken(raw)
                if (token.isBlank()) {
                    _uiState.update { it.copy(loading = false, error = "未找到有效的 authorization 字段") }
                    return@launch
                }
                authRepository.importToken(token)
                _uiState.update { it.copy(loading = false, success = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "导入失败") }
            }
        }
    }

    private fun extractToken(raw: String): String {
        // Cookie 格式：authorization=xxx; ...
        val cookieMatch = Regex("(?i)authorization\\s*=\\s*([^;\\s]+)").find(raw)
        if (cookieMatch != null) return cookieMatch.groupValues[1]
        // JSON 格式
        val jsonMatch = Regex("\"authorization\"\\s*:\\s*\"([^\"]+)\"").find(raw)
        if (jsonMatch != null) return jsonMatch.groupValues[1]
        return raw.trim()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        qrPollJob?.cancel()
    }
}