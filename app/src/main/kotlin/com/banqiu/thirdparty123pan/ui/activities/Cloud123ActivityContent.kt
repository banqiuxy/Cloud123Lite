package com.banqiu.thirdparty123pan.ui.activities

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banqiu.thirdparty123pan.data.prefs.ThemeMode
import com.banqiu.thirdparty123pan.ui.theme.Cloud123Theme
import com.banqiu.thirdparty123pan.ui.viewmodel.ThemeViewModel

@Composable
fun Cloud123ActivityContent(content: @Composable () -> Unit) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by themeViewModel.dynamicColor.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    Cloud123Theme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}