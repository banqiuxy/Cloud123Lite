package com.banqiu.thirdparty123pan.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 文件列表 ----------
@Serializable
data class FileListData(
    @SerialName("InfoList") val infoList: List<RemoteFileInfo> = emptyList(),
    @SerialName("Next") val next: String? = null
)

@Serializable
data class RemoteFileInfo(
    @SerialName("FileId") val fileId: Long = 0,
    @SerialName("FileName") val fileName: String = "",
    @SerialName("Type") val type: Int = 0,      // 0=文件, 1=文件夹
    @SerialName("Size") val size: Long = 0,
    @SerialName("Etag") val etag: String? = null,
    @SerialName("S3KeyFlag") val s3KeyFlag: String? = null,
    @SerialName("parentFileId") val parentFileId: Long = 0,
    @SerialName("UpdateTime") val updateTime: String? = null,
    @SerialName("CreateTime") val createTime: String? = null
)

// ---------- 删除/恢复（回收站） ----------
@Serializable
data class TrashRequest(
    @SerialName("driveId") val driveId: Int = 0,
    @SerialName("fileTrashInfoList") val fileTrashInfoList: List<FileIdWrapper>,
    @SerialName("operation") val operation: Boolean
)

@Serializable
data class FileIdWrapper(
    @SerialName("FileId") val fileId: Long
)

// ---------- 永久删除 ----------
@Serializable
data class DeleteRequest(
    @SerialName("fileIdList") val fileIdList: List<FileIdLower>,
    @SerialName("event") val event: String = "recycleDelete",
    @SerialName("operatePlace") val operatePlace: Int = 1,
    @SerialName("RequestSource") val requestSource: String? = null
)

@Serializable
data class FileIdLower(
    @SerialName("fileId") val fileId: Long
)

// ---------- 重命名 ----------
@Serializable
data class RenameRequest(
    @SerialName("driveId") val driveId: Int = 0,
    @SerialName("fileId") val fileId: Long,
    @SerialName("fileName") val fileName: String
)

// ---------- 移动 ----------
@Serializable
data class MoveRequest(
    @SerialName("fileIdList") val fileIdList: List<FileIdWrapper>,
    @SerialName("parentFileId") val parentFileId: Long
)

// ---------- 复制（异步） ----------
@Serializable
data class CopyRequest(
    @SerialName("fileList") val fileList: List<CopyFileItem>,
    @SerialName("targetFileId") val targetFileId: Long
)

@Serializable
data class CopyFileItem(
    @SerialName("fileId") val fileId: Long,
    @SerialName("fileName") val fileName: String
)

@Serializable
data class CopyTaskData(
    @SerialName("taskId") val taskId: String? = null
)

// ---------- 创建文件夹（复用 upload_request，type=1） ----------
@Serializable
data class CreateFolderRequest(
    @SerialName("driveId") val driveId: Int = 0,
    @SerialName("etag") val etag: String = "",
    @SerialName("fileName") val fileName: String,
    @SerialName("parentFileId") val parentFileId: Long,
    @SerialName("size") val size: Long = 0,
    @SerialName("type") val type: Int = 1,
    @SerialName("duplicate") val duplicate: Int = 1,
    @SerialName("NotReuse") val notReuse: Boolean = true,
    @SerialName("event") val event: String = "newCreateFolder",
    @SerialName("operateType") val operateType: Int = 1
)