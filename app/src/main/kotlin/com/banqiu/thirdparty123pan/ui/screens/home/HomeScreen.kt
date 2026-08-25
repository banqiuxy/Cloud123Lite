package com.banqiu.thirdparty123pan.ui.screens.home

import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.domain.repository.FileOrderBy
import com.banqiu.thirdparty123pan.domain.repository.FileOrderDirection
import com.banqiu.thirdparty123pan.ui.components.CloudEmpty
import com.banqiu.thirdparty123pan.ui.components.CloudError
import com.banqiu.thirdparty123pan.ui.components.CloudLoading
import com.banqiu.thirdparty123pan.ui.components.ConfirmDialog
import com.banqiu.thirdparty123pan.ui.components.FileRow
import com.banqiu.thirdparty123pan.ui.components.FolderPickerDialog
import com.banqiu.thirdparty123pan.ui.components.GlassButton
import com.banqiu.thirdparty123pan.ui.components.GlassCard
import com.banqiu.thirdparty123pan.ui.components.GlassFab
import com.banqiu.thirdparty123pan.ui.components.GlassTopBar
import com.banqiu.thirdparty123pan.ui.components.TextInputDialog
import com.banqiu.thirdparty123pan.ui.theme.CloudBlue
import com.banqiu.thirdparty123pan.ui.theme.CloudError
import dev.chrisbanes.haze.HazeState
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch

/**
 * 首页：文件浏览器
 */
data class PickerState(
    val parentId: Long = 0,
    val currentName: String = "我的网盘",
    val crumbs: List<FileItem> = emptyList()
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    hazeState: HazeState,
    glassEnabled: Boolean,
    onNavigateSearch: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onOpenPreview: (FileItem) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val files = viewModel.filteredFiles
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val listState = rememberLazyListState()

    // 对话框状态
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMovePicker by remember { mutableStateOf(false) }
    var showCopyPicker by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var sharePassword by remember { mutableStateOf("") }
    var shareDays by remember { mutableStateOf(30) }
    var itemMenuTarget by remember { mutableStateOf<FileItem?>(null) }

    // 文件夹选择器状态（移动/复制目标）
    var movePickerState by remember { mutableStateOf(PickerState()) }
    var copyPickerState by remember { mutableStateOf(PickerState()) }
    var pickerFolders by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var pickerLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadPickerFolders(parentId: Long, onResult: () -> Unit = {}) {
        scope.launch {
            pickerLoading = true
            try {
                pickerFolders = viewModel.listFolders(parentId)
            } catch (e: Exception) {
                pickerFolders = emptyList()
            }
            pickerLoading = false
            onResult()
        }
    }

    LaunchedEffect(movePickerState.parentId) {
        if (showMovePicker) loadPickerFolders(movePickerState.parentId)
    }
    LaunchedEffect(copyPickerState.parentId) {
        if (showCopyPicker) loadPickerFolders(copyPickerState.parentId)
    }

    // 分页加载
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { last ->
                if (last != null && last >= state.files.size - 5) {
                    viewModel.loadMore()
                }
            }
    }

    // 分享结果展示
    val shareResult by viewModel.shareUrl.collectAsStateWithLifecycle()
    var showShareResult by remember { mutableStateOf(false) }
    LaunchedEffect(shareResult) {
        if (shareResult != null) showShareResult = true
    }

    // 上传文件选择器
    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.upload(copyUriToCache(context, uri), queryDisplayName(context, uri))
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 顶部栏
        GlassTopBar(
            hazeState = hazeState,
            glassEnabled = glassEnabled,
            title = if (state.selectionMode) "已选 ${state.selectedIds.size} 项"
            else state.breadcrumbs.lastOrNull()?.name ?: "我的网盘",
            navigationIcon = if (state.selectionMode) {
                {
                    IconButton(onClick = viewModel::exitSelectionMode) {
                        Icon(Icons.Filled.Close, contentDescription = "取消选择")
                    }
                }
            } else null,
            actions = {
                if (!state.selectionMode) {
                    IconButton(onClick = onNavigateSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                    SortMenu(
                        orderBy = state.orderBy,
                        orderDirection = state.orderDirection,
                        onOrderBy = viewModel::setOrderBy,
                        onOrderDirection = viewModel::setOrderDirection
                    )
                }
            }
        )

        // 面包屑
        if (state.breadcrumbs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "全部",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state.breadcrumbs.isEmpty()) CloudBlue
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { viewModel.navigateToRoot() }
                )
                state.breadcrumbs.forEach { crumb ->
                    Text(
                        text = " / ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = crumb.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable { }
                    )
                }
            }
        }

        // 筛选 chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                FileFilter.ALL to "全部",
                FileFilter.IMAGE to "图片",
                FileFilter.VIDEO to "视频",
                FileFilter.DOC to "文档",
                FileFilter.AUDIO to "音频"
            ).forEach { (filter, label) ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { viewModel.setFilter(filter) },
                    label = { Text(label) }
                )
            }
        }

        // 内容
        Box(Modifier.weight(1f)) {
            when {
                state.loading && state.files.isEmpty() -> CloudLoading()
                state.error != null && state.files.isEmpty() -> CloudError(
                    message = state.error ?: "加载失败",
                    onRetry = { viewModel.refresh() }
                )
                files.isEmpty() -> CloudEmpty(
                    title = "这里空空如也",
                    subtitle = "点击右下角按钮上传文件或新建文件夹"
                )
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(files, key = { it.fileId }) { item ->
                        val isSelected = item.fileId in state.selectedIds
                        FileRow(
                            item = item,
                            selected = isSelected,
                            selectionMode = state.selectionMode,
                            onClick = {
                                if (state.selectionMode) {
                                    viewModel.toggleSelection(item.fileId)
                                } else if (item.isFolder) {
                                    viewModel.navigateInto(item)
                                } else if (item.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.IMAGE ||
                                    item.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.VIDEO
                                ) {
                                    onOpenPreview(item)
                                } else {
                                    onOpenFile(item)
                                }
                            },
                            onLongClick = {
                                if (!state.selectionMode) {
                                    viewModel.enterSelectionMode()
                                    viewModel.toggleSelection(item.fileId)
                                }
                            },
                            onMoreClick = { itemMenuTarget = item }
                        )
                    }
                    if (state.loadingMore) {
                        item(key = "loading_more") {
                            Box(
                                Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ==================== FAB（上传 / 新建） ====================
    Box(Modifier.fillMaxSize()) {
        // 底部操作按钮
        if (state.selectionMode) {
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
                    SelectionAction("下载", Icons.Filled.Download) {
                        state.files.filter { it.fileId in state.selectedIds }.forEach {
                            viewModel.download(it, defaultDownloadPath(context, it.name))
                        }
                        viewModel.exitSelectionMode()
                    }
                    SelectionAction("分享", Icons.Filled.Share) { showShareSheet = true }
                    SelectionAction("移动", Icons.AutoMirrored.Filled.DriveFileMove) { showMovePicker = true }
                    SelectionAction("复制", Icons.Filled.ContentCopy) { showCopyPicker = true }
                    SelectionAction("删除", Icons.Filled.Delete, danger = true) { showDeleteConfirm = true }
                }
            }
        } else {
            GlassFab(
                icon = Icons.Filled.Upload,
                contentDescription = "上传",
                onClick = { showFabMenu = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            )
        }
    }

    // ==================== 对话框 ====================

    if (showFabMenu) {
        ModalBottomSheet(onDismissRequest = { showFabMenu = false }) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text("添加内容", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FabMenuItem(
                        label = "上传文件",
                        icon = Icons.Filled.Upload,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showFabMenu = false
                            uploadLauncher.launch(arrayOf("*/*"))
                        }
                    )
                    FabMenuItem(
                        label = "新建文件夹",
                        icon = Icons.Filled.CreateNewFolder,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showFabMenu = false
                            showNewFolderDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showNewFolderDialog) {
        TextInputDialog(
            title = "新建文件夹",
            placeholder = "文件夹名称",
            onConfirm = { name ->
                if (name.isNotBlank()) viewModel.createFolder(name)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }

    renameTarget?.let { target ->
        TextInputDialog(
            title = "重命名",
            initialValue = target.name,
            onConfirm = { newName ->
                if (newName.isNotBlank()) viewModel.rename(target.fileId, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "删除文件",
            message = "确定将选中的 ${state.selectedIds.size} 个文件移入回收站吗？",
            confirmText = "移入回收站",
            destructive = true,
            onConfirm = { viewModel.deleteSelected(true) },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showMovePicker) {
        FolderPickerDialog(
            title = "移动到",
            currentDirName = movePickerState.currentName,
            folders = pickerFolders,
            loading = pickerLoading,
            isRoot = movePickerState.parentId == 0L,
            confirmText = "移动到此处",
            onEnterFolder = { folder ->
                movePickerState = movePickerState.copy(
                    parentId = folder.fileId,
                    currentName = folder.name,
                    crumbs = movePickerState.crumbs + folder
                )
            },
            onBack = {
                val crumbs = movePickerState.crumbs.dropLast(1)
                movePickerState = movePickerState.copy(
                    parentId = crumbs.lastOrNull()?.fileId ?: 0L,
                    currentName = crumbs.lastOrNull()?.name ?: "我的网盘",
                    crumbs = crumbs
                )
            },
            onConfirm = {
                viewModel.moveSelected(movePickerState.parentId)
                showMovePicker = false
            },
            onDismiss = {
                showMovePicker = false
                movePickerState = PickerState()
            }
        )
    }

    if (showCopyPicker) {
        FolderPickerDialog(
            title = "复制到",
            currentDirName = copyPickerState.currentName,
            folders = pickerFolders,
            loading = pickerLoading,
            isRoot = copyPickerState.parentId == 0L,
            confirmText = "复制到此处",
            onEnterFolder = { folder ->
                copyPickerState = copyPickerState.copy(
                    parentId = folder.fileId,
                    currentName = folder.name,
                    crumbs = copyPickerState.crumbs + folder
                )
            },
            onBack = {
                val crumbs = copyPickerState.crumbs.dropLast(1)
                copyPickerState = copyPickerState.copy(
                    parentId = crumbs.lastOrNull()?.fileId ?: 0L,
                    currentName = crumbs.lastOrNull()?.name ?: "我的网盘",
                    crumbs = crumbs
                )
            },
            onConfirm = {
                viewModel.copySelected(copyPickerState.parentId)
                showCopyPicker = false
            },
            onDismiss = {
                showCopyPicker = false
                copyPickerState = PickerState()
            }
        )
    }

    if (showShareSheet) {
        ModalBottomSheet(onDismissRequest = { showShareSheet = false }) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text("分享文件", style = MaterialTheme.typography.titleLarge)
                Text(
                    "设置提取码与有效期",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(16.dp))
                TextInputDialogInline(
                    label = "提取码（可选）",
                    value = sharePassword,
                    onValueChange = { sharePassword = it },
                    placeholder = "4位提取码"
                )
                Spacer(Modifier.height(12.dp))
                Text("有效期", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(7, 30, 365, 0).forEach { days ->
                        FilterChip(
                            selected = shareDays == days,
                            onClick = { shareDays = days },
                            label = { Text(if (days == 0) "永久" else "${days}天") }
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                GlassButton(
                    text = "生成分享链接",
                    onClick = {
                        viewModel.shareSelected(sharePassword.takeIf { it.isNotBlank() }, shareDays)
                        showShareSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
            }
        }
    }

    if (showShareResult && shareResult != null) {
        ConfirmDialog(
            title = "分享成功",
            message = buildString {
                append("链接：${shareResult!!.url}\n")
                if (!shareResult!!.password.isNullOrBlank()) {
                    append("提取码：${shareResult!!.password}")
                }
            },
            confirmText = "复制链接",
            onConfirm = {
                scope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(
                            clipData = android.content.ClipData.newPlainText("Cloud123", shareResult!!.url)
                        )
                    )
                }
                viewModel.consumeShareResult()
            },
            onDismiss = {
                showShareResult = false
                viewModel.consumeShareResult()
            }
        )
    }

    // 单项更多菜单
    itemMenuTarget?.let { target ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { itemMenuTarget = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DropdownMenuItem(
                text = { Text("下载") },
                leadingIcon = { Icon(Icons.Filled.Download, null) },
                onClick = {
                    viewModel.download(target, defaultDownloadPath(context, target.name))
                    itemMenuTarget = null
                }
            )
            if (!target.isFolder) {
                DropdownMenuItem(
                    text = { Text("预览") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    onClick = {
                        onOpenPreview(target)
                        itemMenuTarget = null
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("重命名") },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = {
                    renameTarget = target
                    itemMenuTarget = null
                }
            )
            DropdownMenuItem(
                text = { Text("分享") },
                leadingIcon = { Icon(Icons.Filled.Share, null) },
                onClick = {
                    viewModel.enterSelectionMode()
                    viewModel.toggleSelection(target.fileId)
                    showShareSheet = true
                    itemMenuTarget = null
                }
            )
            DropdownMenuItem(
                text = { Text("移动") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
                onClick = {
                    viewModel.enterSelectionMode()
                    viewModel.toggleSelection(target.fileId)
                    showMovePicker = true
                    itemMenuTarget = null
                }
            )
            DropdownMenuItem(
                text = { Text("复制") },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                onClick = {
                    viewModel.enterSelectionMode()
                    viewModel.toggleSelection(target.fileId)
                    showCopyPicker = true
                    itemMenuTarget = null
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    viewModel.enterSelectionMode()
                    viewModel.toggleSelection(target.fileId)
                    showDeleteConfirm = true
                    itemMenuTarget = null
                }
            )
        }
    }
}

@Composable
private fun SortMenu(
    orderBy: FileOrderBy,
    orderDirection: FileOrderDirection,
    onOrderBy: (FileOrderBy) -> Unit,
    onOrderDirection: (FileOrderDirection) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "排序")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            listOf(
                FileOrderBy.FILE_ID to "默认排序",
                FileOrderBy.NAME to "按名称",
                FileOrderBy.SIZE to "按大小",
                FileOrderBy.UPDATE_TIME to "按时间"
            ).forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = {
                        if (orderBy == value) Icon(Icons.Filled.Check, null)
                    },
                    onClick = {
                        onOrderBy(value)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (orderDirection == FileOrderDirection.ASC) "升序 ↑" else "降序 ↓") },
                onClick = {
                    onOrderDirection(
                        if (orderDirection == FileOrderDirection.ASC) FileOrderDirection.DESC
                        else FileOrderDirection.ASC
                    )
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun SelectionAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (danger) CloudError else CloudBlue,
            modifier = Modifier.size(22.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (danger) CloudError else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TextInputDialogInline(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FabMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = CloudBlue,
            modifier = Modifier.size(30.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// ==================== 工具函数 ====================

fun defaultDownloadPath(context: android.content.Context, fileName: String): String {
    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "Cloud123")
    if (!dir.exists()) dir.mkdirs()
    return File(dir, fileName).absolutePath
}

fun copyUriToCache(context: android.content.Context, uri: Uri): String {
    val cacheDir = File(context.cacheDir, "uploads")
    if (!cacheDir.exists()) cacheDir.mkdirs()
    val displayName = queryDisplayName(context, uri)
    val dest = File(cacheDir, displayName)
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        }
    } catch (e: Exception) {
        // 复制失败则直接引用原 uri（如 content:// 无法转为 File 时任务会失败）
    }
    return dest.absolutePath
}

fun queryDisplayName(context: android.content.Context, uri: Uri): String {
    var name = uri.lastPathSegment ?: "upload_${System.currentTimeMillis()}"
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                cursor.getString(idx)?.let { name = it }
            }
        }
    }
    return name
}