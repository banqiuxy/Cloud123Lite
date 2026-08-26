package com.banqiu.thirdparty123pan.data.transfer

import java.util.concurrent.atomic.AtomicLong

/**
 * 按实际成功 PUT 的字节数计算上传进度和速度。
 * 小于 100ms 不频繁写入 Room；批次结束时由调用方强制刷新一次。
 */
class UploadProgressReporter(
    private val totalBytes: Long,
    initialBytes: Long,
    private val onProgress: (Float, Long) -> Unit
) {
    private val uploaded = AtomicLong(initialBytes.coerceAtLeast(0L))
    private var lastBytes = uploaded.get()
    private var lastAt = System.nanoTime()

    @Synchronized
    fun addUploaded(bytes: Long) {
        uploaded.addAndGet(bytes.coerceAtLeast(0L))
        report()
    }

    @Synchronized
    fun report(force: Boolean = false) {
        val now = System.nanoTime()
        val elapsedNanos = now - lastAt
        val current = uploaded.get().coerceIn(0L, totalBytes)
        if (!force && elapsedNanos < 100_000_000L && current < totalBytes) return

        val elapsedSeconds = elapsedNanos / 1_000_000_000.0
        val deltaBytes = (current - lastBytes).coerceAtLeast(0L)
        val speed = if (elapsedSeconds > 0.0) {
            (deltaBytes / elapsedSeconds).toLong().coerceAtLeast(0L)
        } else 0L
        val ratio = if (totalBytes > 0) current.toFloat() / totalBytes else 0f
        onProgress(
            (0.10f + ratio * 0.84f).coerceIn(0.10f, 0.94f),
            speed
        )
        lastBytes = current
        lastAt = now
    }
}