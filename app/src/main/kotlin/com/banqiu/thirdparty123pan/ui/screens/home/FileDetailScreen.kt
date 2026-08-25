package com.banqiu.thirdparty123pan.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.ui.components.CloudBackground
import com.banqiu.thirdparty123pan.ui.components.ConfirmDialog
import com.banqiu.thirdparty123pan.ui.components.FileTypeIcon
import com.banqiu.thirdparty123pan.ui.components.GlassButton
import com.banqiu.thirdparty123pan.ui.components.GlassTopBar
import com.banqiu.thirdparty123pan.ui.components.TextInputDialog
import com.banqiu.thirdparty123pan.ui.theme.CloudBlue
import com.banqiu.thirdparty123pan.ui.theme.CloudError
import com.banqiu.thirdparty123pan.util.Formatters
import dev.chrisbanes.haze.HazeState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 文件详情页
 */
@Composable
fun FileDetailScreen(
    fileId: Long,
    name: String,
    size: Long,
    modTime: Long,
    parentId: Long,
    isFolder: Boolean,
    onBack: () -> Unit,
    onOpenPreview: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val item = remember(fileId, name, size) {
        FileItem(
            fileId = fileId,
            parentId = parentId,
            name = name,
            isFolder = isFolder,
            size = size,
            etag = null,
            s3KeyFlag = null,
            updateTime = modTime,
            createTime = modTime
        )
    }

    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        CloudBackground(hazeState)

        Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                hazeState = hazeState,
                title = "文件详情",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))
                FileTypeIcon(item, size = 88)

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp),
                    maxLines = 2
                )

                Spacer(Modifier.height(28.dp))

                // 信息卡片
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    DetailRow("类型", if (item.isFolder) "文件夹" else item.category.label)
                    DetailRow("大小", if (item.isFolder) "--" else item.sizeText)
                    DetailRow("修改时间", item.updateTimeText)
                    DetailRow("文件 ID", item.fileId.toString())
                }

                Spacer(Modifier.height(24.dp))

                // 操作按钮
                if (!item.isFolder) {
                    GlassButton(
                        text = if (item.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.IMAGE) "预览图片" else "查看",
                        onClick = { onOpenPreview(item.fileId) },
                        leadingIcon = Icons.Filled.Image,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }
                GlassButton(
                    text = "下载到本地",
                    onClick = { viewModel.download(item, defaultDownloadPath(context, item.name)) },
                    containerColor = CloudBlue,
                    leadingIcon = Icons.Filled.Download,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    GlassButton(
                        text = "重命名",
                        onClick = { showRename = true },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        leadingIcon = Icons.Filled.Edit,
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                    Spacer(Modifier.size(12.dp))
                    GlassButton(
                        text = "删除",
                        onClick = { showDelete = true },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        leadingIcon = Icons.Filled.Delete,
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showRename) {
        TextInputDialog(
            title = "重命名",
            initialValue = item.name,
            onConfirm = { newName ->
                if (newName.isNotBlank()) {
                    viewModel.rename(item.fileId, newName)
                    onBack()
                }
                showRename = false
            },
            onDismiss = { showRename = false }
        )
    }

    if (showDelete) {
        ConfirmDialog(
            title = "删除文件",
            message = "确定将「${item.name}」移入回收站吗？",
            confirmText = "删除",
            destructive = true,
            onConfirm = {
                viewModel.enterSelectionMode()
                viewModel.toggleSelection(item.fileId)
                viewModel.deleteSelected(true)
                onBack()
            },
            onDismiss = { showDelete = false }
        )
    }
}

private val com.banqiu.thirdparty123pan.domain.model.FileCategory.label: String
    get() = when (this) {
        com.banqiu.thirdparty123pan.domain.model.FileCategory.FOLDER -> "文件夹"
        com.banqiu.thirdparty123pan.domain.model.FileCategory.IMAGE -> "图片"
        com.banqiu.thirdparty123pan.domain.model.FileCategory.VIDEO -> "视频"
        com.banqiu.thirdparty123pan.domain.model.FileCategory.AUDIO -> "音频"
        com.banqiu.thirdparty123pan.domain.model.FileCategory.DOC -> "文档"
        com.banqiu.thirdparty123pan.domain.model.FileCategory.OTHER -> "文件"
    }

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.65f)
        )
    }
}