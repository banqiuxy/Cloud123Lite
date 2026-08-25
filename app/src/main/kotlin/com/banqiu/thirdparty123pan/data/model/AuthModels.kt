package com.banqiu.thirdparty123pan.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("space") val space: Long = 0,
    @SerialName("useSpace") val useSpace: Long = 0,
    @SerialName("vip") val vip: VipInfo? = null,
    @SerialName("nickname") val nickname: String? = null,
    @SerialName("avatar") val avatar: String? = null
)

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