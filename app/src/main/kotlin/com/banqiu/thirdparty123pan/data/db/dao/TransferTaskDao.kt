package com.banqiu.thirdparty123pan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.banqiu.thirdparty123pan.data.db.entity.TransferTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferTaskDao {

    @Query("SELECT * FROM transfer_tasks ORDER BY id DESC")
    fun observeAll(): Flow<List<TransferTaskEntity>>

    @Query("SELECT * FROM transfer_tasks WHERE id = :id")
    suspend fun getById(id: Long): TransferTaskEntity?

    @Query("SELECT * FROM transfer_tasks WHERE status IN (:statuses)")
    suspend fun getByStatuses(statuses: List<Int>): List<TransferTaskEntity>

    @Insert
    suspend fun insert(task: TransferTaskEntity): Long

    @Update
    suspend fun update(task: TransferTaskEntity)

    @Query("UPDATE transfer_tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int)

    @Query("UPDATE transfer_tasks SET progress = :progress, speed = :speed WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Float, speed: Long)

    @Query("UPDATE transfer_tasks SET status = :status, finishTime = :finishTime WHERE id = :id")
    suspend fun finish(id: Long, status: Int, finishTime: Long)

    @Query("UPDATE transfer_tasks SET error = :error WHERE id = :id")
    suspend fun updateError(id: Long, error: String?)

    @Query("UPDATE transfer_tasks SET url = :url WHERE id = :id")
    suspend fun updateUrl(id: Long, url: String?)

    @Query("DELETE FROM transfer_tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM transfer_tasks WHERE status = 3 OR status = 5")
    suspend fun clearFinished()
}