package com.banqiu.thirdparty123pan.data.repository

import com.banqiu.thirdparty123pan.data.api.ApiService
import com.banqiu.thirdparty123pan.data.model.*
import com.banqiu.thirdparty123pan.data.transfer.DownloadUrlRewriter
import com.banqiu.thirdparty123pan.di.TransferHttpClient
import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.domain.model.ShareCreation
import com.banqiu.thirdparty123pan.domain.model.ShareItem
import com.banqiu.thirdparty123pan.domain.repository.CopySource
import com.banqiu.thirdparty123pan.domain.repository.FileOrderBy
import com.banqiu.thirdparty123pan.domain.repository.FileOrderDirection
import com.banqiu.thirdparty123pan.domain.repository.FileRepository
import okhttp3.OkHttpClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val api: ApiService,
    @TransferHttpClient private val transferClient: OkHttpClient
) : FileRepository {

    override suspend fun listFiles(
        parentId: Long,
        orderBy: FileOrderBy,
        orderDirection: FileOrderDirection,
        search: String?,
        trashed: Boolean,
        page: Int,
        limit: Int
    ): List<FileItem> {
        val resp = api.fileList(
            parentFileId = parentId,
            orderBy = orderBy.value,
            orderDirection = orderDirection.value,
            searchData = search?.takeIf { it.isNotBlank() },
            trashed = trashed.toString(),
            page = page,
            limit = limit
        )
        val data = check(resp) ?: return emptyList()
        return data.infoList.map { FileItem.fromRemote(it) }
    }

    override suspend fun createFolder(parentId: Long, name: String) {
        val resp = api.createFolder(
            CreateFolderRequest(fileName = name, parentFileId = parentId)
        )
        check(resp)
    }

    override suspend fun rename(fileId: Long, newName: String) {
        check(api.rename(RenameRequest(fileId = fileId, fileName = newName)))
    }

    override suspend fun move(fileIds: List<Long>, targetParentId: Long) {
        if (fileIds.isEmpty()) return
        check(api.move(MoveRequest(
            fileIdList = fileIds.map { FileIdWrapper(it) },
            parentFileId = targetParentId
        )))
    }

    override suspend fun copy(fileItems: List<CopySource>, targetParentId: Long) {
        if (fileItems.isEmpty()) return
        val resp = api.copyAsync(CopyRequest(
            fileList = fileItems.map { CopyFileItem(it.fileId, it.fileName) },
            targetFileId = targetParentId
        ))
        check(resp)
    }

    override suspend fun trash(fileIds: List<Long>, toTrash: Boolean) {
        if (fileIds.isEmpty()) return
        check(api.trash(TrashRequest(
            fileTrashInfoList = fileIds.map { FileIdWrapper(it) },
            operation = toTrash
        )))
    }

    override suspend fun deletePermanently(fileIds: List<Long>) {
        if (fileIds.isEmpty()) return
        check(api.deletePermanently(DeleteRequest(
            fileIdList = fileIds.map { FileIdLower(it) }
        )))
    }

    override suspend fun resolveDownloadUrl(fileItem: FileItem): String {
        if (fileItem.isFolder) {
            val batchResp = api.batchDownloadInfo(
                BatchDownloadRequest(fileIdList = listOf(FileIdLower(fileItem.fileId)))
            )
            val batchData = batchResp.data
            val raw = batchData?.redirectUrl ?: batchData?.downloadUrl
            if (raw != null) return DownloadUrlRewriter.resolve(raw, transferClient)
            return ""
        }

        // 文件：download_info，5113/5114 流量超限时不报错，继续 URL 重写
        val resp = api.downloadInfo(
            DownloadInfoRequest(
                etag = fileItem.etag,
                fileId = fileItem.fileId,
                s3keyFlag = fileItem.s3KeyFlag,
                type = 0,
                fileName = fileItem.name,
                size = fileItem.size
            )
        )
        if (!resp.isSuccess &&
            resp.code != CODE_DOWNLOAD_LIMIT_1 &&
            resp.code != CODE_DOWNLOAD_LIMIT_2
        ) {
            throw ApiException(resp.code, resp.message.ifBlank { "获取下载链接失败 (${resp.code})" })
        }
        val data = resp.data
        val raw = data?.redirectUrl ?: data?.downloadUrl
        if (raw.isNullOrEmpty()) throw ApiException(resp.code, "下载链接为空")
        return DownloadUrlRewriter.resolve(raw, transferClient)
    }

    override suspend fun resolveBatchDownloadUrl(fileId: Long): String? {
        val resp = api.batchDownloadInfo(BatchDownloadRequest(fileIdList = listOf(FileIdLower(fileId))))
        if (!resp.isSuccess) return null
        val raw = resp.data?.redirectUrl ?: resp.data?.downloadUrl ?: return null
        return DownloadUrlRewriter.resolve(raw, transferClient)
    }

    override suspend fun createShare(fileIds: List<Long>, password: String?, days: Int): ShareCreation {
        if (fileIds.isEmpty()) throw ApiException(-1, "请选择要分享的文件")

        val expiration = when {
            days <= 0 -> "2099-12-12T08:00:00+08:00"
            else -> LocalDate.now().plusDays(days.toLong())
                .atTime(23, 59, 59)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "+08:00"
        }
        // 官网的 expireValue：1=1天，2=7天，3=30天，4=永久。
        val shareModality = when {
            days <= 0 -> 4
            days <= 1 -> 1
            days <= 7 -> 2
            else -> 3
        }
        val resp = api.shareCreate(
            ShareCreateRequest(
                fileIdList = fileIds.joinToString(","),
                sharePwd = password.orEmpty(),
                expiration = expiration,
                fileNum = fileIds.size,
                renameVisible = true,
                shareModality = shareModality,
                operatePlace = 1,
                trafficLimitSwitch = 1,
                trafficLimit = 0,
                trafficSwitch = 1,
                fillPwdSwitch = 0
            )
        )
        val data = check(resp)
            ?: throw ApiException(resp.code, resp.message.ifBlank { "分享响应为空" })

        val rawUrl = data.links().firstOrNull()
            ?: data.shareUrl?.takeIf { it.isNotBlank() }
            ?: data.shareUrlLower?.takeIf { it.isNotBlank() }
            ?: data.shareKey?.takeIf { it.isNotBlank() }
            ?: data.shareKeyLower?.takeIf { it.isNotBlank() }
            ?: throw ApiException(resp.code, "分享响应缺少分享链接")
        val url = normalizeShareUrl(rawUrl)
        val actualPassword = data.sharePwd?.takeIf { it.isNotBlank() }
            ?: data.sharePwdUpper?.takeIf { it.isNotBlank() }
            ?: password?.takeIf { it.isNotBlank() }
        return ShareCreation(url = url, password = actualPassword)
    }

    private fun normalizeShareUrl(value: String): String {
        val trimmed = value.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("/") -> "https://www.123pan.cn$trimmed"
            else -> "https://www.123pan.cn/s/$trimmed"
        }
    }

    override suspend fun shareList(): List<ShareItem> {
        val resp = api.shareList()
        val data = check(resp) ?: return emptyList()
        return data.infoList.map { ShareItem.fromRemote(it) }
    }

    override suspend fun deleteShare(shareIds: List<Long>) {
        if (shareIds.isEmpty()) return
        check(api.shareDelete(ShareDeleteRequest(
            shareInfoList = shareIds.map { ShareIdWrapper(it) }
        )))
    }

    private fun <T> check(resp: ApiResponse<T>): T? {
        if (!resp.isSuccess) throw ApiException(resp.code, resp.message.ifBlank { "请求失败 (${resp.code})" })
        return resp.data
    }
}