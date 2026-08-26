package com.banqiu.thirdparty123pan.domain.model

import com.banqiu.thirdparty123pan.data.model.RemoteFileInfo
import com.banqiu.thirdparty123pan.util.Formatters

enum class FileCategory {
    FOLDER, IMAGE, VIDEO, AUDIO, DOC, OTHER
}

data class FileItem(
    val fileId: Long,
    val parentId: Long,
    val name: String,
    val isFolder: Boolean,
    val size: Long,
    val etag: String?,
    val s3KeyFlag: String?,
    val updateTime: Long,
    val createTime: Long
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val category: FileCategory
        get() = when {
            isFolder -> FileCategory.FOLDER
            extension in imageExtensions -> FileCategory.IMAGE
            extension in videoExtensions -> FileCategory.VIDEO
            extension in audioExtensions -> FileCategory.AUDIO
            extension in docExtensions -> FileCategory.DOC
            else -> FileCategory.OTHER
        }

    val sizeText: String
        get() = Formatters.formatSize(size)

    val updateTimeText: String
        get() = Formatters.formatTime(updateTime)

    companion object {
        val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "avif")
        val videoExtensions = setOf("mp4", "mkv", "mov", "avi", "wmv", "flv", "webm", "m4v", "ts", "3gp", "rmvb")
        val audioExtensions = setOf("mp3", "flac", "wav", "aac", "ogg", "m4a", "wma", "opus", "mid")
        val docExtensions = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md",
            "csv", "rtf", "epub", "mobi", "zip", "rar", "7z", "apk", "torrent"
        )

        fun fromRemote(info: RemoteFileInfo): FileItem = FileItem(
            fileId = info.fileId,
            parentId = info.parentFileId,
            name = info.fileName,
            isFolder = info.type == 1,
            size = info.size,
            etag = info.etag,
            s3KeyFlag = info.s3KeyFlag,
            updateTime = Formatters.parseServerTime(info.updateTime),
            createTime = Formatters.parseServerTime(info.createTime)
        )
    }
}