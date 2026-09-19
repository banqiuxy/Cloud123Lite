package com.banqiu.thirdparty123pan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.banqiu.thirdparty123pan.data.db.dao.TransferTaskDao
import com.banqiu.thirdparty123pan.data.db.entity.TransferTaskEntity

@Database(
    entities = [TransferTaskEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transferTaskDao(): TransferTaskDao
}