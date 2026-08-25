package com.banqiu.thirdparty123pan.ui.screens.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banqiu.thirdparty123pan.domain.model.TransferTask
import com.banqiu.thirdparty123pan.domain.repository.TransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transferRepository: TransferRepository
) : ViewModel() {

    val tasks: StateFlow<List<TransferTask>> = transferRepository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pause(id: Long) = transferRepository.pause(id)
    fun resume(id: Long) = transferRepository.resume(id)
    fun cancel(id: Long) = transferRepository.cancel(id)
    fun retry(id: Long) = transferRepository.retry(id)
    fun remove(id: Long) = transferRepository.remove(id)
    fun clearFinished() = transferRepository.clearFinished()
}