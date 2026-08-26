package com.banqiu.thirdparty123pan.data.transfer

import com.banqiu.thirdparty123pan.domain.repository.FileRepository
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云端上传结果确认器。
 * upload_complete 成功后重新拉取目标目录，避免“本地显示成功但云端不可见”的假成功。
 */
@Singleton
class UploadVerifier @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend fun verifyWithProgress(
        taskName: String,
        parentId: Long,
        size: Long,
        expectedFileId: Long = 0L,
        onProgress: (Float) -> Unit
    ): Boolean {
        val pageCount = 10
        repeat(pageCount) { attempt ->
            val files = try {
                val searched = fileRepository.listFiles(
                    parentId = parentId,
                    search = taskName,
                    page = 1,
                    limit = 1000
                )
                if (searched.isNotEmpty()) {
                    searched
                } else {
                    fileRepository.listFiles(
                        parentId = parentId,
                        page = attempt + 1,
                        limit = 1000
                    )
                }
            } catch (e: Exception) {
                throw IOException("上传后查询目标目录失败: ${e.message}", e)
            }

            val found = if (expectedFileId > 0L) {
                // 只认本次 upload_request 返回的 FileId，禁止同名同大小兜底造成假成功。
                files.any { it.fileId == expectedFileId }
            } else {
                false
            }
            if (found) {
                onProgress(1f)
                return true
            }
            onProgress((attempt + 1) / pageCount.toFloat())
            if (attempt < pageCount - 1) delay(1000L + attempt * 500L)
        }
        return false
    }
}