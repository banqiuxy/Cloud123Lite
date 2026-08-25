package com.banqiu.thirdparty123pan.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 分享创建 ----------
@Serializable
data class ShareCreateRequest(
    @SerialName("driveId") val driveId: Int = 0,
    @SerialName("expiration") val expiration: String = "2099-12-12T08:00:00+08:00",
    @SerialName("fileIdList") val fileIdList: List<Long>,
    @SerialName("shareName") val shareName: String = "123云盘分享",
    @SerialName("sharePwd") val sharePwd: String? = null,
    @SerialName("event") val event: String = "shareCreate"
)

@Serializable
data class ShareCreateData(
    @SerialName("ShareKey") val shareKey: String? = null
)

// ---------- 分享列表 ----------
@Serializable
data class ShareListData(
    @SerialName("InfoList") val infoList: List<ShareListItem> = emptyList(),
    @SerialName("Next") val next: String? = null
)

@Serializable
data class ShareListItem(
    @SerialName("shareId") val shareId: Long = 0,
    @SerialName("shareKey") val shareKey: String? = null,
    @SerialName("shareName") val shareName: String? = null,
    @SerialName("shareUrl") val shareUrl: String? = null,
    @SerialName("sharePwd") val sharePwd: String? = null,
    @SerialName("fileId") val fileId: Long = 0,
    @SerialName("expiration") val expiration: String? = null,
    @SerialName("shareStatus") val shareStatus: Int = 0
)

// ---------- 删除分享 ----------
@Serializable
data class ShareDeleteRequest(
    @SerialName("driveId") val driveId: Int = 0,
    @SerialName("shareInfoList") val shareInfoList: List<ShareIdWrapper>,
    @SerialName("isPayShare") val isPayShare: Int = 0,
    @SerialName("event") val event: String = "shareCancel",
    @SerialName("operatePlace") val operatePlace: Int = 2
)

@Serializable
data class ShareIdWrapper(
    @SerialName("shareId") val shareId: Long
)

// ---------- 离线下载 ----------
@Serializable
data class OfflineResolveRequest(
    @SerialName("urls") val urls: String
)

@Serializable
data class OfflineSubmitRequest(
    @SerialName("resource_list") val resourceList: List<OfflineResource>
)

@Serializable
data class OfflineResource(
    @SerialName("resource_id") val resourceId: Long,
    @SerialName("select_file_id") val selectFileId: List<Int> = emptyList()
)