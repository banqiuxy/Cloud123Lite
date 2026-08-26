package com.banqiu.thirdparty123pan

import android.app.Application
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.hilt.work.HiltWorkerFactory
import com.banqiu.thirdparty123pan.data.transfer.TransferResumeWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CloudApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 应用启动时调度恢复中断的传输任务（WorkManager 管理后台传输）
        val resumeRequest = OneTimeWorkRequestBuilder<TransferResumeWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this)
            .enqueueUniqueWork("transfer_resume", ExistingWorkPolicy.KEEP, resumeRequest)
    }
}
