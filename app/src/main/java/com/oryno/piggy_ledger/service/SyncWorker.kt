package com.oryno.piggy_ledger.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.fetchToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val prefs: SharedPreferences by lazy {
        applicationContext.getSharedPreferences("sync_state", Context.MODE_PRIVATE)
    }

    override suspend fun doWork(): Result {
        // FIX(sync): previously the syncAll() Boolean was discarded and success()
        // was always returned, so offline/401/500 failures were forgotten until
        // logout. Retry (with backoff) on failure; logged-out = nothing to do.
        if (Clerk.userFlow.value == null) return Result.success()

        return try {
            // FIX(sync): refresh token BEFORE sync attempt. If 401/unauth, this
            // gives us a fresh token. If refresh also fails, user is truly
            // signed out — fail fast, don't retry forever.
            val authHeader = getValidAuthHeader()
            if (authHeader == null) {
                Log.w("SyncWorker", "No valid auth header after refresh, user likely signed out")
                return Result.failure()
            }

            val ok = SyncManager(applicationContext).syncAll(authHeader)
            if (ok) {
                prefs.edit().putBoolean("needs_full_resync", false).apply()
                Result.success()
            } else {
                // syncAll returned false but didn't throw - could be 4xx/5xx
                // Check if we should retry or give up
                handleSyncFailure()
            }
        } catch (e: Exception) {
            Log.w("SyncWorker", "syncAll() threw, attempt $runAttemptCount", e)
            handleSyncFailure(e)
        }
    }

    private suspend fun getValidAuthHeader(): String? {
        val session = Clerk.sessionFlow.value ?: return null

        // First attempt: get existing token
        var result = session.fetchToken(GetTokenOptions())
        if (result is ClerkResult.Success && !result.value.jwt.isNullOrEmpty()) {
            return "Bearer ${result.value.jwt}"
        }

        // Token expired or invalid - force refresh
        Log.i("SyncWorker", "Token expired, attempting force refresh")
        try {
            result = session.fetchToken(GetTokenOptions(forceRefresh = true))
            if (result is ClerkResult.Success && !result.value.jwt.isNullOrEmpty()) {
                Log.i("SyncWorker", "Token force-refreshed successfully")
                return "Bearer ${result.value.jwt}"
            }
            Log.w("SyncWorker", "Force refresh returned no token: $result")
        } catch (e: Exception) {
            Log.w("SyncWorker", "Force refresh threw", e)
        }
        return null
    }

    private fun handleSyncFailure(e: Exception? = null): Result {
        // FIX(sync): hard cap on retries. WorkManager's exponential backoff
        // (10s -> 5h) means after ~8-10 retries the next attempt is hours away,
        // effectively "never syncs again". Cap at 8, flag for next app-open.
        if (runAttemptCount >= MAX_RETRY_ATTEMPTS) {
            Log.w("SyncWorker", "sync failed $MAX_RETRY_ATTEMPTS times, capping retries and flagging for full resync")
            prefs.edit().putBoolean("needs_full_resync", true).apply()
            return Result.failure()
        }

        // FIX(sync): distinguish error type for smarter retry
        val isTransient = when {
            e != null -> isTransientException(e)
            else -> true // syncAll returned false without exception - assume transient (5xx, timeout)
        }

        if (isTransient) {
            Log.w("SyncWorker", "Transient failure (attempt $runAttemptCount), will retry with backoff")
            Result.retry()
        } else {
            Log.w("SyncWorker", "Non-transient failure (attempt $runAttemptCount), failing fast")
            prefs.edit().putBoolean("needs_full_resync", true).apply()
            Result.failure()
        }
    }

    private fun isTransientException(e: Exception): Boolean {
        // 5xx, 404 (eventual consistency), IOException, timeout, socket exception = transient
        // 401, 403 = non-transient (auth/resource issue)
        // 400 = non-transient (bad request - won't fix itself)
        val message = e.message?.lowercase() ?: ""
        return e is java.io.IOException
            || e is java.net.SocketTimeoutException
            || e is java.net.UnknownHostException
            || message.contains("500")
            || message.contains("502")
            || message.contains("503")
            || message.contains("504")
            || message.contains("404")
            || message.contains("timeout")
            || message.contains("connection")
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val MAX_RETRY_ATTEMPTS = 8
        const val UNIQUE_ONE_TIME = "SyncWork"
        const val UNIQUE_PERIODIC = "SyncPeriodic"

        private fun syncConstraints() = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // FIX(sync): single scheduling entry-point.
        // APPEND_OR_REPLACE chains new work after current one (only replaces if failed/cancelled)
        // This prevents killing an in-flight sync when network callback fires mid-upload.
        fun scheduleOneTime(context: Context) {
            try {
                val req = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(syncConstraints())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    UNIQUE_ONE_TIME,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    req
                )
            } catch (e: Exception) {
                Log.e(TAG, "SyncWork enqueue failed, will retry later", e)
            }
        }

        // FIX(sync): UPDATE preserves schedule while applying new constraints/backoff
        fun schedulePeriodic(context: Context) {
            try {
                val req = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                    .setConstraints(syncConstraints())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_PERIODIC,
                    ExistingPeriodicWorkPolicy.UPDATE,
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

        fun needsFullResync(context: Context): Boolean {
            return context.getSharedPreferences("sync_state", Context.MODE_PRIVATE)
                .getBoolean("needs_full_resync", false)
        }

        fun clearFullResyncFlag(context: Context) {
            context.getSharedPreferences("sync_state", Context.MODE_PRIVATE)
                .edit().putBoolean("needs_full_resync", false).apply()
        }
    }
}
