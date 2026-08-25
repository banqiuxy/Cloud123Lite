package com.banqiu.thirdparty123pan.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.banqiu.thirdparty123pan.domain.model.FileCategory
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.ui.theme.AudioColor
import com.banqiu.thirdparty123pan.ui.theme.CloudSuccess
import com.banqiu.thirdparty123pan.ui.theme.DocColor
import com.banqiu.thirdparty123pan.ui.theme.FolderColor
import com.banqiu.thirdparty123pan.ui.theme.ImageColor
import com.banqiu.thirdparty123pan.ui.theme.OtherColor
import com.banqiu.thirdparty123pan.ui.theme.ShapeMedium
import com.banqiu.thirdparty123pan.ui.theme.VideoColor

/** 根据文件类型返回图标与颜色 */
fun fileTypeIcon(item: FileItem): Pair<ImageVector, Color> = when (item.category) {
    FileCategory.FOLDER -> Icons.Filled.Folder to FolderColor
    FileCategory.IMAGE -> Icons.Filled.Image to ImageColor
    FileCategory.VIDEO -> Icons.Filled.Movie to VideoColor
    FileCategory.AUDIO -> Icons.Filled.Audiotrack to AudioColor
    FileCategory.DOC -> Icons.Filled.Description to DocColor
    FileCategory.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile to OtherColor
}

@Composable
fun FileTypeIcon(item: FileItem, size: Int = 44) {
    val (icon, color) = fileTypeIcon(item)
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = item.name,
            tint = color,
            modifier = Modifier.size((size * 0.55f).dp)
        )
    }
}

/**
 * 文件列表行
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    item: FileItem,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick ?: {}
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileTypeIcon(item)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.size(3.dp))
            Text(
                text = if (item.isFolder) "文件夹" else "${item.sizeText} · ${item.updateTimeText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (selectionMode) {
            val checked = selected
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = if (checked) "已选中" else "未选中",
                tint = if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        } else if (onMoreClick != null) {
            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "更多操作",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}