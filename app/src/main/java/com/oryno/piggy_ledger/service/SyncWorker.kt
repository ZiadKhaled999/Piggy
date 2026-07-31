package com.oryno.piggy_ledger.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val syncManager = SyncManager(applicationContext)
            syncManager.syncAll()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
