package com.banqiu.thirdparty123pan.ui.screens.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banqiu.thirdparty123pan.domain.model.FileCategory
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.domain.repository.FileRepository
import com.banqiu.thirdparty123pan.domain.repository.TransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<FileItem> = emptyList(),
    val currentParentId: Long = 0,
    val currentDirName: String = "全部相册",
    val showImages: Boolean = true,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val transferRepository: TransferRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState

    val visibleItems: List<FileItem>
        get() {
            val state = _uiState.value
            val category = if (state.showImages) FileCategory.IMAGE else FileCategory.VIDEO
            return state.items.filter {
                !it.isFolder && it.category == category
            }
        }

    init {
        loadDirectory(0, "全部相册")
    }

    fun loadDirectory(parentId: Long, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val list = fileRepository.listFiles(parentId = parentId, page = 1, limit = 200)
                _uiState.update {
                    it.copy(
                        items = list,
                        currentParentId = parentId,
                        currentDirName = name,
                        loading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "加载失败") }
            }
        }
    }

    suspend fun resolveUrl(item: FileItem): String? = try {
        fileRepository.resolveDownloadUrl(item).takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }

    fun addDownload(item: FileItem, destination: String) {
        transferRepository.addDownload(item, destination)
    }

    fun setShowImages(showImages: Boolean) {
        _uiState.update { it.copy(showImages = showImages) }
    }

    fun enterFolder(folder: FileItem) {
        loadDirectory(folder.fileId, folder.name)
    }

    fun back() {
        loadDirectory(0, "全部相册")
    }

    fun toggleSelection(fileId: Long) {
        _uiState.update { state ->
            val selected = state.selectedIds.toMutableSet()
            if (!selected.add(fileId)) selected.remove(fileId)
            state.copy(selectedIds = selected)
        }
    }

    fun enterSelection() {
        _uiState.update { it.copy(selectionMode = true, selectedIds = emptySet()) }
    }

    fun exitSelection() {
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    /** 批量下载选中项 */
    fun downloadSelected(onDownload: (FileItem, String) -> Unit) {
        val state = _uiState.value
        state.items.filter { it.fileId in state.selectedIds }.forEach { onDownload(it, it.name) }
        exitSelection()
    }

    /** 批量删除选中项 */
    fun deleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                fileRepository.trash(ids, true)
                exitSelection()
                loadDirectory(_uiState.value.currentParentId, _uiState.value.currentDirName)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "删除失败") }
            }
        }
    }
}