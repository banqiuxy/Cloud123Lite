package com.banqiu.thirdparty123pan.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

// ---------- 分享创建 ----------
@Serializable
data class ShareCreateRequest(
    @SerialName("driveId") val driveId: Int = 0,
    @SerialName("expiration") val expiration: String,
    // 官网当前请求发送的是逗号分隔字符串，而不是 JSON 数组。
    @SerialName("fileIdList") val fileIdList: String,
    @SerialName("shareName") val shareName: String = "123云盘分享",
    @SerialName("sharePwd") val sharePwd: String = "",
    @SerialName("event") val event: String = "shareCreate",
    @SerialName("fileNum") val fileNum: Int,
    @SerialName("renameVisible") val renameVisible: Boolean = true,
    @SerialName("shareModality") val shareModality: Int = 4,
    @SerialName("operatePlace") val operatePlace: Int = 1,
    @SerialName("trafficLimitSwitch") val trafficLimitSwitch: Int = 1,
    @SerialName("trafficLimit") val trafficLimit: Long = 0,
    @SerialName("trafficSwitch") val trafficSwitch: Int = 1,
    @SerialName("fillPwdSwitch") val fillPwdSwitch: Int = 0
)

/** 官网当前返回 shareLinkList.list；保留旧字段以兼容历史网关响应。 */
@Serializable
data class ShareCreateData(
    @SerialName("shareLinkList") val shareLinkList: JsonElement? = null,
    @SerialName("sharePwd") val sharePwd: String? = null,
    @SerialName("SharePwd") val sharePwdUpper: String? = null,
    @SerialName("ShareKey") val shareKey: String? = null,
    @SerialName("shareKey") val shareKeyLower: String? = null,
    @SerialName("ShareUrl") val shareUrl: String? = null,
    @SerialName("shareUrl") val shareUrlLower: String? = null
) {
    /** 兼容官网对象格式、旧数组格式和单字符串格式。 */
    fun links(): List<String> = when (val value = shareLinkList) {
        is JsonObject -> (value["list"] as? JsonArray)
            ?.mapNotNull { it.asText() }
            .orEmpty()
        is JsonArray -> value.mapNotNull { it.asText() }
        is JsonPrimitive -> value.asText()?.let(::listOf).orEmpty()
        else -> emptyList()
    }
}

private fun JsonElement.asText(): String? = runCatching {
    jsonPrimitive.content
}.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

// ---------- 分享列表 ----------
@Serializable
data class ShareListData(
    @SerialName("InfoList") val infoList: List<ShareListItem> = emptyList(),
    @SerialName("Next") val next: String? = null
)

@Serializable
data class ShareListItem(
    @SerialName("shareId") val shareId: Long = 0,
    @SerialName("ShareId") val shareIdUpper: Long = 0,
    @SerialName("shareKey") val shareKey: String? = null,
    @SerialName("ShareKey") val shareKeyUpper: String? = null,
    @SerialName("shareName") val shareName: String? = null,
    @SerialName("ShareName") val shareNameUpper: String? = null,
    @SerialName("shareUrl") val shareUrl: String? = null,
    @SerialName("ShareUrl") val shareUrlUpper: String? = null,
    @SerialName("sharePwd") val sharePwd: String? = null,
    @SerialName("SharePwd") val sharePwdUpper: String? = null,
    @SerialName("fileId") val fileId: Long = 0,
    @SerialName("FileId") val fileIdUpper: Long = 0,
    @SerialName("expiration") val expiration: String? = null,
    @SerialName("Expiration") val expirationUpper: String? = null,
    @SerialName("shareStatus") val shareStatus: Int = 0,
    @SerialName("Status") val statusUpper: Int = 0
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