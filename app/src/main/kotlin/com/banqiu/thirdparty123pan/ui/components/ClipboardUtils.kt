package com.banqiu.thirdparty123pan.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * 将分享链接和提取码作为完整文本复制到系统剪切板。
 * 剪切板和 Toast 统一在主线程执行，避免从网络协程回调时没有任何反馈。
 */
fun copyShareToClipboard(
    context: Context,
    url: String,
    password: String? = null,
    showToast: Boolean = true
) {
    val appContext = context.applicationContext
    val content = buildString {
        append("链接：")
        append(url)
        if (!password.isNullOrBlank()) {
            append("\n提取码：")
            append(password)
        }
    }

    fun copyOnMainThread() {
        try {
            val clipboard = appContext.getSystemService(ClipboardManager::class.java)
                ?: throw IllegalStateException("系统剪切板服务不可用")
            clipboard.setPrimaryClip(ClipData.newPlainText("Cloud123 分享", content))
            if (showToast) {
                Toast.makeText(appContext, "复制链接成功！", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // 不吞掉复制失败，至少给用户明确反馈。
            Toast.makeText(
                appContext,
                "复制失败：${e.message ?: "系统剪切板不可用"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (Looper.myLooper() == Looper.getMainLooper()) {
        copyOnMainThread()
    } else {
        Handler(Looper.getMainLooper()).post { copyOnMainThread() }
    }
}