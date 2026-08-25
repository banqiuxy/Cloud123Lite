package com.banqiu.thirdparty123pan.data.transfer

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager 后台任务：应用启动时恢复中断的传输任务
 */
@HiltWorker
class TransferResumeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val transferManager: TransferManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        transferManager.resumeInterrupted()
        return Result.success()
    }
}