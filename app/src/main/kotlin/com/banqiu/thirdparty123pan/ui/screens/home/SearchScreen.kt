package com.banqiu.thirdparty123pan.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.ui.components.CloudEmpty
import com.banqiu.thirdparty123pan.ui.components.CloudError
import com.banqiu.thirdparty123pan.ui.components.CloudLoading
import com.banqiu.thirdparty123pan.ui.components.FileRow
import com.banqiu.thirdparty123pan.ui.components.GlassTopBar
import dev.chrisbanes.haze.HazeState

/**
 * 搜索页：全盘文件搜索（SearchData 参数）
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onOpenPreview: (FileItem) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hazeState = remember { HazeState() }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        if (query.length >= 1) {
            viewModel.search(query)
        }
    }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            hazeState = hazeState,
            title = "搜索",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索文件名…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        Box(Modifier.weight(1f)) {
            when {
                state.loading -> CloudLoading(text = "搜索中…")
                state.error != null && state.results.isEmpty() -> CloudError(
                    message = state.error ?: "加载失败",
                    onRetry = { viewModel.search(query) }
                )
                query.isBlank() -> CloudEmpty(
                    title = "输入关键词开始搜索",
                    subtitle = "支持搜索整个网盘的文件和文件夹"
                )
                state.results.isEmpty() -> CloudEmpty(
                    title = "未找到相关文件",
                    subtitle = "换个关键词试试"
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state.results, key = { it.fileId }) { item ->
                        FileRow(
                            item = item,
                            onClick = {
                                if (item.isFolder) {
                                    // 文件夹跳转到首页对应位置（简化：打开详情）
                                    onOpenFile(item)
                                } else if (item.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.IMAGE ||
                                    item.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.VIDEO
                                ) {
                                    onOpenPreview(item)
                                } else {
                                    onOpenFile(item)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}