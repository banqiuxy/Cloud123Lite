package com.banqiu.thirdparty123pan.ui.screens.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.ui.components.CloudEmpty
import com.banqiu.thirdparty123pan.ui.components.CloudError
import com.banqiu.thirdparty123pan.ui.components.CloudLoading
import com.banqiu.thirdparty123pan.ui.components.GlassCard
import com.banqiu.thirdparty123pan.ui.components.GlassTopBar
import com.banqiu.thirdparty123pan.ui.screens.home.defaultDownloadPath
import com.banqiu.thirdparty123pan.ui.theme.CloudBlue
import com.banqiu.thirdparty123pan.ui.theme.CloudError
import com.banqiu.thirdparty123pan.ui.theme.ImageColor
import com.banqiu.thirdparty123pan.ui.theme.VideoColor
import dev.chrisbanes.haze.HazeState

/**
 * 相册页：图片/视频网格浏览（瀑布流），支持批量下载/删除
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumScreen(
    hazeState: HazeState,
    glassEnabled: Boolean,
    viewModel: AlbumViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val items = viewModel.visibleItems
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            hazeState = hazeState,
            glassEnabled = glassEnabled,
            title = if (state.selectionMode) "已选 ${state.selectedIds.size} 项" else state.currentDirName,
            navigationIcon = if (state.selectionMode) {
                {
                    IconButton(onClick = viewModel::exitSelection) {
                        Icon(Icons.Filled.Close, contentDescription = "取消选择")
                    }
                }
            } else null,
            actions = {
                if (!state.selectionMode) {
                    IconButton(onClick = viewModel::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回全部")
                    }
                }
            }
        )

        // 图片/视频 Tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AlbumTab("图片", state.showImages, Modifier.weight(1f)) {
                viewModel.setShowImages(true)
            }
            AlbumTab("视频", !state.showImages, Modifier.weight(1f)) {
                viewModel.setShowImages(false)
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                state.loading -> CloudLoading()
                state.error != null && items.isEmpty() -> CloudError(
                    message = state.error ?: "加载失败",
                    onRetry = { viewModel.loadDirectory(state.currentParentId, state.currentDirName) }
                )
                items.isEmpty() -> CloudEmpty(
                    title = if (state.showImages) "暂无图片" else "暂无视频",
                    subtitle = if (state.currentParentId != 0L) "此目录没有${if (state.showImages) "图片" else "视频"}"
                    else "上传到网盘后这里会展示${if (state.showImages) "图片" else "视频"}",
                    icon = if (state.showImages) Icons.Filled.Image else Icons.Filled.Movie
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.fileId }) { item ->
                        AlbumGridItem(
                            item = item,
                            selected = item.fileId in state.selectedIds,
                            selectionMode = state.selectionMode,
                            onClick = {
                                if (state.selectionMode) viewModel.toggleSelection(item.fileId)
                            },
                            onLongClick = {
                                if (!state.selectionMode) {
                                    viewModel.enterSelection()
                                    viewModel.toggleSelection(item.fileId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 批量操作栏
    if (state.selectionMode) {
        Box(Modifier.fillMaxSize()) {
            GlassCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                hazeState = hazeState,
                glassEnabled = glassEnabled,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(10.dp)
                            .combinedClickable(onClick = {
                                viewModel.downloadSelected { item, _ ->
                                    viewModel.addDownload(item, defaultDownloadPath(context, item.name))
                                }
                            })
                    ) {
                        Icon(Icons.Filled.Download, null, tint = CloudBlue, modifier = Modifier.size(22.dp))
                        Text("下载", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(10.dp)
                            .combinedClickable(onClick = {
                                viewModel.deleteSelected()
                            })
                    ) {
                        Icon(Icons.Filled.Delete, null, tint = CloudError, modifier = Modifier.size(22.dp))
                        Text("删除", style = MaterialTheme.typography.labelSmall, color = CloudError)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumTab(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                RoundedCornerShape(12.dp)
            )
            .combinedClickable(onClick = onClick)
            .padding(vertical = 10.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGridItem(
    item: FileItem,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var url by remember(item.fileId) { mutableStateOf<String?>(null) }
    val viewModel = androidx.hilt.navigation.compose.hiltViewModel<AlbumViewModel>()
    LaunchedEffect(item.fileId) {
        if (url == null) url = viewModel.resolveUrl(item)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (item.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.VIDEO)
                        Icons.Filled.Movie else Icons.Filled.Image,
                    contentDescription = null,
                    tint = if (item.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.VIDEO)
                        VideoColor else ImageColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 视频时长/类型角标
        if (item.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "视频",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // 选择标记
        if (selectionMode) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (selected) CloudBlue else Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
            )
        }
    }
}