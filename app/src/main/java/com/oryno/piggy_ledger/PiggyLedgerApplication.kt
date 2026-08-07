package com.oryno.piggy_ledger

import android.app.Application
import android.util.Log
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PiggyLedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            val config = PostHogAndroidConfig(
                apiKey = BuildConfig.POSTHOG_API_KEY,
                host = "https://us.i.posthog.com"
            ).apply {
                captureScreenViews = true
                captureApplicationLifecycleEvents = true
                sessionReplay = true
            }
            PostHogAndroid.setup(this, config)
        } catch (e: Exception) {
            Log.e("PiggyLedgerApp", "Failed to initialize PostHog", e)
        }

        try {
            if (BuildConfig.REVENUECAT_API_KEY.isNotBlank()) {
                com.revenuecat.purchases.Purchases.logLevel = com.revenuecat.purchases.LogLevel.INFO
                com.revenuecat.purchases.Purchases.configure(
                    com.revenuecat.purchases.PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build()
                )
            }
        } catch (e: Exception) {
            Log.w("PiggyLedgerApp", "RevenueCat Purchases initialization skipped/failed: ${e.message}")
        }

        val clerkKey = BuildConfig.CLERK_PUBLISHABLE_KEY
        if (clerkKey.isNotBlank()) {
            try {
                Clerk.initialize(
                    this,
                    clerkKey,
                    options = ClerkConfigurationOptions(enableDebugMode = true),
                )
            } catch (e: Exception) {
                Log.e("PiggyLedgerApp", "Failed to initialize Clerk", e)
            }
        } else {
            Log.w("PiggyLedgerApp", "Clerk Publishable Key is missing.")
        }

        // Initialize Notification Channels & Schedule Background Reminders
        try {
            com.oryno.piggy_ledger.ui.NotificationHelper(this)
            com.oryno.piggy_ledger.service.NotificationScheduler.scheduleAll(this)
        } catch (e: Exception) {
            Log.e("PiggyLedgerApp", "Failed to initialize NotificationScheduler", e)
        }

        // Schedule Remote Config & Mascot Asset Sync
        try {
            // Immediate startup sync
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val updated = com.oryno.piggy_ledger.data.PiggyRemoteConfigManager.fetchAndSyncConfig(this@PiggyLedgerApplication)
                    if (updated) {
                        com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(this@PiggyLedgerApplication)
                    }
                } catch (e: Exception) {
                    Log.e("PiggyLedgerApp", "Immediate fetch error", e)
                }
            }

            val oneTimeSync = androidx.work.OneTimeWorkRequestBuilder<com.oryno.piggy_ledger.service.PiggyRemoteConfigWorker>().build()
            androidx.work.WorkManager.getInstance(this).enqueue(oneTimeSync)

            val periodicSync = androidx.work.PeriodicWorkRequestBuilder<com.oryno.piggy_ledger.service.PiggyRemoteConfigWorker>(
                12, java.util.concurrent.TimeUnit.HOURS
            ).build()
            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PiggyRemoteConfigSync",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                periodicSync
            )
        } catch (e: Exception) {
            Log.e("PiggyLedgerApp", "Failed to schedule PiggyRemoteConfigWorker", e)
        }
    }
}
