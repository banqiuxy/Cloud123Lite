package com.banqiu.thirdparty123pan.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object Formatters {

    private val units = arrayOf("B", "KB", "MB", "GB", "TB")

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val digit = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digit.toDouble())
        return if (digit == 0) "${bytes} B"
        else String.format(Locale.US, "%.1f %s", value, units[digit])
    }

    fun formatSpeed(bytesPerSec: Long): String = "${formatSize(bytesPerSec)}/s"

    fun formatTime(timestamp: Long): String {
        if (timestamp <= 0) return "--"
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(timestamp))
    }

    fun formatEta(seconds: Long): String {
        if (seconds <= 0) return "--"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    /** 解析服务器 ISO8601 时间（如 2024-01-01T00:00:00+08:00） */
    fun parseServerTime(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(iso)?.time ?: 0L
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso)?.time ?: 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }
}