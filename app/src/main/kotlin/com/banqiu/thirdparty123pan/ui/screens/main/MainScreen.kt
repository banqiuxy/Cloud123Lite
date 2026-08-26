package com.banqiu.thirdparty123pan.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banqiu.thirdparty123pan.data.prefs.SettingsStore
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.ui.components.BottomNavItem
import com.banqiu.thirdparty123pan.ui.components.CloudBackground
import com.banqiu.thirdparty123pan.ui.components.GlassBottomBar
import com.banqiu.thirdparty123pan.ui.screens.album.AlbumScreen
import com.banqiu.thirdparty123pan.ui.screens.home.HomeScreen
import com.banqiu.thirdparty123pan.ui.screens.profile.ProfileScreen
import com.banqiu.thirdparty123pan.ui.screens.transfer.TransferScreen
import dev.chrisbanes.haze.HazeState

/**
 * 主界面：底部玻璃导航（首页 / 传输 / 相册 / 我的）
 */
@Composable
fun MainScreen(
    onNavigateSearch: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateRecycleBin: () -> Unit,
    onNavigateShares: () -> Unit,
    onNavigateAbout: () -> Unit,
    onLogout: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onOpenPreview: (FileItem) -> Unit,
    settingsStore: SettingsStore = hiltViewModel<SettingsHolder>().settingsStore
) {
    val hazeState = remember { HazeState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val glassEnabled by settingsStore.glassEnabled.collectAsStateWithLifecycle(initialValue = true)

    val navItems = listOf(
        BottomNavItem("首页", Icons.Outlined.Folder, Icons.Filled.FolderOpen),
        BottomNavItem("传输", Icons.Outlined.Sync, Icons.Filled.Sync),
        BottomNavItem("相册", Icons.Outlined.PhotoLibrary, Icons.Filled.PhotoLibrary),
        BottomNavItem("我的", Icons.Outlined.Person, Icons.Filled.Person)
    )

    Box(Modifier.fillMaxSize()) {
        CloudBackground(hazeState)

        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        hazeState = hazeState,
                        glassEnabled = glassEnabled,
                        onNavigateSearch = onNavigateSearch,
                        onOpenFile = onOpenFile,
                        onOpenPreview = onOpenPreview
                    )
                    1 -> TransferScreen(hazeState = hazeState, glassEnabled = glassEnabled)
                    2 -> AlbumScreen(hazeState = hazeState, glassEnabled = glassEnabled)
                    else -> ProfileScreen(
                        hazeState = hazeState,
                        glassEnabled = glassEnabled,
                        onNavigateSettings = onNavigateSettings,
                        onNavigateRecycleBin = onNavigateRecycleBin,
                        onNavigateShares = onNavigateShares,
                        onNavigateAbout = onNavigateAbout,
                        onLogout = onLogout
                    )
                }
            }
            GlassBottomBar(
                hazeState = hazeState,
                items = navItems,
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                glassEnabled = glassEnabled
            )
        }
    }
}

/** 轻量 Holder：通过 Hilt 获取 SettingsStore */
@dagger.hilt.android.lifecycle.HiltViewModel
class SettingsHolder @javax.inject.Inject constructor(
    val settingsStore: SettingsStore
) : androidx.lifecycle.ViewModel()