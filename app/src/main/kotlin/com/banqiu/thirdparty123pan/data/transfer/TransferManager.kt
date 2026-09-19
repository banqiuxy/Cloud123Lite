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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val storage: DownloadTargetStorage,
    private val settingsStore: SettingsStore,
    @ApplicationContext private val context: Context
) : TransferRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val queueMutex = Mutex()
    private val manuallyPaused = ConcurrentHashMap.newKeySet<Long>()

    override val tasks: Flow<List<TransferTask>> =
        dao.observeAll().map { list -> list.map { TransferTask.fromEntity(it) } }

    init {
        scope.launch {
            settingsStore.speedLimit.collect { downloadEngine.speedLimit = it }
        }
        createNotificationChannel()
    }

    override fun addDownload(fileItem: FileItem) {
        scope.launch {
            val directory = settingsStore.downloadDir.first()
            val id = dao.insert(
                TransferTaskEntity(
                    type = 1,
                    fileId = fileItem.fileId,
                    storageKind = if (directory.isBlank()) "public" else "saf",
                    directoryUri = directory.takeIf { it.isNotBlank() },
                    remoteDirId = fileItem.parentId,
                    name = fileItem.name,
                    sourceName = fileItem.name,
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

    override fun resolveConflict(id: Long, overwrite: Boolean, newName: String?) {
        scope.launch {
            queueMutex.withLock {
                val task = dao.getById(id) ?: return@withLock
                if (task.status != 6) return@withLock
                try {
                    val name = newName ?: task.name
                    validateDownloadName(name)
                    if (overwrite && task.conflictUri == null) throw IllegalArgumentException("请重新检查目标文件")
                    val reset = if (overwrite && task.ownsOutput && task.outputUri != null) {
                        storage.discardOutput(task)
                        task.copy(outputUri = null, ownsOutput = false)
                    } else task
                    dao.update(reset.copy(name = name, overwriteUri = if (overwrite) task.conflictUri else null,
                        conflictUri = null, error = null, status = 0))
                } catch (e: Exception) { dao.updateError(id, e.message ?: "文件名无效") }
            }
            pump()
        }
    }

    override fun pause(id: Long) {
        scope.launch {
            queueMutex.withLock {
                val task = dao.getById(id) ?: return@withLock
                if (task.status != 0 && task.status != 1) return@withLock
                manuallyPaused.add(id)
                activeJobs[id]?.cancel() ?: dao.updateStatus(id, 2)
            }
        }
    }

    override fun resume(id: Long) {
        scope.launch {
            queueMutex.withLock {
                val task = dao.getById(id) ?: return@withLock
                if (task.status != 2 || activeJobs.containsKey(id)) return@withLock
                manuallyPaused.remove(id)
                dao.updateStatus(id, 0)
            }
            pump()
        }
    }

    override fun cancel(id: Long) {
        scope.launch {
            queueMutex.withLock {
                manuallyPaused.remove(id)
                val task = dao.getById(id) ?: return@withLock
                if (task.status == 3 || task.status == 5) return@withLock
                val job = activeJobs[id]
                if (job != null) job.cancel()
                else {
                    runCatching { storage.cleanup(task) }.onFailure { dao.updateError(id, "清理未完成文件失败：${it.message}") }
                    dao.finish(id, 5, System.currentTimeMillis())
                }
            }
        }
    }

    override fun retry(id: Long) {
        scope.launch {
            queueMutex.withLock {
                val task = dao.getById(id) ?: return@withLock
                if (task.status != 4 || activeJobs.containsKey(id)) return@withLock
                dao.update(task.copy(error = null, url = null, status = 0,
                    overwriteUri = null, conflictUri = null,
                    outputUri = if (task.ownsOutput) task.outputUri else null))
            }
            pump()
        }
    }

    override fun clearFinished() { scope.launch { dao.clearFinished() } }
    override fun remove(id: Long) {
        scope.launch {
            queueMutex.withLock {
                val task = dao.getById(id) ?: return@withLock
                if (activeJobs.containsKey(id)) return@withLock
                if (task.status != 3) runCatching { storage.cleanup(task) }
                dao.delete(id)
            }
        }
    }

    suspend fun resumeInterrupted() {
        queueMutex.withLock {
            dao.getByStatuses(listOf(1)).filterNot { activeJobs.containsKey(it.id) }.forEach { dao.updateStatus(it.id, 0) }
        }
        pump()
    }

    private suspend fun pump() = queueMutex.withLock {
        val concurrency = settingsStore.concurrency.first().coerceIn(1, 3)
        val running = dao.getByStatuses(listOf(1)).toMutableList()
        for (task in dao.getByStatuses(listOf(0)).sortedBy { it.id }) {
            if (activeJobs.size >= concurrency) break
            if (activeJobs.containsKey(task.id)) continue
            // Serialize only identical destinations, so a second task sees the first published file.
            if (task.type == 1 && running.any { it.type == 1 && it.storageKind == task.storageKind &&
                    it.directoryUri == task.directoryUri && it.name == task.name }) continue
            try {
                if (task.type == 1) storage.checkConflict(task)
                dao.updateStatus(task.id, 1)
                startTask(task)
                running.add(task)
            } catch (e: DownloadConflict) {
                dao.update(task.copy(status = 6, conflictUri = e.existingUri, error = e.message))
            } catch (e: Exception) {
                dao.update(task.copy(status = 4, error = e.message))
            }
        }
    }

    private fun startTask(task: TransferTaskEntity) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                if (task.type == 1) downloadEngine.download(task.id) { progress, speed -> updateProgress(task.id, progress, speed) }
                else uploadEngine.upload(task.id) { progress, speed -> updateProgress(task.id, progress, speed) }
                dao.updateProgress(task.id, 1f, 0)
                dao.finish(task.id, 3, System.currentTimeMillis())
                notifyFinished(task.name)
            } catch (e: CancellationException) {
                withContext(NonCancellable) {
                    val latest = dao.getById(task.id)
                    if (latest?.phase == "published") dao.finish(task.id, 3, System.currentTimeMillis())
                    else if (manuallyPaused.contains(task.id)) dao.updateStatus(task.id, 2)
                    else {
                        if (latest != null) runCatching { storage.cleanup(latest) }
                            .onFailure { dao.updateError(task.id, "清理未完成文件失败：${it.message}") }
                        dao.finish(task.id, 5, System.currentTimeMillis())
                    }
                }
                throw e
            } catch (e: DownloadConflict) {
                dao.getById(task.id)?.let { dao.update(it.copy(status = 6, conflictUri = e.existingUri, error = e.message)) }
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
        job.start()
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