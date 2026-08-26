package com.banqiu.thirdparty123pan.domain.model

import com.banqiu.thirdparty123pan.data.model.ShareListItem

data class ShareItem(
    val shareId: Long,
    val shareKey: String,
    val shareName: String,
    val shareUrl: String,
    val sharePwd: String?,
    val fileId: Long,
    val expiration: String?
) {
    companion object {
        fun fromRemote(item: ShareListItem): ShareItem = ShareItem(
            shareId = item.shareId,
            shareKey = item.shareKey ?: "",
            shareName = item.shareName ?: "",
            shareUrl = item.shareUrl ?: (item.shareKey?.let { "https://www.123pan.cn/s/$it" } ?: ""),
            sharePwd = item.sharePwd,
            fileId = item.fileId,
            expiration = item.expiration
        )
    }
}