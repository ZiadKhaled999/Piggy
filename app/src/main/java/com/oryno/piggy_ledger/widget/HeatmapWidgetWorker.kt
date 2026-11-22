package com.oryno.piggy_ledger.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class HeatmapWidgetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            HeatmapWidgetProvider.updateAllWidgetsNow(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Heatmap widget periodic refresh failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "HeatmapWidgetWorker"
        private const val UNIQUE_WORK_NAME = "HeatmapWidgetPeriodicRefresh"

        fun schedulePeriodic(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<HeatmapWidgetWorker>(
                    6, TimeUnit.HOURS
                ).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule HeatmapWidgetWorker", e)
            }
        }

        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel HeatmapWidgetWorker", e)
            }
        }
    }
}
