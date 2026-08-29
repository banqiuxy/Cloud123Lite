package com.banqiu.thirdparty123pan.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.banqiu.thirdparty123pan.ui.components.CloudBackground
import com.banqiu.thirdparty123pan.ui.components.CloudEmpty
import com.banqiu.thirdparty123pan.ui.components.CloudError
import com.banqiu.thirdparty123pan.ui.components.CloudLoading
import com.banqiu.thirdparty123pan.ui.components.ConfirmDialog
import com.banqiu.thirdparty123pan.ui.components.FileRow
import com.banqiu.thirdparty123pan.ui.components.GlassTopBar
import com.banqiu.thirdparty123pan.ui.theme.CloudError
import dev.chrisbanes.haze.HazeState

/**
 * 回收站：恢复 / 彻底删除 / 清空
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hazeState = remember { HazeState() }
    var confirmTarget by remember { mutableStateOf<FileItem?>(null) }
    var confirmPermanent by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        CloudBackground(hazeState)

        Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                hazeState = hazeState,
                title = "回收站",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.files.isNotEmpty()) {
                        IconButton(onClick = { confirmClearAll = true }) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = "清空回收站", tint = CloudError)
                        }
                    }
                }
            )

            Box(Modifier.weight(1f)) {
                when {
                    state.loading -> CloudLoading()
                    state.error != null && state.files.isEmpty() -> CloudError(
                        message = state.error ?: "加载失败",
                        onRetry = viewModel::load
                    )
                    state.files.isEmpty() -> CloudEmpty(
                        title = "回收站是空的",
                        subtitle = "删除的文件会保留在这里，可随时恢复"
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(state.files, key = { it.fileId }) { item ->
                            FileRow(
                                item = item,
                                onClick = {},
                                onMoreClick = { confirmTarget = item }
                            )
                        }
                    }
                }
            }
        }
    }

    confirmTarget?.let { item ->
        ModalBottomSheet(onDismissRequest = { confirmTarget = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.restore(listOf(item.fileId))
                            confirmTarget = null
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.RestoreFromTrash, null, tint = MaterialTheme.colorScheme.primary)
                    Text("恢复文件", modifier = Modifier.padding(start = 16.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            confirmPermanent = true
                            confirmTarget = null
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.DeleteForever, null, tint = CloudError)
                    Text("彻底删除", color = CloudError, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }

    if (confirmPermanent) {
        ConfirmDialog(
            title = "彻底删除",
            message = "删除后不可恢复，确定继续吗？",
            confirmText = "彻底删除",
            destructive = true,
            onConfirm = {
                confirmTarget?.let { viewModel.deletePermanently(listOf(it.fileId)) }
                confirmTarget = null
            },
            onDismiss = { confirmPermanent = false }
        )
    }

    if (confirmClearAll) {
        ConfirmDialog(
            title = "清空回收站",
            message = "将永久删除回收站中的全部文件，此操作不可撤销。",
            confirmText = "清空",
            destructive = true,
            onConfirm = {
                confirmClearAll = false
                viewModel.clearAll()
            },
            onDismiss = { confirmClearAll = false }
        )
    }
}