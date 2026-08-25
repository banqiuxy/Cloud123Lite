package com.banqiu.thirdparty123pan.di

import android.content.Context
import androidx.room.Room
import com.banqiu.thirdparty123pan.data.db.AppDatabase
import com.banqiu.thirdparty123pan.data.db.dao.TransferTaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "cloud123.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideTransferTaskDao(db: AppDatabase): TransferTaskDao = db.transferTaskDao()
}