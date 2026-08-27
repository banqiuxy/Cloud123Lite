package com.banqiu.thirdparty123pan.data.transfer

import com.banqiu.thirdparty123pan.data.api.ApiService
import com.banqiu.thirdparty123pan.data.db.dao.TransferTaskDao
import com.banqiu.thirdparty123pan.data.model.*
import com.banqiu.thirdparty123pan.di.TransferHttpClient
import com.banqiu.thirdparty123pan.util.Md5Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
        // MD5 计算回调按时间/进度节流，避免每个 1MB 都写入 Room 导致 UI 进度排队卡住。
        var lastMd5ReportAt = 0L
        var lastMd5Progress = -1f
        val md5 = Md5Utils.md5(file) { p ->
            val now = System.currentTimeMillis()
            if (p >= 1f || now - lastMd5ReportAt >= 150L || p - lastMd5Progress >= 0.02f) {
                lastMd5ReportAt = now
                lastMd5Progress = p
                onProgress(0.01f + p * 0.09f, 0L)
            }
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
        if (!resp.isSuccess) {
            throw ApiException(resp.code, "上传初始化失败：${resp.message.ifBlank { "请求失败 (${resp.code})" }}")
        }
        val data = resp.data ?: throw ApiException(resp.code, "上传响应为空")
        val effectiveFileId = data.effectiveFileId
        if (effectiveFileId <= 0L) {
            throw IOException("上传初始化未返回有效 FileId，不能确认上传结果")
        }

        if (data.reuse) {
            onProgress(1f, 0L)
            return@withContext
        }

        val bucket = data.bucket ?: throw IOException("缺少 Bucket")
        val key = data.key ?: throw IOException("缺少 Key")
        val uploadId = data.uploadId ?: throw IOException("缺少 UploadId")
        val storageNode = data.storageNode?.takeIf { it.isNotBlank() }
            ?: throw IOException("缺少 StorageNode")
        val fileId = effectiveFileId
        if (fileId <= 0L) {
            throw IOException("上传响应缺少有效 FileId，无法确认云端文件")
        }

        // 2. 查询已上传分片（断点续传）
        val uploadedParts = try {
            val partsResponse = api.s3ListParts(S3ListPartsRequest(bucket, key, uploadId, storageNode))
            if (!partsResponse.isSuccess) {
                throw ApiException(
                    partsResponse.code,
                    partsResponse.message.ifBlank { "获取已上传分片列表失败 (${partsResponse.code})" }
                )
            }
            partsResponse.data?.allParts?.map { it.partNumber }?.toSet() ?: emptySet()
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw IOException("获取已上传分片列表失败: ${e.message}", e)
        }

        // 3-4. 批量预签名 URL + 并发 PUT
        val totalParts = ((file.length() + partSize - 1) / partSize).toInt()
        if (totalParts == 0) throw IOException("文件为空")
        val batchSize = 4 // 参考客户端最多 4 个上传线程，降低服务端分片列表丢失概率
        val initialUploadedBytes = uploadedParts.sumOf { partNumber ->
            val offset = (partNumber - 1) * partSize
            minOf(partSize, (file.length() - offset).coerceAtLeast(0L))
        }
        val progressReporter = UploadProgressReporter(
            totalBytes = file.length(),
            initialBytes = initialUploadedBytes,
            onProgress = onProgress
        )
        progressReporter.report(force = true)

        for (start in 1..totalParts step batchSize) {
            val end = minOf(start + batchSize - 1, totalParts)
            val urlsResp = api.s3PrepareParts(
                // 参考客户端使用半开区间 [start, end)，因此 end 为下一分片编号。
                S3PartRequest(bucket, key, start, end + 1, uploadId, storageNode)
            )
            if (!urlsResp.isSuccess) {
                throw ApiException(
                    urlsResp.code,
                    urlsResp.message.ifBlank { "获取上传分片地址失败 (${urlsResp.code})" }
                )
            }
            val urls = urlsResp.data?.allUrls
                ?: throw IOException("预签名分片地址响应为空")
            coroutineScope {
                for (partNum in start..end) {
                    if (partNum in uploadedParts) continue
                    launch {
                        val partUrl = urls[partNum.toString()]
                            ?: fetchSinglePresignedUrl(
                                bucket = bucket,
                                key = key,
                                uploadId = uploadId,
                                storageNode = storageNode,
                                partNumber = partNum
                            )
                        val bytes = readPart(file, partNum)
                        putPartWithRetry(partUrl, bytes)
                        progressReporter.addUploaded(bytes.size.toLong())
                    }
                }
            }
            progressReporter.report(force = true)
        }

        // 5. 确认分片全部上传。
        var confirmed = emptySet<Int>()
        var missing = (1..totalParts).toList()
        var confirmedAll = false
        repeat(5) { attempt ->
            if (confirmedAll) return@repeat
            delay(if (attempt == 0) 1000L else 1500L)
            val confirm = api.s3ListParts(S3ListPartsRequest(bucket, key, uploadId, storageNode))
            if (!confirm.isSuccess) {
                throw ApiException(
                    confirm.code,
                    confirm.message.ifBlank { "确认上传分片失败 (${confirm.code})" }
                )
            }
            confirmed = confirm.data?.allParts?.map { it.partNumber }?.filter { it > 0 }?.toSet() ?: emptySet()
            missing = (1..totalParts).filter { it !in confirmed }
            onProgress(
                0.94f + 0.01f * ((attempt + 1) / 5f),
                0L
            )
            confirmedAll = missing.isEmpty()
        }

        // 如果列表最终仍缺失，重新补传缺失分片一次，避免 S3 PUT 成功但列表同步延迟造成假失败。
        if (missing.isNotEmpty()) {
            for (partNumber in missing) {
                val partUrl = fetchSinglePresignedUrl(
                    bucket = bucket,
                    key = key,
                    uploadId = uploadId,
                    storageNode = storageNode,
                    partNumber = partNumber
                )
                putPartWithRetry(partUrl, readPart(file, partNumber))
                progressReporter.addUploaded(
                    minOf(partSize, (file.length() - (partNumber - 1) * partSize).coerceAtLeast(0L))
                )
            }
            delay(1500L)
            val finalConfirm = api.s3ListParts(S3ListPartsRequest(bucket, key, uploadId, storageNode))
            if (finalConfirm.isSuccess) {
                confirmed = finalConfirm.data?.allParts
                    ?.map { it.partNumber }
                    ?.filter { it > 0 }
                    ?.toSet() ?: emptySet()
                missing = (1..totalParts).filter { it !in confirmed }
            }
        }

        // 合并阶段明确显示 96%~98%，最终云端目录验证完成后才到 100%。
        onProgress(0.96f, 0L)

        // 6-7. 新版 v2 完成接口（2026-08 修复）：
        // 旧流程 s3_complete_multipart_upload + upload_complete 已被服务端废弃——
        // 所有接口仍返回 code 0 但不再落地文件，导致"上传成功但文件消失"。
        // 官方 Web 客户端现行流程：POST upload_complete/v2（同步合并并返回真实文件信息），
        // 异步合并时轮询 GET upload_complete/result。
        val v2Request = UploadCompleteV2Request(
            fileId = fileId,
            bucket = bucket,
            fileSize = file.length(),
            key = key,
            isMultipart = true,
            uploadId = uploadId,
            storageNode = storageNode
        )
        val completeResp = api.uploadCompleteV2(v2Request)
        if (!completeResp.isSuccess) {
            throw ApiException(
                completeResp.code,
                completeResp.message.ifBlank { "合并上传分片失败 (${completeResp.code})" }
            )
        }
        var fileInfo = completeResp.data?.fileInfo

        // 服务端异步合并时轮询结果接口（对齐官方行为，间隔取响应 duration，默认 2s）。
        if (fileInfo == null || fileInfo.fileId <= 0L) {
            var pollDuration = completeResp.data?.duration ?: 2
            var attempts = 0
            while (attempts < 30 && (fileInfo == null || fileInfo.fileId <= 0L)) {
                delay(pollDuration * 1000L)
                val pollResp = api.uploadCompleteResult(
                    fileId = fileId,
                    bucket = bucket,
                    fileSize = file.length(),
                    key = key,
                    isMultipart = true,
                    uploadId = uploadId,
                    storageNode = storageNode
                )
                if (!pollResp.isSuccess) {
                    throw ApiException(
                        pollResp.code,
                        pollResp.message.ifBlank { "查询上传结果失败 (${pollResp.code})" }
                    )
                }
                pollDuration = pollResp.data?.duration ?: 2
                fileInfo = pollResp.data?.fileInfo
                attempts++
            }
        }
        if (fileInfo == null || fileInfo.fileId <= 0L) {
            throw IOException("上传完成确认超时：服务端未返回文件信息")
        }

        onProgress(1f, 0L)
    }

    /**
     * 参考客户端的单分片回退：请求半开区间 [partNumber, partNumber + 1)。
     */
    private suspend fun fetchSinglePresignedUrl(
        bucket: String,
        key: String,
        uploadId: String,
        storageNode: String,
        partNumber: Int
    ): String {
        val response = api.s3PrepareParts(
            S3PartRequest(
                bucket = bucket,
                key = key,
                partNumberStart = partNumber,
                partNumberEnd = partNumber + 1,
                uploadId = uploadId,
                storageNode = storageNode
            )
        )
        if (!response.isSuccess) {
            throw ApiException(
                response.code,
                response.message.ifBlank { "获取分片 $partNumber 预签名 URL 失败 (${response.code})" }
            )
        }
        val url = response.data?.allUrls?.get(partNumber.toString())
        return url ?: throw IOException("获取分片 $partNumber 预签名 URL 失败")
    }

    private suspend fun putPartWithRetry(url: String, bytes: ByteArray) {
        var lastError: IOException? = null
        repeat(5) { attempt ->
            try {
                putPart(url, bytes)
                return
            } catch (e: IOException) {
                lastError = e
                if (attempt < 4) delay((attempt + 1) * 750L)
            }
        }
        throw lastError ?: IOException("S3 分片上传失败")
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