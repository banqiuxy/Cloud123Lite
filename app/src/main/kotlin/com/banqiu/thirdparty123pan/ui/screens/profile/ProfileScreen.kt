package com.banqiu.thirdparty123pan.ui.screens.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banqiu.thirdparty123pan.domain.model.User
import com.banqiu.thirdparty123pan.ui.components.CloudEmpty
import com.banqiu.thirdparty123pan.ui.components.ConfirmDialog
import com.banqiu.thirdparty123pan.ui.components.GlassTopBar
import com.banqiu.thirdparty123pan.ui.theme.CloudBlue
import com.banqiu.thirdparty123pan.ui.theme.CloudError
import com.banqiu.thirdparty123pan.ui.theme.CloudSuccess
import com.banqiu.thirdparty123pan.util.Formatters
import dev.chrisbanes.haze.HazeState

/**
 * 我的页：用户信息、容量、入口列表
 */
@Composable
fun ProfileScreen(
    hazeState: HazeState,
    glassEnabled: Boolean,
    onNavigateSettings: () -> Unit,
    onNavigateRecycleBin: () -> Unit,
    onNavigateShares: () -> Unit,
    onNavigateAbout: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            hazeState = hazeState,
            glassEnabled = glassEnabled,
            title = "我的",
            actions = {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(22.dp)
                        .clickable(onClick = onNavigateSettings)
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 用户卡片
            UserCard(
                user = state.user,
                loading = state.loading,
                onRefresh = viewModel::refreshUser
            )

            // 用户信息加载失败时给出可见提示
            state.error?.let { err ->
                Text(
                    text = "用户信息加载失败：$err",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // 功能入口
            ProfileSection {
                ProfileItem("回收站", Icons.Filled.DeleteSweep, CloudError) { onNavigateRecycleBin() }
                ProfileItem("分享记录", Icons.Filled.Share, CloudBlue) { onNavigateShares() }
                ProfileItem("设置", Icons.Filled.Settings, Color(0xFF5B8DEF)) { onNavigateSettings() }
                ProfileItem("关于 Cloud123", Icons.Filled.Info, Color(0xFF7E6FF0)) { onNavigateAbout() }
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable { showLogoutConfirm = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "退出登录",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showLogoutConfirm) {
        ConfirmDialog(
            title = "退出登录",
            message = "确定要退出当前账号吗？传输中的任务会继续运行。",
            confirmText = "退出",
            destructive = true,
            onConfirm = { viewModel.logout(onDone = onLogout) },
            onDismiss = { showLogoutConfirm = false }
        )
    }
}

@Composable
private fun UserCard(user: User?, loading: Boolean, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(CloudBlue.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "头像",
                    tint = CloudBlue,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = user?.nickname ?: "用户 ${user?.uid ?: ""}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = user?.vipName ?: "加载中…",
                    style = MaterialTheme.typography.bodySmall,
                    color = if ((user?.vipLevel ?: 0) > 0) CloudSuccess
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "刷新",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onRefresh)
            )
        }

        // 容量使用
        if (user != null && user.totalSpace > 0) {
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "已用 ${Formatters.formatSize(user.usedSpace)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "共 ${Formatters.formatSize(user.totalSpace)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { user.usedPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = CloudBlue,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }
}

@Composable
private fun ProfileSection(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun ProfileItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}