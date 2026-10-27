package com.oryno.piggy_ledger.service

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.clerk.api.Clerk
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // FIX(sync): previously the syncAll() Boolean was discarded and success()
        // was always returned, so offline/401/500 failures were forgotten until
        // logout. Retry (with backoff) on failure; logged-out = nothing to do.
        if (Clerk.userFlow.value == null) return Result.success()
        return try {
            val ok = SyncManager(applicationContext).syncAll()
            if (ok) {
                Result.success()
            } else if (runAttemptCount >= MAX_ATTEMPTS) {
                Log.w("SyncWorker", "syncAll() failed $MAX_ATTEMPTS times, giving up until next trigger")
                Result.failure()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.w("SyncWorker", "syncAll() threw, attempt $runAttemptCount", e)
            if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val MAX_ATTEMPTS = 3
        const val UNIQUE_ONE_TIME = "SyncWork"
        const val UNIQUE_PERIODIC = "SyncPeriodic"

        private fun syncConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // FIX(sync): single scheduling entry-point. KEEP (not REPLACE) coalesces
        // rapid writes into one pending job since syncAll() flushes everything;
        // CONNECTED constraint lets offline edits wait for reconnect instead of
        // running, failing, and being forgotten.
        fun scheduleOneTime(context: Context) {
            try {
                val req = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(syncConstraints())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    UNIQUE_ONE_TIME,
                    ExistingWorkPolicy.KEEP,
                    req
                )
            } catch (e: Exception) {
                // FIX(first-install-crash): a broken WorkManager DB must not crash
                // the DB write that triggered the sync.
                Log.e(TAG, "SyncWork enqueue failed, will retry later", e)
            }
        }

        fun schedulePeriodic(context: Context) {
            try {
                val req = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                    .setConstraints(syncConstraints())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    req
                )
            } catch (e: Exception) {
                Log.e(TAG, "SyncPeriodic schedule failed", e)
            }
        }

        // FIX(sync): logout wipes Room; a stray queued/periodic worker pulling
        // after the wipe (while still authed) would repopulate just-deleted rows.
        fun cancelAll(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_ONE_TIME)
                WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC)
            } catch (e: Exception) {
                Log.e(TAG, "SyncWork cancel failed", e)
            }
        }
    }
}
