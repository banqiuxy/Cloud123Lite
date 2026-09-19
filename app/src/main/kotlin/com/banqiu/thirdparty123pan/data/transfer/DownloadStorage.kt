package com.banqiu.thirdparty123pan.data.transfer

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.banqiu.thirdparty123pan.data.db.dao.TransferTaskDao
import com.banqiu.thirdparty123pan.data.db.entity.TransferTaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

class DownloadConflict(val existingUri: String) : IOException("目标目录已有同名文件，请选择覆盖、重命名或取消")

internal fun validateDownloadName(name: String) {
    require(name.isNotBlank() && name == name.trim() && name != "." && name != ".." &&
        name.none { it == '/' || it == '\\' || it.code < 32 } && name.toByteArray(Charsets.UTF_8).size <= 240) {
        "文件名不能为空，不能包含路径分隔符或控制字符，且不能超过 240 字节"
    }
}

interface DownloadTargetStorage {
    fun stagingFile(task: TransferTaskEntity): File
    fun validateDirectory(directory: String)
    fun directoryLabel(directory: String): String
    suspend fun checkConflict(task: TransferTaskEntity)
    suspend fun publish(taskId: Long)
    suspend fun discardOutput(task: TransferTaskEntity)
    suspend fun cleanup(task: TransferTaskEntity)
}

/** All public/SAF writes go through content URIs; legacy tasks keep their original files. */
@Singleton
class DownloadStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TransferTaskDao
) : DownloadTargetStorage {
    private val resolver get() = context.contentResolver
    private val collection get() = MediaStore.Downloads.EXTERNAL_CONTENT_URI
    private val relativePath = "Download/Cloud123Lite/"

    override fun stagingFile(task: TransferTaskEntity): File = if (task.storageKind == "legacy") {
        File(task.localPath ?: throw IOException("旧任务下载路径无效"))
    } else File(task.stagingPath ?: File(context.getExternalFilesDir(null) ?: context.filesDir,
        "download-staging/${task.id}.part").absolutePath)

    override fun validateDirectory(directory: String) {
        if (directory.isBlank()) return
        val tree = Uri.parse(directory)
        if (tree.scheme != "content" || !DocumentsContract.isTreeUri(tree)) throw IOException("目录设置无效，请重新选择下载目录")
        if (resolver.persistedUriPermissions.none { it.uri == tree && it.isReadPermission && it.isWritePermission }) {
            throw IOException("下载目录授权已失效，请在设置中重新选择目录")
        }
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        resolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_FLAGS), null, null, null)?.use {
            if (!it.moveToFirst() || it.getInt(0) and DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE == 0)
                throw IOException("所选目录不可写，请重新选择")
        } ?: throw IOException("无法访问所选目录")
    }

    override fun directoryLabel(directory: String): String {
        if (directory.isBlank()) return "公共存储 / Download / Cloud123Lite"
        return runCatching {
            validateDirectory(directory)
            val tree = Uri.parse(directory)
            DocumentsContract.getTreeDocumentId(tree).replace(':', '/')
        }.getOrElse { "授权失效，请重新选择目录" }
    }

    private fun findDocument(tree: Uri, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) if (cursor.getString(1) == name)
                return DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
        } ?: throw IOException("无法读取目标目录，不能安全检查同名文件")
        return null
    }

    private fun existing(task: TransferTaskEntity): String? {
        validateDownloadName(task.name)
        if (task.storageKind == "saf") {
            val directory = task.directoryUri ?: throw IOException("下载目录缺失")
            validateDirectory(directory)
            return findDocument(Uri.parse(directory), task.name)?.toString()
        }
        if (task.storageKind == "legacy") return null
        resolver.query(collection, arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.RELATIVE_PATH} = ? AND ${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(relativePath, task.name), null)?.use {
            while (it.moveToNext()) {
                val uri = ContentUris.withAppendedId(collection, it.getLong(0)).toString()
                if (uri != task.outputUri) return uri
            }
        } ?: throw IOException("无法查询公共 Download 目录")
        return null
    }

    override suspend fun checkConflict(task: TransferTaskEntity) = withContext(Dispatchers.IO) {
        if (task.storageKind == "legacy" || task.phase == "published") return@withContext
        val found = existing(task)
        if (found != null && found != task.overwriteUri && !(task.ownsOutput && found == task.outputUri))
            throw DownloadConflict(found)
        if (task.overwriteUri != null && found != task.overwriteUri)
            throw IOException("确认覆盖的目标已变化，请重新处理同名冲突")
    }

    override suspend fun publish(taskId: Long): Unit = withContext(Dispatchers.IO) {
        var task = dao.getById(taskId) ?: throw IOException("任务不存在")
        if (task.storageKind == "legacy" || task.phase == "published") return@withContext
        checkConflict(task)
        val source = stagingFile(task)
        if (!source.isFile || (task.size > 0 && source.length() != task.size)) throw IOException("暂存文件不完整")
        // Stable temporary names let an interrupted creation recover its own output rather than duplicate it.
        val tempName = ".cloud123-${context.packageName}-${task.id}.part"
        val output: Uri = task.outputUri?.let(Uri::parse) ?: task.overwriteUri?.let(Uri::parse) ?: if (task.storageKind == "saf") {
            val tree = Uri.parse(task.directoryUri!!)
            findDocument(tree, tempName) ?: DocumentsContract.createDocument(resolver,
                DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)),
                "application/octet-stream", tempName) ?: throw IOException("无法创建下载文件")
        } else {
            var recovered: Uri? = null
            resolver.query(collection, arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.RELATIVE_PATH} = ? AND ${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.IS_PENDING} = 1",
                arrayOf(relativePath, tempName), null)?.use { if (it.moveToFirst()) recovered = ContentUris.withAppendedId(collection, it.getLong(0)) }
            recovered ?: resolver.insert(collection, ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, tempName)
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }) ?: throw IOException("无法创建公共下载文件")
        }
        task = task.copy(outputUri = output.toString(), ownsOutput = task.overwriteUri == null, phase = "committing")
        dao.update(task)
        val buffer = ByteArray(256 * 1024)
        source.inputStream().use { input ->
            (resolver.openOutputStream(output, "wt") ?: throw IOException("目标文件不可写，请重新授权或重命名")).use { stream ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    stream.write(buffer, 0, count)
                }
                stream.flush()
            }
        }
        currentCoroutineContext().ensureActive()
        checkConflict(task)
        // Finish the short publish/DB transition even if pause arrives during finalization.
        withContext(NonCancellable) {
            val finalUri = if (task.ownsOutput && task.storageKind == "saf") {
                DocumentsContract.renameDocument(resolver, output, task.name) ?: throw IOException("目录提供方不支持文件重命名")
            } else {
                if (task.ownsOutput) {
                    val count = resolver.update(output, ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, task.name)
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }, null, null)
                    if (count != 1) throw IOException("发布下载文件失败")
                }
                output
            }
            // Persist a provider's replacement URI before further checks, so retries never create a duplicate.
            task = task.copy(outputUri = finalUri.toString())
            dao.update(task)
            val displayName = resolver.query(finalUri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null }
            if (displayName != task.name) throw IOException("目录提供方改变了文件名，请检查同名文件或选择其他目录")
            dao.update(task.copy(localPath = finalUri.toString(), phase = "published"))
            source.delete()
        }
    }

    override suspend fun discardOutput(task: TransferTaskEntity): Unit = withContext(Dispatchers.IO) {
        if (!task.ownsOutput || task.phase == "published") return@withContext
        task.outputUri?.let { value ->
            val uri = Uri.parse(value)
            if (task.storageKind == "saf") DocumentsContract.deleteDocument(resolver, uri)
            else resolver.delete(uri, null, null)
        }
    }

    override suspend fun cleanup(task: TransferTaskEntity) = withContext(Dispatchers.IO) {
        if (task.type != 1 || task.storageKind == "legacy" || task.phase == "published") return@withContext
        stagingFile(task).delete()
        discardOutput(task)
    }
}
