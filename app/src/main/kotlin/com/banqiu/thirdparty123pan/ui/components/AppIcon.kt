package com.banqiu.thirdparty123pan.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

/** 读取并显示清单中配置的应用图标，确保与桌面启动器图标保持一致。 */
@Composable
fun AppIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Cloud123Lite 应用图标"
) {
    val context = LocalContext.current
    val icon = remember(context) {
        context.applicationInfo
            .loadIcon(context.packageManager)
            .toBitmap(width = 512, height = 512, config = Bitmap.Config.ARGB_8888)
            .asImageBitmap()
    }

    Image(
        bitmap = icon,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}