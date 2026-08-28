package com.banqiu.thirdparty123pan.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

// ---------- 账号密码登录 ----------
@Serializable
data class SignInRequest(
    @SerialName("type") val type: Int = 1,
    @SerialName("passport") val passport: String,
    @SerialName("password") val password: String
)

@Serializable
data class SignInData(
    @SerialName("token") val token: String? = null,
    @SerialName("authorization") val authorization: String? = null
)

// ---------- 二维码登录 ----------
@Serializable
data class QrGenerateData(
    @SerialName("uniID") val uniId: String? = null,
    @SerialName("url") val url: String? = null
)

@Serializable
data class QrResultData(
    @SerialName("loginStatus") val loginStatus: Int = 0,
    @SerialName("scanPlatform") val scanPlatform: Int = 0,
    @SerialName("token") val token: String? = null,
    @SerialName("login_type") val loginType: Int? = null
)

@Serializable
data class WxCodeRequest(
    @SerialName("uniID") val uniId: String
)

// ---------- 用户信息 ----------
@Serializable
data class UserInfoData(
    @SerialName("uid") val uid: Long? = null,
    @SerialName("UID") val uidUpper: JsonElement? = null,
    @SerialName("space") val space: Long = 0,
    @SerialName("SpacePermanent") val spacePermanent: JsonElement? = null,
    @SerialName("useSpace") val useSpace: Long = 0,
    @SerialName("SpaceUsed") val spaceUsed: JsonElement? = null,
    @SerialName("vip") val vip: VipInfo? = null,
    @SerialName("VipLevel") val vipLevelUpper: JsonElement? = null,
    @SerialName("nickname") val nickname: String? = null,
    @SerialName("Nickname") val nicknameUpper: String? = null,
    @SerialName("username") val username: String? = null,
    @SerialName("Username") val usernameUpper: String? = null,
    @SerialName("passport") val passport: JsonElement? = null,
    @SerialName("Passport") val passportUpper: JsonElement? = null,
    @SerialName("avatar") val avatar: String? = null,
    @SerialName("HeadImage") val avatarUpper: String? = null
) {
    /** 官网当前响应使用大写字段，兼容旧接口的小写字段。 */
    val effectiveUid: Long
        get() = uid ?: uidUpper.asLongOrNull() ?: 0L

    val effectiveNickname: String
        get() = listOf(nickname, nicknameUpper, username, usernameUpper)
            .firstOrNull { !it.isNullOrBlank() }
            ?: effectivePassport

    val effectivePassport: String
        get() = listOf(passport, passportUpper)
            .mapNotNull { it.asTextOrNull() }
            .firstOrNull()
            .orEmpty()

    val effectiveAvatar: String?
        get() = listOf(avatar, avatarUpper)
            .firstOrNull { !it.isNullOrBlank() }

    val effectiveTotalSpace: Long
        get() = if (space > 0) space else spacePermanent.asLongOrNull() ?: 0L

    val effectiveUsedSpace: Long
        get() = if (useSpace > 0) useSpace else spaceUsed.asLongOrNull() ?: 0L

    val effectiveVipLevel: Int
        get() = vip?.level?.takeIf { it > 0 }
            ?: vipLevelUpper.asLongOrNull()?.toInt()?.takeIf { it > 0 }
            ?: 0
}

private fun JsonElement?.asTextOrNull(): String? = runCatching {
    this?.jsonPrimitive?.content
}.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

private fun JsonElement?.asLongOrNull(): Long? =
    asTextOrNull()?.toLongOrNull()

@Serializable
data class VipInfo(
    @SerialName("level") val level: Int = 0,
    @SerialName("name") val name: String? = null
)

// ---------- 设备列表 ----------
@Serializable
data class DeviceInfo(
    @SerialName("deviceId") val deviceId: String? = null,
    @SerialName("deviceName") val deviceName: String? = null,
    @SerialName("lastLoginTime") val lastLoginTime: String? = null
)