package com.banqiu.thirdparty123pan.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecycleBinUiState(
    val loading: Boolean = false,
    val files: List<FileItem> = emptyList(),
    val error: String? = null,
    val busy: Boolean = false
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecycleBinUiState())
    val uiState: StateFlow<RecycleBinUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val files = fileRepository.listFiles(parentId = 0, trashed = true, limit = 200)
                _uiState.update { it.copy(loading = false, files = files) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "加载失败") }
            }
        }
    }

    fun restore(fileIds: List<Long>) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            try {
                fileRepository.trash(fileIds, toTrash = false)
                _uiState.update { state ->
                    state.copy(
                        busy = false,
                        files = state.files.filterNot { it.fileId in fileIds }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(busy = false, error = e.message ?: "恢复失败") }
            }
        }
    }

    fun deletePermanently(fileIds: List<Long>) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            try {
                fileRepository.deletePermanently(fileIds)
                _uiState.update { state ->
                    state.copy(
                        busy = false,
                        files = state.files.filterNot { it.fileId in fileIds }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(busy = false, error = e.message ?: "删除失败") }
            }
        }
    }

    fun clearAll() {
        val ids = _uiState.value.files.map { it.fileId }
        if (ids.isNotEmpty()) deletePermanently(ids)
    }
}