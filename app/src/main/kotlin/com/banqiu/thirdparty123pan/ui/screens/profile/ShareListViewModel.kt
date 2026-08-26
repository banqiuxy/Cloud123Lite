package com.banqiu.thirdparty123pan.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banqiu.thirdparty123pan.domain.model.ShareItem
import com.banqiu.thirdparty123pan.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShareListUiState(
    val loading: Boolean = false,
    val shares: List<ShareItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ShareListViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareListUiState())
    val uiState: StateFlow<ShareListUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val shares = fileRepository.shareList()
                _uiState.update { it.copy(loading = false, shares = shares) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "加载失败") }
            }
        }
    }

    fun deleteShare(shareId: Long) {
        viewModelScope.launch {
            try {
                fileRepository.deleteShare(listOf(shareId))
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "删除失败") }
            }
        }
    }
}