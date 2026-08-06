package com.oryno.piggy_ledger.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oryno.piggy_ledger.data.PiggyRemoteConfigManager
import com.oryno.piggy_ledger.widget.StreakWidgetProvider

class PiggyRemoteConfigWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val updated = PiggyRemoteConfigManager.fetchAndSyncConfig(applicationContext)
            if (updated) {
                StreakWidgetProvider.triggerUpdate(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
