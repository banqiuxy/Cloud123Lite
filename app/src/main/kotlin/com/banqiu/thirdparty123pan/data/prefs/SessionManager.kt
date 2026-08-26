package com.banqiu.thirdparty123pan.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.banqiu.thirdparty123pan.util.DeviceFingerprint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话管理：authorization（JWT）与设备指纹使用 EncryptedSharedPreferences 加密存储
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "cloud123_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        migrateSessionSchema()
    }

    /**
     * 早期版本错误地把 authorization 当作 Cookie token 保存，导致需要 Cookie 的
     * user/share/upload/download 接口全部失败。清除旧会话并强制重新登录一次。
     */
    private fun migrateSessionSchema() {
        if (prefs.getInt(KEY_SESSION_SCHEMA, 0) < SESSION_SCHEMA_VERSION) {
            prefs.edit()
                .remove(KEY_AUTHORIZATION)
                .remove(KEY_COOKIE)
                .putInt(KEY_SESSION_SCHEMA, SESSION_SCHEMA_VERSION)
                .apply()
        }
    }

    var authorization: String
        get() = prefs.getString(KEY_AUTHORIZATION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AUTHORIZATION, value).apply()

    /**
     * 会话 Cookie（API.md §5.1：登录成功后设置 Cookie，后续请求自动携带）。
     * 以 "name=value; name2=value2" 形式持久化，加密存储。
     */
    var cookie: String
        get() = prefs.getString(KEY_COOKIE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_COOKIE, value).apply()

    /** 合并一组 Set-Cookie 头（保留已有键，新键覆盖），返回更新后的完整 Cookie 串 */
    fun mergeCookies(setCookieHeaders: List<String>): String {
        if (setCookieHeaders.isEmpty()) return cookie
        val map = linkedMapOf<String, String>()
        cookie.split(";").forEach { part ->
            val p = part.trim()
            if (p.contains("=")) map[p.substringBefore("=").trim()] = p.substringAfter("=").trim()
        }
        setCookieHeaders.forEach { header ->
            val pair = header.substringBefore(";").trim()
            if (pair.contains("=")) {
                val name = pair.substringBefore("=").trim()
                val value = pair.substringAfter("=").trim()
                // 服务端偶尔会返回 token= 或其他空值清理 Cookie。
                // 不允许空值覆盖当前有效会话，否则后续接口会报 cookie token is empty。
                if (name.isNotEmpty() && value.isNotEmpty()) {
                    map[name] = value
                }
            }
        }
        val merged = map.entries.joinToString("; ") { "${it.key}=${it.value}" }
        cookie = merged
        return merged
    }

    /**
     * 保存登录响应中的两个独立值。
     * data.token 写入 Cookie 的 token 键；data.authorization 仅作为请求头使用，
     * 不写入 Cookie（参考实现同样将二者分离）。
     */
    fun setLoginCookies(token: String) {
        if (token.isNotBlank()) mergeCookies(listOf("token=$token"))
    }

    var loginUuid: String
        get() {
            val value = prefs.getString(KEY_LOGIN_UUID, "") ?: ""
            if (value.isEmpty()) {
                val newUuid = DeviceFingerprint.newUuid()
                prefs.edit().putString(KEY_LOGIN_UUID, newUuid).apply()
                return newUuid
            }
            return value
        }
        set(value) = prefs.edit().putString(KEY_LOGIN_UUID, value).apply()

    var deviceType: String
        get() = prefs.getString(KEY_DEVICE_TYPE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE_TYPE, value).apply()

    var osVersion: String
        get() = prefs.getString(KEY_OS_VERSION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OS_VERSION, value).apply()

    val isLoggedIn: Boolean
        get() = authorization.isNotEmpty()

    /** 首次使用时生成并持久化设备指纹（API.md §9.5） */
    fun ensureDeviceFingerprint() {
        if (deviceType.isEmpty()) deviceType = DeviceFingerprint.randomDeviceType()
        if (osVersion.isEmpty()) osVersion = DeviceFingerprint.randomOsVersion()
        if (loginUuid.isEmpty()) loginUuid = DeviceFingerprint.newUuid()
    }

    fun clearSession() {
        prefs.edit().remove(KEY_AUTHORIZATION).apply()
        prefs.edit().remove(KEY_COOKIE).apply()
    }

    companion object {
        private const val KEY_AUTHORIZATION = "authorization"
        private const val KEY_COOKIE = "cookie"
        private const val KEY_SESSION_SCHEMA = "session_schema"
        private const val SESSION_SCHEMA_VERSION = 5
        private const val KEY_LOGIN_UUID = "loginuuid"
        private const val KEY_DEVICE_TYPE = "devicetype"
        private const val KEY_OS_VERSION = "osversion"
    }
}