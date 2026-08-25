package com.banqiu.thirdparty123pan.data.transfer

import com.banqiu.thirdparty123pan.data.api.ApiService
import com.banqiu.thirdparty123pan.data.db.dao.TransferTaskDao
import com.banqiu.thirdparty123pan.data.model.*
import com.banqiu.thirdparty123pan.di.TransferHttpClient
import com.banqiu.thirdparty123pan.util.Md5Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 上传引擎：完整 S3 分片上传流程（API.md §5.14-5.18）
 *
 * 1. upload_request（含秒传）→ 2. s3_list_upload_parts（断点续传）
 * 3. s3_repare_upload_parts_batch（预签名 URL）→ 4. PUT 分片到 S3（直连）
 * 5. 确认分片 → 6. s3_complete_multipart_upload → 7. upload_complete
 */
@Singleton
class UploadEngine @Inject constructor(
    private val api: ApiService,
    @TransferHttpClient private val transferClient: OkHttpClient,
    private val dao: TransferTaskDao
) {

    private val partSize = 5L * 1024 * 1024 // 5MB 分片

    suspend fun upload(taskId: Long, onProgress: (Float, Long) -> Unit) = withContext(Dispatchers.IO) {
        val task = dao.getById(taskId) ?: return@withContext
        val file = File(task.localPath ?: throw IOException("本地文件不存在"))
        if (!file.exists() || file.length() == 0L) throw IOException("本地文件不存在")

        // 1. MD5 + upload_request（秒传）
        onProgress(0.02f, 0L) // 计算 MD5 阶段
        val md5 = Md5Utils.md5(file) { p ->
            onProgress(0.02f + p * 0.03f, 0L)
        }

        var req = UploadRequest(
            etag = md5,
            fileName = task.name,
            parentFileId = task.remoteDirId,
            size = file.length(),
            type = 0,
            duplicate = 0
        )
        var resp = api.uploadRequest(req)
        // 5060 同名冲突 → duplicate=1 覆盖重试
        if (resp.code == CODE_FILE_CONFLICT) {
            resp = api.uploadRequest(req.copy(duplicate = 1))
        }
        if (!resp.isSuccess) throw ApiException(resp.code, resp.message.ifBlank { "上传请求失败" })
        val data = resp.data ?: throw ApiException(resp.code, "上传响应为空")

        // 秒传成功
        if (data.reuse) {
            onProgress(1f, 0L)
            return@withContext
        }

        val bucket = data.bucket ?: throw IOException("缺少 Bucket")
        val key = data.key ?: throw IOException("缺少 Key")
        val uploadId = data.uploadId ?: throw IOException("缺少 UploadId")
        val storageNode = data.storageNode ?: ""
        val fileId = data.fileId

        // 2. 查询已上传分片（断点续传）
        val uploadedParts = try {
            api.s3ListParts(S3ListPartsRequest(bucket, key, uploadId, storageNode))
                .data?.parts?.map { it.partNumber }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }

        // 3-4. 批量预签名 URL + 并发 PUT
        val totalParts = ((file.length() + partSize - 1) / partSize).toInt()
        if (totalParts == 0) throw IOException("文件为空")
        val batchSize = 10
        var completedBytes = uploadedParts.size.toLong() * partSize

        for (start in 1..totalParts step batchSize) {
            val end = minOf(start + batchSize - 1, totalParts)
            val urlsResp = api.s3PrepareParts(
                S3PartRequest(bucket, key, start, end, uploadId, storageNode)
            )
            if (!urlsResp.isSuccess) {
                throw ApiException(
                    urlsResp.code,
                    urlsResp.message.ifBlank { "获取上传分片地址失败 (${urlsResp.code})" }
                )
            }
            val urls = urlsResp.data?.urls
                ?: throw IOException("预签名分片地址响应为空")

            coroutineScope {
                for (partNum in start..end) {
                    if (partNum in uploadedParts) continue
                    launch {
                        val partUrl = urls[partNum.toString()]
                            ?: throw IOException("缺少分片 $partNum 的预签名 URL")
                        val bytes = readPart(file, partNum)
                        putPart(partUrl, bytes)
                    }
                }
            }

            completedBytes = minOf(file.length(), completedBytes + (end - start + 1) * partSize)
            val progress = 0.05f + 0.92f * (completedBytes.toFloat() / file.length().coerceAtLeast(1))
            onProgress(progress.coerceIn(0f, 0.97f), 0L)
        }

        // 5. 确认分片全部上传
        val confirm = api.s3ListParts(S3ListPartsRequest(bucket, key, uploadId, storageNode))
        val confirmed = confirm.data?.parts?.map { it.partNumber }?.toSet() ?: emptySet()
        val missing = (1..totalParts).filter { it !in confirmed }
        if (missing.isNotEmpty()) throw IOException("分片上传不完整: ${missing.take(5)}")

        // 6-7. 完成上传
        api.s3CompleteMultipart(S3ListPartsRequest(bucket, key, uploadId, storageNode))
        api.uploadComplete(UploadCompleteRequest(fileId))

        onProgress(1f, 0L)
    }

    private fun readPart(file: File, partNum: Int): ByteArray {
        val start = (partNum - 1) * partSize
        val length = minOf(partSize, file.length() - start)
        val bytes = ByteArray(length.toInt())
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(start)
            raf.readFully(bytes)
        }
        return bytes
    }

    private fun putPart(url: String, bytes: ByteArray) {
        val request = Request.Builder()
            .url(url)
            .put(bytes.toRequestBody(null))
            .build()
        transferClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful && resp.code != 200) {
                throw IOException("S3 分片上传失败 HTTP ${resp.code}")
            }
        }
    }
}