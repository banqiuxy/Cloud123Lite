package com.banqiu.thirdparty123pan.domain.repository

import com.banqiu.thirdparty123pan.domain.model.FileItem
import com.banqiu.thirdparty123pan.domain.model.ShareItem

enum class FileOrderBy(val value: String) {
    FILE_ID("file_id"), NAME("file_name"), SIZE("size"), UPDATE_TIME("update_time")
}

enum class FileOrderDirection(val value: String) {
    ASC("asc"), DESC("desc")
}

/** 复制源文件（fileId + fileName，API.md §5.12 需要 fileName） */
data class CopySource(val fileId: Long, val fileName: String)

interface FileRepository {
    suspend fun listFiles(
        parentId: Long,
        orderBy: FileOrderBy = FileOrderBy.FILE_ID,
        orderDirection: FileOrderDirection = FileOrderDirection.DESC,
        search: String? = null,
        trashed: Boolean = false,
        page: Int = 1,
        limit: Int = 100
    ): List<FileItem>

    suspend fun createFolder(parentId: Long, name: String)
    suspend fun rename(fileId: Long, newName: String)
    suspend fun move(fileIds: List<Long>, targetParentId: Long)
    suspend fun copy(fileItems: List<CopySource>, targetParentId: Long)
    suspend fun trash(fileIds: List<Long>, toTrash: Boolean = true)
    suspend fun deletePermanently(fileIds: List<Long>)

    /** 解析可直连的下载 URL（含 URL 重写机制，见 DownloadUrlRewriter） */
    suspend fun resolveDownloadUrl(fileItem: FileItem): String

    /** 批量下载（文件夹）的下载链接 */
    suspend fun resolveBatchDownloadUrl(fileId: Long): String?

    suspend fun createShare(fileIds: List<Long>, password: String?, days: Int): String
    suspend fun shareList(): List<ShareItem>
    suspend fun deleteShare(shareIds: List<Long>)
}