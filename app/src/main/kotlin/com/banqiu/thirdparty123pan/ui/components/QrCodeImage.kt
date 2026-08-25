package com.banqiu.thirdparty123pan.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/** 使用 zxing 将文本渲染为二维码图片 */
@Composable
fun QrCodeImage(
    content: String,
    size: Dp = 220.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(content) {
        runCatching {
            val pixelSize = 512
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, pixelSize, pixelSize)
            val pixels = IntArray(pixelSize * pixelSize)
            for (y in 0 until pixelSize) {
                for (x in 0 until pixelSize) {
                    pixels[y * pixelSize + x] = if (matrix[x, y]) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
                }
            }
            Bitmap.createBitmap(pixels, pixelSize, pixelSize, Bitmap.Config.ARGB_8888)
        }.getOrNull()
    }

    Box(
        modifier = modifier
            .size(size)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "二维码",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size - 20.dp)
            )
        }
    }
}