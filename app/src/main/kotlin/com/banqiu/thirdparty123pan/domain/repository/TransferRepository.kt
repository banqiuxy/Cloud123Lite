package com.banqiu.thirdparty123pan.domain.repository

import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.domain.model.TransferTask
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    val tasks: Flow<List<TransferTask>>
    fun addDownload(fileItem: FileItem, destination: String)
    fun addUpload(localPath: String, remoteDirId: Long, name: String)
    fun pause(id: Long)
    fun resume(id: Long)
    fun cancel(id: Long)
    fun retry(id: Long)
    fun clearFinished()
    fun remove(id: Long)
}