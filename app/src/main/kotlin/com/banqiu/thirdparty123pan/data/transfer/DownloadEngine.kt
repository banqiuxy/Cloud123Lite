package com.banqiu.thirdparty123pan.data.transfer

import com.banqiu.thirdparty123pan.data.db.dao.TransferTaskDao
import com.banqiu.thirdparty123pan.di.TransferHttpClient
import com.banqiu.thirdparty123pan.domain.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 下载引擎：断点续传（Range）+ 速度限制 + 进度回调
 * 使用 Transfer Session（不携带鉴权头，大连接池）
 */
@Singleton
class DownloadEngine @Inject constructor(
    @TransferHttpClient private val transferClient: OkHttpClient,
    private val dao: TransferTaskDao,
    private val fileRepository: com.banqiu.thirdparty123pan.domain.repository.FileRepository
) {

    private val buffer = ByteArray(256 * 1024)

    /**
     * 执行下载任务；每 500ms 回调进度
     * @param onProgress (progress 0..1, speed bytes/s)
     */
    suspend fun download(taskId: Long, onProgress: (Float, Long) -> Unit) = withContext(Dispatchers.IO) {
        val task = dao.getById(taskId) ?: return@withContext
        val destFile = File(task.localPath ?: throw IOException("下载路径无效"))
        destFile.parentFile?.mkdirs()

        // 解析直链（缓存到任务，失败重试时重新解析）
        val fileItem = FileItem(
            fileId = task.fileId,
            parentId = task.remoteDirId,
            name = task.name,
            isFolder = false,
            size = task.size,
            etag = task.etag,
            s3KeyFlag = task.s3KeyFlag,
            updateTime = task.createTime,
            createTime = task.createTime
        )
        var url = task.url
        if (url.isNullOrEmpty()) {
            url = fileRepository.resolveDownloadUrl(fileItem)
            if (url.isEmpty()) throw IOException("获取下载链接失败")
            dao.updateUrl(taskId, url)
        }

        var downloaded = destFile.length()
        if (downloaded >= task.size && task.size > 0) {
            onProgress(1f, 0L)
            return@withContext
        }

        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$downloaded-")
            .get()
            .build()

        transferClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw IOException("下载失败 HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("响应体为空")

            val append = response.code == 206
            if (!append) {
                // 服务端忽略 Range，从头写
                destFile.delete()
                downloaded = 0
            }

            val source = body.source()
            RandomAccessFile(destFile, "rw").use { raf ->
                raf.seek(downloaded)
                var lastReport = System.currentTimeMillis()
                var lastBytes = downloaded
                var lastSpeedTime = lastReport
                var smoothSpeed = 0L
                val total = task.size.coerceAtLeast(1)

                while (true) {
                    val read = source.read(buffer)
                    if (read <= 0) break
                    raf.write(buffer, 0, read)
                    downloaded += read

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastSpeedTime
                    if (elapsed >= 500) {
                        val speed = (downloaded - lastBytes) * 1000 / elapsed
                        // 滑动平均平滑速度
                        smoothSpeed = if (smoothSpeed == 0L) speed else (smoothSpeed * 3 + speed) / 4
                        lastBytes = downloaded
                        lastSpeedTime = now

                        // 速度限制（bytes/s，0=不限）
                        val limit = speedLimit
                        if (limit > 0 && smoothSpeed > limit) {
                            val sleepMs = ((downloaded - lastBytes) * 1000 / limit - elapsed).coerceAtLeast(20L)
                            delay(sleepMs)
                        }

                        onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f), smoothSpeed)
                        lastReport = now
                    }
                }
            }
            onProgress(1f, 0L)
        }
    }

    @Volatile
    var speedLimit: Long = 0L

    companion object {
        fun formatProgress(progress: Float): String = "%.1f%%".format(progress * 100)
    }
}