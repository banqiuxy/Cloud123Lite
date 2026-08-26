package com.banqiu.thirdparty123pan.util

import java.io.File
import java.security.MessageDigest

object Md5Utils {

    private const val BUFFER_SIZE = 1024 * 1024 // 1MB

    /**
     * 流式计算文件 MD5（32位十六进制），大文件不占用额外内存
     */
    fun md5(file: File, onProgress: (Float) -> Unit = {}): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var total = 0L
            val size = file.length().coerceAtLeast(1)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
                total += read
                onProgress(total.toFloat() / size)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}