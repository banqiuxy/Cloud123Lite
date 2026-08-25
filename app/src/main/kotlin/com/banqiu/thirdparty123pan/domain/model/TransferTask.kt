package com.banqiu.thirdparty123pan.domain.model

import com.banqiu.thirdparty123pan.data.db.entity.TransferTaskEntity

enum class TaskType(val value: Int) {
    UPLOAD(0), DOWNLOAD(1);

    companion object {
        fun from(value: Int): TaskType = entries.firstOrNull { it.value == value } ?: DOWNLOAD
    }
}

enum class TaskStatus(val value: Int) {
    WAITING(0), RUNNING(1), PAUSED(2), SUCCESS(3), FAILED(4), CANCELED(5);

    companion object {
        fun from(value: Int): TaskStatus = entries.firstOrNull { it.value == value } ?: WAITING
    }
}

data class TransferTask(
    val id: Long,
    val type: TaskType,
    val fileId: Long,
    val localPath: String?,
    val remoteDirId: Long,
    val name: String,
    val size: Long,
    val progress: Float,
    val speed: Long,
    val status: TaskStatus,
    val createTime: Long,
    val finishTime: Long?,
    val error: String?,
    val etag: String?,
    val s3KeyFlag: String?,
    val url: String?
) {
    val isActive: Boolean
        get() = status == TaskStatus.WAITING || status == TaskStatus.RUNNING

    companion object {
        fun fromEntity(e: TransferTaskEntity): TransferTask = TransferTask(
            id = e.id,
            type = TaskType.from(e.type),
            fileId = e.fileId,
            localPath = e.localPath,
            remoteDirId = e.remoteDirId,
            name = e.name,
            size = e.size,
            progress = e.progress,
            speed = e.speed,
            status = TaskStatus.from(e.status),
            createTime = e.createTime,
            finishTime = e.finishTime,
            error = e.error,
            etag = e.etag,
            s3KeyFlag = e.s3KeyFlag,
            url = e.url
        )
    }
}