package com.banqiu.thirdparty123pan.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 上传请求（含秒传） ----------
@Serializable
data class UploadRequest(
    @SerialName("driveId") val driveId: Int = 0,
    @SerialName("etag") val etag: String,
    @SerialName("fileName") val fileName: String,
    @SerialName("parentFileId") val parentFileId: Long,
    @SerialName("size") val size: Long,
    @SerialName("type") val type: Int = 0,
    @SerialName("duplicate") val duplicate: Int = 0
)

@Serializable
data class UploadRequestData(
    @SerialName("Reuse") val reuse: Boolean = false,
    @SerialName("FileId") val fileId: Long = 0,
    @SerialName("Bucket") val bucket: String? = null,
    @SerialName("StorageNode") val storageNode: String? = null,
    @SerialName("Key") val key: String? = null,
    @SerialName("UploadId") val uploadId: String? = null
)

// ---------- S3 分片预签名 URL ----------
@Serializable
data class S3PartRequest(
    @SerialName("bucket") val bucket: String,
    @SerialName("key") val key: String,
    @SerialName("partNumberStart") val partNumberStart: Int,
    @SerialName("partNumberEnd") val partNumberEnd: Int,
    @SerialName("uploadId") val uploadId: String,
    @SerialName("StorageNode") val storageNode: String
)

@Serializable
data class S3PartData(
    @SerialName("urls") val urls: Map<String, String> = emptyMap()
)

// ---------- S3 分片列表 / 完成 ----------
@Serializable
data class S3ListPartsRequest(
    @SerialName("bucket") val bucket: String,
    @SerialName("key") val key: String,
    @SerialName("uploadId") val uploadId: String,
    @SerialName("storageNode") val storageNode: String
)

@Serializable
data class S3ListPartsData(
    @SerialName("parts") val parts: List<S3PartInfo> = emptyList()
)

@Serializable
data class S3PartInfo(
    @SerialName("PartNumber") val partNumber: Int = 0
)

// ---------- 上传完成确认 ----------
@Serializable
data class UploadCompleteRequest(
    @SerialName("fileId") val fileId: Long
)

// 同名文件冲突业务码，需以 duplicate=1 重试
const val CODE_FILE_CONFLICT = 5060