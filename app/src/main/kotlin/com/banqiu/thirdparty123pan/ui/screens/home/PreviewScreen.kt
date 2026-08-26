package com.banqiu.thirdparty123pan.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.ui.components.GlassTopBar
import dev.chrisbanes.haze.HazeState

/**
 * 图片预览页：全屏大图浏览（Coil 加载）
 */
@Composable
fun PreviewScreen(
    fileId: Long,
    name: String,
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val hazeState = remember { HazeState() }
    var url by remember(fileId) { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val item = remember(fileId, name) {
        FileItem(
            fileId = fileId,
            parentId = 0,
            name = name,
            isFolder = false,
            size = 0,
            etag = null,
            s3KeyFlag = null,
            updateTime = 0,
            createTime = 0
        )
    }

    LaunchedEffect(fileId) {
        if (url == null) {
            try {
                url = viewModel.resolveUrlForPreview(item)
            } catch (e: Exception) {
                error = e.message ?: "加载失败"
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            hazeState = hazeState,
            title = name,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when {
                url != null -> AsyncImage(
                    model = url,
                    contentDescription = name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                error != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.BrokenImage,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(12.dp)
                    )
                    Text(
                        text = error ?: "",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
        }
    }
}