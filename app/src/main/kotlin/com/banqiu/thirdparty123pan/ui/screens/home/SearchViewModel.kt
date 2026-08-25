package com.banqiu.thirdparty123pan.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val loading: Boolean = false,
    val results: List<FileItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    private var searchJob: Job? = null

    /** 防抖搜索（300ms） */
    fun search(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), loading = false, error = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val results = fileRepository.listFiles(
                    parentId = 0,
                    search = query,
                    page = 1,
                    limit = 200
                )
                _uiState.update { it.copy(loading = false, results = results) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "搜索失败") }
            }
        }
    }
}