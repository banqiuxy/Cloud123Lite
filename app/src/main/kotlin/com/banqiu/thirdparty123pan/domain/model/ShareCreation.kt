package com.banqiu.thirdparty123pan.domain.model

/** 官网创建分享后返回的实际链接和提取码。 */
data class ShareCreation(
    val url: String,
    val password: String?
)
