package com.banqiu.thirdparty123pan.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banqiu.thirdparty123pan.ui.components.CloudBackground
import com.banqiu.thirdparty123pan.ui.components.GlassButton
import com.banqiu.thirdparty123pan.ui.components.QrCodeImage
import com.banqiu.thirdparty123pan.ui.theme.CloudBlue
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * 登录页：账号密码 / 扫码 / Token 导入
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hazeState = remember { HazeState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(state.success) {
        if (state.success) onLoginSuccess()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.stopQrPolling() }
    }

    Box(Modifier.fillMaxSize()) {
        CloudBackground(hazeState)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(72.dp))

            // 品牌区
            Text(
                text = "Cloud123",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "登录 123 云盘，享受无广告体验",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(36.dp))

            // 登录方式切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp)
            ) {
                LoginTab("账号密码", tab == 0, Modifier.weight(1f)) { tab = 0 }
                LoginTab("扫码登录", tab == 1, Modifier.weight(1f)) { tab = 1 }
                LoginTab("Token 导入", tab == 2, Modifier.weight(1f)) { tab = 2 }
            }

            Spacer(Modifier.height(24.dp))

            when (tab) {
                0 -> PasswordLoginContent(
                    loading = state.loading,
                    error = state.error,
                    onLogin = viewModel::login
                )
                1 -> QrLoginContent(
                    qrContent = state.qrContent,
                    qrStatus = state.qrStatus,
                    qrStatusText = state.qrStatusText,
                    onRefresh = viewModel::refreshQr,
                    onStart = viewModel::startQrLogin
                )
                else -> TokenImportContent(
                    loading = state.loading,
                    error = state.error,
                    onImport = viewModel::importToken
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun LoginTab(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(
                if (selected) CloudBlue else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PasswordLoginContent(
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit
) {
    var passport by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column {
        OutlinedTextField(
            value = passport,
            onValueChange = { passport = it },
            label = { Text("手机号 / 邮箱") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
        GlassButton(
            text = if (loading) "登录中…" else "登录",
            onClick = { onLogin(passport, password) },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}

@Composable
private fun QrLoginContent(
    qrContent: String?,
    qrStatus: QrStatus,
    qrStatusText: String,
    onRefresh: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (qrStatus) {
            QrStatus.IDLE -> {
                Text(
                    text = "使用 123云盘 App 扫码登录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                GlassButton(
                    text = "获取二维码",
                    onClick = onStart,
                    leadingIcon = Icons.Filled.QrCode,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
            }
            QrStatus.LOADING -> {
                CircularProgressIndicator(Modifier.size(48.dp), strokeWidth = 3.dp)
                Text(
                    text = "正在获取二维码…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            QrStatus.WAITING, QrStatus.SCANNED, QrStatus.CONFIRMED -> {
                if (qrContent != null) {
                    QrCodeImage(content = qrContent, size = 220.dp)
                }
                Text(
                    text = qrStatusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (qrStatus == QrStatus.SCANNED) CloudBlue
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "打开 123云盘 App → 设置 → 扫一扫",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                GlassButton(
                    text = "刷新二维码",
                    onClick = onRefresh,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )
            }
            QrStatus.EXPIRED, QrStatus.ERROR -> {
                Text(
                    text = qrStatusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                GlassButton(
                    text = "重新获取",
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
            }
        }
    }
}

@Composable
private fun TokenImportContent(
    loading: Boolean,
    error: String?,
    onImport: (String) -> Unit
) {
    var token by rememberSaveable { mutableStateOf("") }

    Column {
        Text(
            text = "粘贴 authorization Token 或 Cookie 快速登录",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Token / Cookie") },
            leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null) },
            minLines = 3,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
        GlassButton(
            text = if (loading) "导入中…" else "导入并登录",
            onClick = { onImport(token) },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Token 仅保存在本机加密存储中",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}