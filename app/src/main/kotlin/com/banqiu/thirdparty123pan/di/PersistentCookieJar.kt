package com.banqiu.thirdparty123pan.di

import com.banqiu.thirdparty123pan.data.prefs.SessionManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * 持久化 OkHttp CookieJar，等价于参考项目中的 requests.Session CookieJar。
 * 仅保存服务端真实下发的 Cookie，不负责猜测 Cookie 名称。
 */
class PersistentCookieJar(
    private val sessionManager: SessionManager
) : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        sessionManager.mergeCookies(cookies.map { "${it.name}=${it.value}" })
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val raw = sessionManager.cookie
        if (raw.isBlank()) return emptyList()
        return raw.split(';').mapNotNull { part ->
            val trimmed = part.trim()
            if (!trimmed.contains('=')) return@mapNotNull null
            val name = trimmed.substringBefore('=').trim()
            val value = trimmed.substringAfter('=').trim()
            if (name.isBlank() || value.isBlank()) return@mapNotNull null
            // 仅把持久化的 name=value 转成当前请求域 Cookie。
            Cookie.Builder()
                .name(name)
                .value(value)
                .domain(url.host)
                .path("/")
                .build()
        }
    }
}