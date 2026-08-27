package com.banqiu.thirdparty123pan.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

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
    @SerialName("Info") val info: UploadFileInfo? = null,
    @SerialName("Bucket") val bucket: String? = null,
    @SerialName("StorageNode") val storageNode: String? = null,
    @SerialName("Key") val key: String? = null,
    @SerialName("UploadId") val uploadId: String? = null
) {
    val effectiveFileId: Long
        get() = if (fileId > 0L) fileId else info?.fileId ?: 0L
}

@Serializable
data class UploadFileInfo(
    @SerialName("FileId") val fileId: Long = 0
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
    /** 参考客户端实际读取 data.presignedUrls。 */
    @SerialName("presignedUrls") val presignedUrls: Map<String, String> = emptyMap(),
    /** 兼容部分服务端版本返回的 urls 字段。 */
    @SerialName("urls") val urls: Map<String, String> = emptyMap()
) {
    val allUrls: Map<String, String>
        get() = if (presignedUrls.isNotEmpty()) presignedUrls else urls
}

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
    // 实测（2026-08）：服务端返回大写 "Parts" 键，保留小写作兼容。
    @SerialName("Parts") val upperParts: List<S3PartInfo> = emptyList(),
    @SerialName("parts") val lowerParts: List<S3PartInfo> = emptyList()
) {
    val allParts: List<S3PartInfo>
        get() = if (upperParts.isNotEmpty()) upperParts else lowerParts
}

@Serializable
data class S3PartInfo(
    // 服务端不同节点可能返回数字或数字字符串，使用 JsonElement 兼容两种格式。
    @SerialName("PartNumber") val rawPartNumber: JsonElement? = null,
    @SerialName("partNumber") val rawLowerPartNumber: JsonElement? = null
) {
    val partNumber: Int
        get() = parsePartNumber(rawPartNumber ?: rawLowerPartNumber)

    private fun parsePartNumber(value: JsonElement?): Int = runCatching {
        value?.jsonPrimitive?.content?.toInt() ?: 0
    }.getOrDefault(0)
}

// ---------- 上传完成确认 ----------
@Serializable
data class UploadCompleteRequest(
    @SerialName("fileId") val fileId: Long
)

// ---------- 新版 v2 上传完成（2026-08 实测：旧版 upload_complete 已废弃，仍返回 code 0 但不再落地文件） ----------
// 还原自官方 Web 客户端（yun.123pan.cn）上传引擎：
//   complete:    file/upload_complete/v2  （POST，body 为下方完整字段）
//   completePoll: file/upload_complete/result（GET，参数同 v2 body）
@Serializable
data class UploadCompleteV2Request(
    @SerialName("fileId") val fileId: Long,
    @SerialName("bucket") val bucket: String,
    @SerialName("fileSize") val fileSize: Long,
    @SerialName("key") val key: String,
    @SerialName("isMultipart") val isMultipart: Boolean,
    @SerialName("uploadId") val uploadId: String,
    // 注意：官方使用大写 StorageNode（与 upload_request 响应字段一致）
    @SerialName("StorageNode") val storageNode: String
)

@Serializable
data class UploadCompleteV2Data(
    // v2 成功时同步返回真实文件信息；异步合并时为 null，需轮询 result 接口。
    @SerialName("file_info") val fileInfo: UploadFileInfoV2? = null,
    // 轮询间隔秒数（result 接口返回，默认 2s）
    @SerialName("duration") val duration: Int? = null
)

@Serializable
data class UploadFileInfoV2(
    // 云盘中的真实 FileId。upload_request 返回的 FileId 仅为会话占位 ID（数量级差异巨大）。
    @SerialName("FileId") val fileId: Long = 0,
    @SerialName("FileName") val fileName: String? = null,
    @SerialName("Type") val type: Int = 0,
    @SerialName("Size") val size: Long = 0,
    @SerialName("Etag") val etag: String? = null,
    @SerialName("ParentFileId") val parentFileId: Long = 0
)

// 同名文件冲突业务码，需以 duplicate=1 重试
const val CODE_FILE_CONFLICT = 5060