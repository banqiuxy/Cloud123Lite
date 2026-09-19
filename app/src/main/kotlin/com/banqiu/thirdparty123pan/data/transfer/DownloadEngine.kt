package com.banqiu.thirdparty123pan.data.transfer

import com.banqiu.thirdparty123pan.data.db.dao.TransferTaskDao
import com.banqiu.thirdparty123pan.di.TransferHttpClient
import com.banqiu.thirdparty123pan.domain.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEngine @Inject constructor(
    @TransferHttpClient private val transferClient: OkHttpClient,
    private val dao: TransferTaskDao,
    private val fileRepository: com.banqiu.thirdparty123pan.domain.repository.FileRepository,
    private val storage: DownloadTargetStorage
) {
    suspend fun download(taskId: Long, onProgress: (Float, Long) -> Unit) = withContext(Dispatchers.IO) {
        var task = dao.getById(taskId) ?: return@withContext
        if (task.phase == "published") return@withContext
        storage.checkConflict(task)
        val dest = storage.stagingFile(task)
        if (dest.parentFile?.let { it.isDirectory || it.mkdirs() } != true) throw IOException("无法创建下载暂存目录")
        if (task.storageKind != "legacy" && task.stagingPath == null) {
            task = task.copy(stagingPath = dest.absolutePath)
            dao.update(task)
        }
        if (task.phase == "committing" || task.phase == "downloaded") {
            storage.publish(taskId)
            return@withContext
        }
        val item = FileItem(fileId = task.fileId, parentId = task.remoteDirId, name = task.sourceName ?: task.name,
            isFolder = false, size = task.size, etag = task.etag, s3KeyFlag = task.s3KeyFlag,
            updateTime = task.createTime, createTime = task.createTime)
        val url = task.url?.takeIf { it.isNotBlank() } ?: fileRepository.resolveDownloadUrl(item).also {
            if (it.isBlank()) throw IOException("获取下载链接失败")
            dao.updateUrl(taskId, it)
        }
        var downloaded = dest.length()
        if (task.size > 0 && downloaded > task.size) throw IOException("暂存文件长度异常，请取消任务后重新下载")
        if (task.size <= 0 || downloaded != task.size) {
            val call = transferClient.newCall(Request.Builder().url(url).header("Range", "bytes=$downloaded-").build())
            coroutineScope {
            // A child cancellation hook closes a blocked socket promptly on pause/cancel.
            val cancellation = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                try { awaitCancellation() } finally { call.cancel() }
            }
            try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("下载失败 HTTP ${response.code}")
                val body = response.body ?: throw IOException("响应体为空")
                if (response.code == 206) validateContentRange(response.header("Content-Range"), downloaded, task.size)
                else downloaded = 0
                val responseStart = downloaded
                val expectedBody = body.contentLength()
                // Buffer belongs to this invocation, not the singleton engine.
                val buffer = ByteArray(256 * 1024)
                RandomAccessFile(dest, "rw").use { file ->
                    if (response.code != 206) file.setLength(0)
                    file.seek(downloaded)
                    var reportTime = System.currentTimeMillis()
                    var reportBytes = downloaded
                    var smoothSpeed = 0L
                    val start = System.currentTimeMillis()
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = body.source().read(buffer)
                        if (count < 0) break
                        file.write(buffer, 0, count)
                        downloaded += count
                        if (task.size > 0 && downloaded > task.size) throw IOException("下载内容超过预期长度")
                        val limit = speedLimit
                        if (limit > 0) {
                            val wait = ((downloaded - responseStart) * 1000 / limit - (System.currentTimeMillis() - start)).coerceAtLeast(0)
                            if (wait > 0) delay(wait)
                        }
                        val now = System.currentTimeMillis()
                        if (now - reportTime >= 500) {
                            val speed = (downloaded - reportBytes) * 1000 / (now - reportTime)
                            smoothSpeed = if (smoothSpeed == 0L) speed else (smoothSpeed * 3 + speed) / 4
                            onProgress(if (task.size > 0) (downloaded.toFloat() / task.size).coerceIn(0f, 0.99f) else 0f, smoothSpeed)
                            reportTime = now; reportBytes = downloaded
                        }
                    }
                    file.fd.sync()
                }
                if (expectedBody >= 0 && downloaded - responseStart != expectedBody) throw IOException("下载中断，响应长度不完整")
            }
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                throw e
            } finally { cancellation.cancel() }
            }
        }
        if (task.size > 0 && downloaded != task.size) throw IOException("下载中断，文件长度不完整")
        currentCoroutineContext().ensureActive()
        if (task.storageKind != "legacy") {
            val latest = dao.getById(taskId) ?: return@withContext
            dao.update(latest.copy(phase = "downloaded"))
            storage.publish(taskId)
        }
        onProgress(1f, 0L)
    }

    @Volatile var speedLimit: Long = 0L
    companion object { fun formatProgress(progress: Float): String = "%.1f%%".format(progress * 100) }
}

internal fun validateContentRange(header: String?, offset: Long, expectedSize: Long) {
    val match = Regex("bytes (\\d+)-(\\d+)/(\\d+|\\*)").matchEntire(header ?: "")
        ?: throw IOException("服务器返回无效的 Content-Range")
    val start = match.groupValues[1].toLongOrNull() ?: throw IOException("无效的 Range 起点")
    val end = match.groupValues[2].toLongOrNull() ?: throw IOException("无效的 Range 终点")
    val total = match.groupValues[3].toLongOrNull()
    if (start != offset || end < start || (total != null && end >= total) ||
        (expectedSize > 0 && (total != expectedSize || end != expectedSize - 1)))
        throw IOException("服务器返回的续传范围与任务不一致")
}
