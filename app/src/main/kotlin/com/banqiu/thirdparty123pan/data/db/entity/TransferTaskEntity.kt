package com.banqiu.thirdparty123pan.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_tasks")
data class TransferTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int = 1,                 // 0=上传, 1=下载
    val fileId: Long = 0,              // 远端文件ID
    val localPath: String? = null,     // 本地路径
    val remoteDirId: Long = 0,         // 上传目标目录
    val name: String = "",
    val size: Long = 0,
    val progress: Float = 0f,
    val speed: Long = 0,
    val status: Int = 0,               // 0=等待 1=进行中 2=暂停 3=成功 4=失败 5=取消
    val createTime: Long = System.currentTimeMillis(),
    val finishTime: Long? = null,
    val error: String? = null,
    // 下载请求必须携带文件列表返回的 Etag 和 S3KeyFlag（API.md §5.9）。
    val etag: String? = null,
    val s3KeyFlag: String? = null,
    val url: String? = null            // 下载：已解析的直链（断点续传复用）
)