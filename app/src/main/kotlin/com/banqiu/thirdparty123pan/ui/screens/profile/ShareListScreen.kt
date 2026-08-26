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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import com.banqiu.thirdparty123pan.domain.model.ShareItem
import com.banqiu.thirdparty123pan.ui.components.CloudBackground
import com.banqiu.thirdparty123pan.ui.components.CloudEmpty
import com.banqiu.thirdparty123pan.ui.components.CloudError
import com.banqiu.thirdparty123pan.ui.components.CloudLoading
import com.banqiu.thirdparty123pan.ui.components.ConfirmDialog
import com.banqiu.thirdparty123pan.ui.components.GlassTopBar
import com.banqiu.thirdparty123pan.ui.theme.CloudBlue
import com.banqiu.thirdparty123pan.ui.theme.CloudError
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

/**
 * 分享记录：查看、复制链接、删除分享
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ShareListScreen(
    onBack: () -> Unit,
    viewModel: ShareListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hazeState = remember { HazeState() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<ShareItem?>(null) }

    Box(Modifier.fillMaxSize()) {
        CloudBackground(hazeState)

        Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                hazeState = hazeState,
                title = "分享记录",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )

            Box(Modifier.weight(1f)) {
                when {
                    state.loading -> CloudLoading()
                    state.error != null && state.shares.isEmpty() -> CloudError(
                        message = state.error ?: "加载失败",
                        onRetry = viewModel::load
                    )
                    state.shares.isEmpty() -> CloudEmpty(
                        title = "暂无分享",
                        subtitle = "在文件页选择文件并分享后，记录会显示在这里",
                        icon = Icons.Filled.Link
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(state.shares, key = { it.shareId }) { share ->
                            ShareRow(
                                share = share,
                                onCopy = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(
                                            clipData = android.content.ClipData.newPlainText("Cloud123", share.shareUrl)
                                        )
                                    )
                                }
                            },
                                onDelete = { deleteTarget = share }
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { share ->
        ConfirmDialog(
            title = "取消分享",
            message = "删除后链接将失效，确定取消该分享吗？",
            confirmText = "取消分享",
            destructive = true,
            onConfirm = { viewModel.deleteShare(share.shareId) },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun ShareRow(
    share: ShareItem,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(CloudBlue.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Link, contentDescription = null, tint = CloudBlue, modifier = Modifier.size(20.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = share.shareName.ifBlank { "分享" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = share.shareUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (!share.sharePwd.isNullOrBlank()) {
                Text(
                    text = "提取码：${share.sharePwd}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CloudBlue,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "复制链接", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "取消分享", tint = CloudError)
        }
    }
}