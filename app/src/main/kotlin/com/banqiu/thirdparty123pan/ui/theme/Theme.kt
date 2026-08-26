package com.banqiu.thirdparty123pan.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = CloudBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E8FF),
    onPrimaryContainer = Color(0xFF1A2C8A),
    secondary = Color(0xFF6C63FF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E7FF),
    onSecondaryContainer = Color(0xFF26206B),
    tertiary = Color(0xFF4CAF8D),
    background = LightBgStart,
    onBackground = LightOnSurface,
    surface = Color(0xFFF5F6FA),
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFE9EBF5),
    onSurfaceVariant = Color(0xFF5A5D6E),
    surfaceContainer = Color(0xFFEDEFF8),
    surfaceContainerHigh = Color(0xFFE7E9F4),
    surfaceContainerHighest = Color(0xFFE1E3EF),
    outline = Color(0xFFB9BDD0),
    outlineVariant = Color(0xFFD5D8E6),
    error = CloudError,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A)
)

private val DarkColors = darkColorScheme(
    primary = CloudBlueDark,
    onPrimary = Color(0xFF0B0B1A),
    primaryContainer = Color(0xFF2E3AB5),
    onPrimaryContainer = Color(0xFFE4E8FF),
    secondary = Color(0xFF9A9BFF),
    onSecondary = Color(0xFF12102F),
    secondaryContainer = Color(0xFF3A3680),
    onSecondaryContainer = Color(0xFFE9E7FF),
    tertiary = Color(0xFF5FD4A8),
    background = DarkBgStart,
    onBackground = DarkOnSurface,
    surface = Color(0xFF111318),
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF23252C),
    onSurfaceVariant = Color(0xFFB8BAC4),
    surfaceContainer = Color(0xFF15171D),
    surfaceContainerHigh = Color(0xFF1A1C23),
    surfaceContainerHighest = Color(0xFF21242C),
    outline = Color(0xFF3E4149),
    outlineVariant = Color(0xFF2A2D35),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

/**
 * Cloud123 主题：Material3 + 品牌主色，支持动态色彩与深浅模式
 */
@Composable
fun Cloud123Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CloudTypography,
        shapes = CloudShapes,
        content = content
    )
}
