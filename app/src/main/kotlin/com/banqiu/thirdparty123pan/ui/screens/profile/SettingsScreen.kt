package com.banqiu.thirdparty123pan.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banqiu.thirdparty123pan.data.prefs.SettingsStore
import com.banqiu.thirdparty123pan.data.prefs.ThemeMode
import com.banqiu.thirdparty123pan.ui.components.CloudBackground
import com.banqiu.thirdparty123pan.ui.components.ConfirmDialog
import com.banqiu.thirdparty123pan.ui.components.GlassTopBar
import com.banqiu.thirdparty123pan.ui.theme.CloudBlue
import com.banqiu.thirdparty123pan.ui.theme.CloudError
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

/**
 * 设置页
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsStore: SettingsStore = hiltViewModel<SettingsStoreHolder>().settingsStore
) {
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeMode by settingsStore.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val dynamicColor by settingsStore.dynamicColor.collectAsStateWithLifecycle(initialValue = false)
    val glassEnabled by settingsStore.glassEnabled.collectAsStateWithLifecycle(initialValue = true)
    val notificationsEnabled by settingsStore.notificationsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val concurrency by settingsStore.concurrency.collectAsStateWithLifecycle(initialValue = 3)
    val downloadDir by settingsStore.downloadDir.collectAsStateWithLifecycle(initialValue = "")

    var showClearCacheConfirm by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    Box(Modifier.fillMaxSize()) {
        CloudBackground(hazeState)

        Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                hazeState = hazeState,
                title = "设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 外观
                SectionTitle("外观")
                SettingsCard {
                    SettingsItem("主题模式", icon = Icons.Filled.Tune, value = themeModeLabel(themeMode)) {
                        val next = when (themeMode) {
                            ThemeMode.SYSTEM -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.SYSTEM
                        }
                        scope.launch { settingsStore.setThemeMode(next) }
                    }
                    SettingsSwitchItem(
                        "动态色彩",
                        icon = Icons.Filled.Palette,
                        checked = dynamicColor
                    ) { checked ->
                        scope.launch { settingsStore.setDynamicColor(checked) }
                    }
                    SettingsSwitchItem(
                        "液态玻璃效果",
                        icon = Icons.Filled.DarkMode,
                        checked = glassEnabled
                    ) { checked ->
                        scope.launch { settingsStore.setGlassEnabled(checked) }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 传输
                SectionTitle("传输")
                SettingsCard {
                    SettingsItem(
                        "并发任务数",
                        icon = Icons.Filled.Speed,
                        value = "$concurrency",
                        onClick = {
                            scope.launch {
                                settingsStore.setConcurrency(if (concurrency >= 3) 1 else concurrency + 1)
                            }
                        }
                    )
                    SettingsItem(
                        "下载位置",
                        icon = Icons.Filled.Download,
                        value = if (downloadDir.isBlank()) "应用目录 (Cloud123)" else "自定义目录",
                        onClick = {}
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 通知
                SectionTitle("通知")
                SettingsCard {
                    SettingsSwitchItem(
                        "任务完成通知",
                        icon = Icons.Filled.Notifications,
                        checked = notificationsEnabled
                    ) { checked ->
                        scope.launch { settingsStore.setNotificationsEnabled(checked) }
                        if (checked) {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 存储
                SectionTitle("存储")
                SettingsCard {
                    SettingsItem(
                        "清除缓存",
                        icon = Icons.Filled.DeleteSweep,
                        value = formatCacheSize(context),
                        onClick = { showClearCacheConfirm = true }
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showClearCacheConfirm) {
        ConfirmDialog(
            title = "清除缓存",
            message = "将清除图片缩略图等缓存文件，不影响已下载文件。",
            confirmText = "清除",
            destructive = true,
            onConfirm = { clearCache(context) },
            onDismiss = { showClearCacheConfirm = false }
        )
    }
}

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}

private fun formatCacheSize(context: android.content.Context): String {
    val cacheDir = context.cacheDir
    val size = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    return com.banqiu.thirdparty123pan.util.Formatters.formatSize(size)
}

private fun clearCache(context: android.content.Context) {
    val cacheDir = context.cacheDir
    cacheDir.listFiles()?.forEach { it.deleteRecursively() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingsItem(
    label: String,
    icon: ImageVector,
    value: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CloudBlue, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CloudBlue, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 轻量 Holder：通过 Hilt 获取 SettingsStore */
@dagger.hilt.android.lifecycle.HiltViewModel
class SettingsStoreHolder @javax.inject.Inject constructor(
    val settingsStore: SettingsStore
) : androidx.lifecycle.ViewModel()