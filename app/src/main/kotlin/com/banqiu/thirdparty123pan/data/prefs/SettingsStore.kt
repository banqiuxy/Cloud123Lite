package com.banqiu.thirdparty123pan.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val GLASS_ENABLED = booleanPreferencesKey("glass_enabled")
        val DOWNLOAD_DIR = stringPreferencesKey("download_dir")
        val CONCURRENCY = intPreferencesKey("concurrency")
        val SPEED_LIMIT = longPreferencesKey("speed_limit")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: false }

    val glassEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.GLASS_ENABLED] ?: true }

    val downloadDir: Flow<String> = context.dataStore.data.map { it[Keys.DOWNLOAD_DIR] ?: "" }

    val concurrency: Flow<Int> = context.dataStore.data.map { it[Keys.CONCURRENCY] ?: 3 }

    /** 速度限制 bytes/s，0 = 不限速 */
    val speedLimit: Flow<Long> = context.dataStore.data.map { it[Keys.SPEED_LIMIT] ?: 0L }

    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setGlassEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.GLASS_ENABLED] = enabled }

    suspend fun setDownloadDir(uri: String) =
        context.dataStore.edit { it[Keys.DOWNLOAD_DIR] = uri }

    suspend fun setConcurrency(n: Int) =
        context.dataStore.edit { it[Keys.CONCURRENCY] = n }

    suspend fun setSpeedLimit(bytes: Long) =
        context.dataStore.edit { it[Keys.SPEED_LIMIT] = bytes }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
}