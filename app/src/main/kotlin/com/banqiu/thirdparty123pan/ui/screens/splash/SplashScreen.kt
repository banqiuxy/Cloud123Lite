package com.banqiu.thirdparty123pan.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.banqiu.thirdparty123pan.ui.components.AppIcon
import com.banqiu.thirdparty123pan.ui.components.CloudBackground
import dev.chrisbanes.haze.HazeState

/**
 * 启动页：品牌展示 + 登录状态检查
 */
@Composable
fun SplashScreen(
    onNavigateLogin: () -> Unit,
    onNavigateMain: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val hazeState = remember { HazeState() }
    var started by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.6f,
        animationSpec = tween(600),
        label = "logo"
    )

    LaunchedEffect(Unit) {
        started = true
        viewModel.checkLogin()
        viewModel.isLoggedIn.collect { loggedIn ->
            if (loggedIn != null) {
                if (loggedIn) onNavigateMain() else onNavigateLogin()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        CloudBackground(hazeState)

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppIcon(
                modifier = Modifier
                    .size(88.dp)
                    .scale(logoScale)
            )
            Text(
                text = "Cloud123Lite",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                text = "轻量 · 无广告 · 流畅",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}