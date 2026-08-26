package com.banqiu.thirdparty123pan.domain.model

data class User(
    val uid: Long = 0,
    val nickname: String = "",
    val avatar: String? = null,
    val vipLevel: Int = 0,
    val vipName: String = "普通用户",
    val totalSpace: Long = 0,
    val usedSpace: Long = 0
) {
    val usedPercent: Float
        get() = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace).coerceIn(0f, 1f) else 0f
}