package com.banqiu.thirdparty123pan.di

import com.banqiu.thirdparty123pan.data.repository.AuthRepositoryImpl
import com.banqiu.thirdparty123pan.data.repository.FileRepositoryImpl
import com.banqiu.thirdparty123pan.data.transfer.TransferManager
import com.banqiu.thirdparty123pan.domain.repository.AuthRepository
import com.banqiu.thirdparty123pan.domain.repository.FileRepository
import com.banqiu.thirdparty123pan.domain.repository.TransferRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Domain repository interfaces and their data-layer implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    abstract fun bindTransferRepository(impl: TransferManager): TransferRepository
}
