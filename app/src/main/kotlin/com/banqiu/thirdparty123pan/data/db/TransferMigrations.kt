package com.banqiu.thirdparty123pan.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object TransferMigrations {
    val FROM_2_TO_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transfer_tasks ADD COLUMN storageKind TEXT NOT NULL DEFAULT 'legacy'")
            listOf("sourceName", "directoryUri", "outputUri", "stagingPath", "conflictUri", "overwriteUri").forEach {
                db.execSQL("ALTER TABLE transfer_tasks ADD COLUMN $it TEXT")
            }
            db.execSQL("ALTER TABLE transfer_tasks ADD COLUMN phase TEXT NOT NULL DEFAULT 'download'")
            db.execSQL("ALTER TABLE transfer_tasks ADD COLUMN ownsOutput INTEGER NOT NULL DEFAULT 0")
        }
    }
}
