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
            shareId = if (item.shareId > 0) item.shareId else item.shareIdUpper,
            shareKey = item.shareKey ?: item.shareKeyUpper ?: "",
            shareName = item.shareName ?: item.shareNameUpper ?: "",
            shareUrl = (item.shareUrl ?: item.shareUrlUpper).orEmpty().ifBlank {
                val key = item.shareKey ?: item.shareKeyUpper
                key?.let { "https://www.123pan.cn/s/$it" }.orEmpty()
            },
            sharePwd = item.sharePwd ?: item.sharePwdUpper,
            fileId = if (item.fileId > 0) item.fileId else item.fileIdUpper,
            expiration = item.expiration ?: item.expirationUpper
        )
    }
}