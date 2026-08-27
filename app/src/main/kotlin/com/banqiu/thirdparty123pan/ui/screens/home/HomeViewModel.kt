package com.banqiu.thirdparty123pan.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.domain.repository.CopySource
import com.banqiu.thirdparty123pan.domain.repository.FileOrderBy
import com.banqiu.thirdparty123pan.domain.repository.FileOrderDirection
import com.banqiu.thirdparty123pan.domain.repository.FileRepository
import com.banqiu.thirdparty123pan.domain.repository.TransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FileFilter { ALL, IMAGE, VIDEO, DOC, AUDIO }

data class HomeUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val files: List<FileItem> = emptyList(),
    val currentParentId: Long = 0,
    val breadcrumbs: List<FileItem> = emptyList(),
    val orderBy: FileOrderBy = FileOrderBy.FILE_ID,
    val orderDirection: FileOrderDirection = FileOrderDirection.DESC,
    val filter: FileFilter = FileFilter.ALL,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val page: Int = 1,
    val busyMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val transferRepository: TransferRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val pageSize = 100

    init {
        loadFiles(0)
    }

    // ==================== 浏览 ====================

    /** 加载目录下的子文件夹（用于移动/复制选择器） */
    suspend fun listFolders(parentId: Long): List<FileItem> {
        return try {
            fileRepository.listFiles(parentId = parentId, page = 1, limit = 200)
                .filter { it.isFolder }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 解析文件直链（预览用） */
    suspend fun resolveUrlForPreview(item: FileItem): String? =
        fileRepository.resolveDownloadUrl(item).takeIf { it.isNotBlank() }

    fun loadFiles(parentId: Long = _uiState.value.currentParentId) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, page = 1) }
            try {
                val state = _uiState.value
                val list = fileRepository.listFiles(
                    parentId = parentId,
                    orderBy = state.orderBy,
                    orderDirection = state.orderDirection,
                    page = 1,
                    limit = pageSize
                )
                _uiState.update {
                    it.copy(
                        files = list,
                        currentParentId = parentId,
                        loading = false,
                        hasMore = list.size >= pageSize,
                        page = 2
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loading = false, error = e.message ?: "加载失败")
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, error = null) }
            try {
                val state = _uiState.value
                val list = fileRepository.listFiles(
                    parentId = state.currentParentId,
                    orderBy = state.orderBy,
                    orderDirection = state.orderDirection,
                    page = 1,
                    limit = pageSize
                )
                _uiState.update {
                    it.copy(
                        files = list,
                        refreshing = false,
                        hasMore = list.size >= pageSize,
                        page = 2
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(refreshing = false, error = e.message ?: "刷新失败") }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.loadingMore || !state.hasMore || state.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            try {
                val more = fileRepository.listFiles(
                    parentId = state.currentParentId,
                    orderBy = state.orderBy,
                    orderDirection = state.orderDirection,
                    page = state.page,
                    limit = pageSize
                )
                _uiState.update {
                    it.copy(
                        files = it.files + more.filterNot { f -> it.files.any { f2 -> f2.fileId == f.fileId } },
                        loadingMore = false,
                        hasMore = more.size >= pageSize,
                        page = it.page + 1
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingMore = false) }
            }
        }
    }

    fun navigateInto(folder: FileItem) {
        if (!folder.isFolder) return
        val newBreadcrumbs = _uiState.value.breadcrumbs + folder
        _uiState.update { it.copy(breadcrumbs = newBreadcrumbs) }
        loadFiles(folder.fileId)
    }

    fun navigateBack() {
        val state = _uiState.value
        if (state.currentParentId == 0L) return
        val newCrumbs = state.breadcrumbs.dropLast(1)
        val parentId = newCrumbs.lastOrNull()?.fileId ?: 0L
        _uiState.update { it.copy(breadcrumbs = newCrumbs) }
        loadFiles(parentId)
    }

    fun navigateToRoot() {
        _uiState.update { it.copy(breadcrumbs = emptyList()) }
        loadFiles(0)
    }

    // ==================== 排序 / 筛选 ====================

    fun setOrderBy(orderBy: FileOrderBy) {
        _uiState.update { it.copy(orderBy = orderBy) }
        loadFiles()
    }

    fun setOrderDirection(direction: FileOrderDirection) {
        _uiState.update { it.copy(orderDirection = direction) }
        loadFiles()
    }

    fun setFilter(filter: FileFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    val filteredFiles: List<FileItem>
        get() {
            val state = _uiState.value
            return when (state.filter) {
                FileFilter.ALL -> state.files
                FileFilter.IMAGE -> state.files.filter { !it.isFolder && it.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.IMAGE }
                FileFilter.VIDEO -> state.files.filter { !it.isFolder && it.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.VIDEO }
                FileFilter.DOC -> state.files.filter { !it.isFolder && it.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.DOC }
                FileFilter.AUDIO -> state.files.filter { !it.isFolder && it.category == com.banqiu.thirdparty123pan.domain.model.FileCategory.AUDIO }
            }
        }

    // ==================== 多选 ====================

    fun enterSelectionMode() {
        _uiState.update { it.copy(selectionMode = true, selectedIds = emptySet()) }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun toggleSelection(fileId: Long) {
        _uiState.update { state ->
            val selected = state.selectedIds.toMutableSet()
            if (!selected.add(fileId)) selected.remove(fileId)
            state.copy(selectedIds = selected)
        }
    }

    fun selectAll(currentList: List<FileItem>) {
        _uiState.update { it.copy(selectedIds = currentList.map { f -> f.fileId }.toSet()) }
    }

    // ==================== 文件操作 ====================

    fun createFolder(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyMessage = "创建中…") }
            try {
                fileRepository.createFolder(_uiState.value.currentParentId, name)
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(busyMessage = null, error = e.message ?: "创建失败") }
            } finally {
                _uiState.update { it.copy(busyMessage = null) }
            }
        }
    }

    fun rename(fileId: Long, newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyMessage = "重命名中…") }
            try {
                fileRepository.rename(fileId, newName)
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(busyMessage = null, error = e.message ?: "重命名失败") }
            } finally {
                _uiState.update { it.copy(busyMessage = null) }
            }
        }
    }

    fun deleteSelected(toTrash: Boolean = true) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyMessage = if (toTrash) "移入回收站…" else "删除中…") }
            try {
                fileRepository.trash(ids, toTrash)
                exitSelectionMode()
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(busyMessage = null, error = e.message ?: "操作失败") }
            } finally {
                _uiState.update { it.copy(busyMessage = null) }
            }
        }
    }

    fun moveSelected(targetParentId: Long) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyMessage = "移动中…") }
            try {
                fileRepository.move(ids, targetParentId)
                exitSelectionMode()
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(busyMessage = null, error = e.message ?: "移动失败") }
            } finally {
                _uiState.update { it.copy(busyMessage = null) }
            }
        }
    }

    fun copySelected(targetParentId: Long) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(busyMessage = "复制中…") }
            try {
                val files = _uiState.value.files
                val sources = ids.mapNotNull { id ->
                    files.find { it.fileId == id }?.let { CopySource(it.fileId, it.name) }
                }
                fileRepository.copy(sources, targetParentId)
                exitSelectionMode()
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(busyMessage = null, error = e.message ?: "复制失败") }
            } finally {
                _uiState.update { it.copy(busyMessage = null) }
            }
        }
    }

    fun shareSelected(
        password: String?,
        days: Int,
        onSuccess: (ShareResult) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) {
            onFailure("未选择要分享的文件")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busyMessage = "生成分享链接…") }
            try {
                val share = fileRepository.createShare(ids, password, days)
                val result = ShareResult(
                    url = share.url,
                    password = share.password
                )
                _uiState.update { it.copy(busyMessage = null) }
                _shareUrl.value = result
                onSuccess(result)
            } catch (e: Exception) {
                val message = e.message?.takeIf { it.isNotBlank() } ?: "分享失败"
                _uiState.update { it.copy(busyMessage = null, error = message) }
                onFailure(message)
            }
        }
    }

    private val _shareUrl = MutableStateFlow<ShareResult?>(null)
    val shareUrl: StateFlow<ShareResult?> = _shareUrl

    fun consumeShareResult() {
        _shareUrl.value = null
    }

    // ==================== 传输 ====================

    fun download(item: FileItem, destination: String) {
        viewModelScope.launch {
            // 列表中已有元数据时直接入队；详情页/搜索页可能丢失 Etag/S3KeyFlag，先回查父目录。
            val resolved = if (!item.etag.isNullOrBlank() && !item.s3KeyFlag.isNullOrBlank()) {
                item
            } else {
                runCatching {
                    fileRepository.listFiles(parentId = item.parentId, page = 1, limit = 200)
                        .firstOrNull { it.fileId == item.fileId }
                }.getOrNull() ?: item
            }

            if (resolved.etag.isNullOrBlank() || resolved.s3KeyFlag.isNullOrBlank()) {
                _uiState.update {
                    it.copy(error = "无法获取文件下载元数据（Etag/S3KeyFlag），请返回文件列表后重试")
                }
                return@launch
            }
            transferRepository.addDownload(resolved, destination)
        }
    }

    fun upload(localPath: String, name: String) {
        transferRepository.addUpload(localPath, _uiState.value.currentParentId, name)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class ShareResult(val url: String, val password: String?)