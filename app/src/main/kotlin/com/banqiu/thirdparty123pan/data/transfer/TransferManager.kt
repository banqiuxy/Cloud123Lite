package com.banqiu.thirdparty123pan.data.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.banqiu.thirdparty123pan.R
import com.banqiu.thirdparty123pan.data.db.dao.TransferTaskDao
import com.banqiu.thirdparty123pan.data.db.entity.TransferTaskEntity
import com.banqiu.thirdparty123pan.data.prefs.SettingsStore
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.domain.model.TransferTask
import com.banqiu.thirdparty123pan.domain.repository.TransferRepository
import com.banqiu.thirdparty123pan.util.Formatters
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * 传输管理器：
 * - Room 持久化任务状态
 * - 并发控制（默认 3 个任务）
 * - 暂停/继续/取消/重试
 * - 后台传输通知
 */
@Singleton
class TransferManager @Inject constructor(
    private val dao: TransferTaskDao,
    private val downloadEngine: DownloadEngine,
    private val uploadEngine: UploadEngine,
    private val settingsStore: SettingsStore,
    @ApplicationContext private val context: Context
) : TransferRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val manuallyPaused = ConcurrentHashMap.newKeySet<Long>()

    override val tasks: Flow<List<TransferTask>> =
        dao.observeAll().map { list -> list.map { TransferTask.fromEntity(it) } }

    init {
        scope.launch {
            settingsStore.speedLimit.collect { downloadEngine.speedLimit = it }
        }
        createNotificationChannel()
    }

    override fun addDownload(fileItem: FileItem, destination: String) {
        scope.launch {
            val id = dao.insert(
                TransferTaskEntity(
                    type = 1,
                    fileId = fileItem.fileId,
                    localPath = destination,
                    remoteDirId = fileItem.parentId,
                    name = fileItem.name,
                    size = fileItem.size,
                    etag = fileItem.etag,
                    s3KeyFlag = fileItem.s3KeyFlag,
                    status = 0
                )
            )
            pump()
        }
    }

    override fun addUpload(localPath: String, remoteDirId: Long, name: String) {
        val file = File(localPath)
        scope.launch {
            if (!file.exists() || !file.isFile) {
                return@launch
            }
            val id = dao.insert(
                TransferTaskEntity(
                    type = 0,
                    localPath = file.absolutePath,
                    remoteDirId = remoteDirId,
                    name = name.ifBlank { file.name },
                    size = file.length(),
                    status = 0
                )
            )
            pump()
        }
    }

    override fun pause(id: Long) {
        manuallyPaused.add(id)
        activeJobs[id]?.cancel()
    }

    override fun resume(id: Long) {
        manuallyPaused.remove(id)
        scope.launch {
            dao.updateStatus(id, 0)
            pump()
        }
    }

    override fun cancel(id: Long) {
        manuallyPaused.remove(id)
        activeJobs[id]?.cancel()
        scope.launch {
            val task = dao.getById(id) ?: return@launch
            if (task.status == 0 || task.status == 1) {
                dao.finish(id, 5, System.currentTimeMillis())
            }
            // 清理残留的临时文件
            task.localPath?.let { File(it).takeIf { f -> f.exists() && f.length() < task.size }?.delete() }
        }
    }

    override fun retry(id: Long) {
        scope.launch {
            dao.updateError(id, null)
            dao.updateUrl(id, null) // 强制重新解析下载链接
            dao.updateStatus(id, 0)
            pump()
        }
    }

    override fun clearFinished() {
        scope.launch { dao.clearFinished() }
    }

    override fun remove(id: Long) {
        scope.launch { dao.delete(id) }
    }

    /** 应用/进程重启后恢复中断任务 */
    suspend fun resumeInterrupted() {
        val interrupted = dao.getByStatuses(listOf(0, 1))
        interrupted.forEach { dao.updateStatus(it.id, 0) }
        pump()
    }

    private suspend fun pump() {
        val concurrency = settingsStore.concurrency.first()
        val running = dao.getByStatuses(listOf(1)).size
        val slots = (concurrency - running).coerceAtLeast(0)
        if (slots <= 0) return
        val waiting = dao.getByStatuses(listOf(0)).take(slots)
        waiting.forEach { startTask(it) }
    }

    private fun startTask(task: TransferTaskEntity) {
        val job = scope.launch {
            dao.updateStatus(task.id, 1)
            try {
                if (task.type == 1) {
                    downloadEngine.download(task.id) { progress, speed ->
                        updateProgress(task.id, progress, speed)
                    }
                } else {
                    uploadEngine.upload(task.id) { progress, speed ->
                        updateProgress(task.id, progress, speed)
                    }
                }
                dao.finish(task.id, 3, System.currentTimeMillis())
                notifyFinished(task.name)
            } catch (e: CancellationException) {
                if (manuallyPaused.contains(task.id)) {
                    dao.updateStatus(task.id, 2)
                } else {
                    dao.finish(task.id, 5, System.currentTimeMillis())
                }
                throw e
            } catch (e: Exception) {
                dao.updateError(task.id, e.message ?: "未知错误")
                dao.finish(task.id, 4, System.currentTimeMillis())
                notifyFailed(task.name)
            } finally {
                activeJobs.remove(task.id)
                scope.launch { pump() }
            }
        }
        activeJobs[task.id] = job
    }

    private fun updateProgress(id: Long, progress: Float, speed: Long) {
        scope.launch {
            dao.updateProgress(id, progress, speed)
        }
    }

    // ==================== 通知 ====================

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "传输进度", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "上传下载任务进度通知" }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun notifyFinished(name: String) {
        scope.launch {
            if (!settingsStore.notificationsEnabled.first()) return@launch
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return@launch
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("传输完成")
                .setContentText(name)
                .setAutoCancel(true)
                .build()
            runCatching {
                NotificationManagerCompat.from(context)
                    .notify(Random.nextInt(1000, 9999), notification)
            }
        }
    }

    private fun notifyFailed(name: String) {
        scope.launch {
            if (!settingsStore.notificationsEnabled.first()) return@launch
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return@launch
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("传输失败")
                .setContentText(name)
                .setAutoCancel(true)
                .build()
            runCatching {
                NotificationManagerCompat.from(context)
                    .notify(Random.nextInt(1000, 9999), notification)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "transfer_progress"
    }
}