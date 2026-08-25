package com.banqiu.thirdparty123pan.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 123pan 统一响应包装。
 *
 * API 文档中的业务接口以 code=0 表示成功，但登录服务实际还会返回
 * HTTP 风格的 code=200/message=success。两种成功码都必须接受，否则会把
 * 成功响应作为异常显示在登录页，导致无法进入主界面。
 */
@Serializable
data class ApiResponse<T>(
    @SerialName("code") val code: Int = 0,
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: T? = null
) {
    val isSuccess: Boolean get() = code == 0 || code == 200
}

class ApiException(val code: Int, msg: String) : Exception(msg)