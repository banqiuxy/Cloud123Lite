package com.banqiu.thirdparty123pan.ui.screens.transfer

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banqiu.thirdparty123pan.domain.model.TaskStatus
import com.banqiu.thirdparty123pan.domain.model.TaskType
import com.banqiu.thirdparty123pan.domain.model.TransferTask
import com.banqiu.thirdparty123pan.ui.components.CloudEmpty
import com.banqiu.thirdparty123pan.ui.components.GlassButton
import com.banqiu.thirdparty123pan.ui.components.GlassTopBar
import com.banqiu.thirdparty123pan.ui.theme.CloudBlue
import com.banqiu.thirdparty123pan.ui.theme.CloudError
import com.banqiu.thirdparty123pan.ui.theme.CloudSuccess
import com.banqiu.thirdparty123pan.ui.theme.CloudWarning
import com.banqiu.thirdparty123pan.util.Formatters
import dev.chrisbanes.haze.HazeState

/**
 * 传输页：上传中 / 下载中 / 已完成 / 失败
 */
@Composable
fun TransferScreen(
    hazeState: HazeState,
    glassEnabled: Boolean,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    // Tab: 0 进行中(上传+下载), 1 已完成, 2 失败
    val active = tasks.filter { it.status == TaskStatus.WAITING || it.status == TaskStatus.RUNNING || it.status == TaskStatus.PAUSED }
    val finished = tasks.filter { it.status == TaskStatus.SUCCESS || it.status == TaskStatus.CANCELED }
    val failed = tasks.filter { it.status == TaskStatus.FAILED }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            hazeState = hazeState,
            glassEnabled = glassEnabled,
            title = "传输管理",
            actions = {
                if (tab == 1) {
                    IconButton(onClick = viewModel::clearFinished) {
                        Icon(Icons.Filled.Delete, contentDescription = "清除已完成")
                    }
                }
            }
        )

        // 统计概览
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TransferStat("上传中", active.count { it.type == TaskType.UPLOAD }, Icons.Filled.Upload, CloudBlue, Modifier.weight(1f))
            TransferStat("下载中", active.count { it.type == TaskType.DOWNLOAD }, Icons.Filled.Download, CloudSuccess, Modifier.weight(1f))
            TransferStat("已完成", finished.count { it.status == TaskStatus.SUCCESS }, Icons.Filled.CheckCircle, Color(0xFF5B8DEF), Modifier.weight(1f))
            TransferStat("失败", failed.size, Icons.Filled.ErrorOutline, CloudError, Modifier.weight(1f))
        }

        // Tab 切换
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    RoundedCornerShape(14.dp)
                )
                .padding(4.dp)
        ) {
            TransferTab("进行中", tab == 0, Modifier.weight(1f)) { tab = 0 }
            TransferTab("已完成", tab == 1, Modifier.weight(1f)) { tab = 1 }
            TransferTab("失败", tab == 2, Modifier.weight(1f)) { tab = 2 }
        }

        Spacer(Modifier.height(8.dp))

        val visible = when (tab) {
            0 -> active
            1 -> finished
            else -> failed
        }

        Box(Modifier.weight(1f)) {
            when {
                visible.isEmpty() -> CloudEmpty(
                    title = when (tab) {
                        0 -> "没有进行中的任务"
                        1 -> "暂无已完成任务"
                        else -> "一切顺利，没有失败任务"
                    },
                    subtitle = when (tab) {
                        0 -> "从文件页发起上传或下载"
                        1 -> "完成的任务会显示在这里"
                        else -> "失败的任务可以点击重试"
                    }
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(visible, key = { it.id }) { task ->
                        TransferTaskRow(
                            task = task,
                            onPause = { viewModel.pause(task.id) },
                            onResume = { viewModel.resume(task.id) },
                            onCancel = { viewModel.cancel(task.id) },
                            onRetry = { viewModel.retry(task.id) },
                            onRemove = { viewModel.remove(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferStat(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                color = color,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun TransferTab(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TransferTaskRow(
    task: TransferTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit
) {
    val isUpload = task.type == TaskType.UPLOAD
    val icon = if (isUpload) Icons.Filled.Upload else Icons.Filled.Download
    val iconColor = if (isUpload) CloudBlue else CloudSuccess

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconColor.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = statusText(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            // 操作按钮
            when (task.status) {
                TaskStatus.WAITING, TaskStatus.RUNNING -> {
                    if (task.status == TaskStatus.RUNNING) {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Filled.Pause, contentDescription = "暂停", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "继续", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Cancel, contentDescription = "取消", tint = CloudError)
                    }
                }
                TaskStatus.PAUSED -> {
                    IconButton(onClick = onResume) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "继续", tint = CloudBlue)
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Cancel, contentDescription = "取消", tint = CloudError)
                    }
                }
                TaskStatus.FAILED -> {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = "重试", tint = CloudWarning)
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (task.status == TaskStatus.WAITING || task.status == TaskStatus.RUNNING || task.status == TaskStatus.PAUSED) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { task.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .animateContentSize(),
                color = CloudBlue,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(task.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = CloudBlue
                )
                Text(
                    text = buildString {
                        append(Formatters.formatSpeed(task.speed))
                        if (task.status == TaskStatus.PAUSED) append(" · 已暂停")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (task.status == TaskStatus.FAILED && task.error != null) {
            Text(
                text = task.error,
                style = MaterialTheme.typography.bodySmall,
                color = CloudError,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private fun statusText(task: TransferTask): String = when (task.status) {
    TaskStatus.WAITING -> "等待中"
    TaskStatus.RUNNING -> if (task.type == TaskType.UPLOAD) "上传中" else "下载中"
    TaskStatus.PAUSED -> "已暂停"
    TaskStatus.SUCCESS -> "已完成 · ${Formatters.formatSize(task.size)}"
    TaskStatus.FAILED -> "失败"
    TaskStatus.CANCELED -> "已取消"
}