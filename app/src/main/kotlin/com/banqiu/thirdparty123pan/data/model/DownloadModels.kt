package com.banqiu.thirdparty123pan.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 单文件下载 ----------
@Serializable
data class DownloadInfoRequest(
    @SerialName("driveId") val driveId: Int = 0,
    @SerialName("etag") val etag: String? = null,
    @SerialName("fileId") val fileId: Long,
    @SerialName("s3keyFlag") val s3keyFlag: String? = null,
    @SerialName("type") val type: Int = 0,
    @SerialName("fileName") val fileName: String,
    @SerialName("size") val size: Long
)

// ---------- 文件夹批量下载 ----------
@Serializable
data class BatchDownloadRequest(
    @SerialName("fileIdList") val fileIdList: List<FileIdLower>
)

@Serializable
data class DownloadInfoData(
    @SerialName("DownloadUrl") val downloadUrl: String? = null,
    @SerialName("RedirectUrl") val redirectUrl: String? = null
)

// 下载流量超限的业务码（不报错，走 URL 重写绕过）
const val CODE_DOWNLOAD_LIMIT_1 = 5113
const val CODE_DOWNLOAD_LIMIT_2 = 5114