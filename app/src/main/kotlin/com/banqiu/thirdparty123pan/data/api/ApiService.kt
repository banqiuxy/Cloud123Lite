package com.banqiu.thirdparty123pan.data.api

import com.banqiu.thirdparty123pan.data.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 123pan API（依据 API.md 端点清单）
 *
 * 域名说明：
 * - 相对路径走主域名 https://www.123pan.cn（自动容灾切换）
 * - 完整 URL 为硬编码专用域名（login.123pan.com / api.123278.com），不走容灾
 */
interface ApiService {

    // ==================== 5.1 账号密码登录 ====================
    @POST("b/api/user/sign_in")
    suspend fun signIn(@Body body: SignInRequest): ApiResponse<SignInData>

    // ==================== 5.2 二维码登录 ====================
    @Headers(
        "platform: web",
        "app-version: 3",
        "content-type: application/json;charset=UTF-8"
    )
    @GET("https://login.123pan.com/api/user/qr-code/generate")
    suspend fun qrGenerate(): ApiResponse<QrGenerateData>

    @Headers(
        "platform: web",
        "app-version: 3",
        "content-type: application/json;charset=UTF-8"
    )
    @GET("https://login.123pan.com/api/user/qr-code/result")
    suspend fun qrResult(@Query("uniID") uniId: String): ApiResponse<QrResultData>

    @Headers(
        "platform: web",
        "app-version: 3",
        "content-type: application/json;charset=UTF-8"
    )
    @POST("https://login.123pan.com/api/user/qr-code/wx_code")
    suspend fun qrWxCode(@Body body: WxCodeRequest): ApiResponse<Map<String, String>>

    // ==================== 5.3 用户信息 ====================
    @GET("b/api/user/info")
    suspend fun userInfo(): ApiResponse<UserInfoData>

    // ==================== 5.4 设备列表 ====================
    @GET("b/api/user/device_list")
    suspend fun deviceList(
        @Query("driveId") driveId: Int = 0,
        @Query("maxTime") maxTime: Int = 0,
        @Query("operateType") operateType: Int = 2,
        @Query("deviceType") deviceType: Int = 2
    ): ApiResponse<Map<String, Any>>

    // ==================== 5.5 文件列表 ====================
    @GET("api/file/list/new")
    suspend fun fileList(
        @Query("driveId") driveId: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("next") next: Int = 0,
        @Query("orderBy") orderBy: String = "file_id",
        @Query("orderDirection") orderDirection: String = "desc",
        @Query("parentFileId") parentFileId: Long = 0,
        @Query("trashed") trashed: String = "false",
        @Query("SearchData") searchData: String? = null,
        @Query("Page") page: Int = 1,
        @Query("OnlyLookAbnormalFile") onlyLookAbnormalFile: Int = 0
    ): ApiResponse<FileListData>

    // ==================== 5.6 创建文件夹（复用 upload_request） ====================
    @POST("a/api/file/upload_request")
    suspend fun createFolder(@Body body: CreateFolderRequest): ApiResponse<UploadRequestData>

    // ==================== 5.7 删除/恢复（回收站） ====================
    @POST("a/api/file/trash")
    suspend fun trash(@Body body: TrashRequest): ApiResponse<Unit>

    // ==================== 5.8 永久删除 ====================
    @POST("https://api.123278.com/b/api/file/delete")
    suspend fun deletePermanently(@Body body: DeleteRequest): ApiResponse<Unit>

    // ==================== 5.9 下载链接 ====================
    @POST("a/api/file/download_info")
    suspend fun downloadInfo(@Body body: DownloadInfoRequest): ApiResponse<DownloadInfoData>

    @POST("a/api/file/batch_download_info")
    suspend fun batchDownloadInfo(@Body body: BatchDownloadRequest): ApiResponse<DownloadInfoData>

    // ==================== 5.10 重命名 ====================
    @POST("a/api/file/rename")
    suspend fun rename(@Body body: RenameRequest): ApiResponse<Unit>

    // ==================== 5.11 移动 ====================
    @POST("b/api/file/mod_pid")
    suspend fun move(@Body body: MoveRequest): ApiResponse<Unit>

    // ==================== 5.12 复制（异步） ====================
    @POST("b/api/restful/goapi/v1/file/copy/async")
    suspend fun copyAsync(@Body body: CopyRequest): ApiResponse<CopyTaskData>

    // ==================== 5.13 复制任务查询 ====================
    @GET("b/api/restful/goapi/v1/file/copy/task")
    suspend fun copyTask(@Query("taskId") taskId: String): ApiResponse<Map<String, Any>>

    // ==================== 5.14 上传请求（含秒传） ====================
    // 2026-08 修复：上传链路统一迁移到 a/api 网关（官方 Web 客户端 postUrl 前缀）。
    // b/api 旧链路的最终落地管道已被服务端废弃。
    @POST("a/api/file/upload_request")
    suspend fun uploadRequest(@Body body: UploadRequest): ApiResponse<UploadRequestData>

    // ==================== 5.15 S3 分片预签名 URL ====================
    @POST("a/api/file/s3_repare_upload_parts_batch")
    suspend fun s3PrepareParts(@Body body: S3PartRequest): ApiResponse<S3PartData>

    // ==================== 5.16 S3 分片列表 ====================
    @POST("a/api/file/s3_list_upload_parts")
    suspend fun s3ListParts(@Body body: S3ListPartsRequest): ApiResponse<S3ListPartsData>

    // ==================== 5.17 S3 完成分片上传（已废弃） ====================
    // 官方 Web 客户端已不调用该接口，合并动作由 upload_complete/v2 一并完成。
    // 保留定义仅供回滚，勿在新代码中使用。
    @POST("a/api/file/s3_complete_multipart_upload")
    suspend fun s3CompleteMultipart(@Body body: S3ListPartsRequest): ApiResponse<Unit>

    // ==================== 5.18 上传完成确认（新版 v2） ====================
    // 2026-08 修复：旧版 b/api/file/upload_complete 已废弃——仍返回 code 0
    // 但服务端不再创建文件（API 全成功、文件不落地的假成功）。
    // 还原自官方 Web 客户端：POST file/upload_complete/v2，同步返回 file_info；
    // 异步合并时轮询 GET file/upload_complete/result。
    @POST("a/api/file/upload_complete/v2")
    suspend fun uploadCompleteV2(@Body body: UploadCompleteV2Request): ApiResponse<UploadCompleteV2Data>

    @GET("a/api/file/upload_complete/result")
    suspend fun uploadCompleteResult(
        @Query("fileId") fileId: Long,
        @Query("bucket") bucket: String,
        @Query("fileSize") fileSize: Long,
        @Query("key") key: String,
        @Query("isMultipart") isMultipart: Boolean,
        @Query("uploadId") uploadId: String,
        @Query("StorageNode") storageNode: String
    ): ApiResponse<UploadCompleteV2Data>

    // ==================== 5.19 分享创建 ====================
    // 官网当前生产环境使用 api.123278.com 的 b/api 网关。
    @POST("https://api.123278.com/b/api/share/create")
    suspend fun shareCreate(@Body body: ShareCreateRequest): ApiResponse<ShareCreateData>

    // ==================== 5.20/5.21 分享列表 ====================
    @GET("https://api.123278.com/b/api/share/list")
    suspend fun shareList(
        @Query("driveId") driveId: Int = 0,
        @Query("limit") limit: Int = 500,
        @Query("next") next: Int = 0,
        @Query("orderBy") orderBy: String = "fileId",
        @Query("orderDirection") orderDirection: String = "desc",
        @Query("SearchData") searchData: String? = null,
        @Query("event") event: String = "shareListFile",
        @Query("operateType") operateType: Int = 1
    ): ApiResponse<ShareListData>

    // ==================== 5.22 删除分享 ====================
    @POST("https://api.123278.com/b/api/share/delete")
    suspend fun shareDelete(@Body body: ShareDeleteRequest): ApiResponse<Unit>

    // ==================== 5.23 离线下载解析 ====================
    @POST("https://api.123278.com/b/api/v2/offline_download/task/resolve")
    suspend fun offlineResolve(@Body body: OfflineResolveRequest): ApiResponse<Map<String, Any>>

    // ==================== 5.24 离线下载提交 ====================
    @POST("https://api.123278.com/b/api/v2/offline_download/task/submit")
    suspend fun offlineSubmit(@Body body: OfflineSubmitRequest): ApiResponse<Map<String, Any>>
}