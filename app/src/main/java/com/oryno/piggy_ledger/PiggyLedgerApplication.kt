package com.oryno.piggy_ledger

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.oryno.piggy_ledger.data.UserPreferences
import com.oryno.piggy_ledger.service.SyncWorker
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

class PiggyLedgerApplication : Application() {

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        
        // Restore locally saved language choice immediately
        try {
            val savedLanguage = UserPreferences.getSavedAppLanguageSync(this)
            if (!savedLanguage.isNullOrBlank()) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLanguage))
            }
        } catch (e: Exception) {
            Log.e("PiggyLedgerApp", "Failed to restore saved app locale", e)
        }
        
        // FIX(first-install-crash): the repo ships no google-services.json (CI injects
        // it); only touch Crashlytics when Firebase actually initialized, otherwise
        // the release build can throw here before anything else runs.
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isNotEmpty()) {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            }
        } catch (e: Exception) {
            Log.e("PiggyLedgerApp", "Failed to initialize Crashlytics", e)
        }

        // FIX: placeholder keys from .env.example ("..._placeholder") are not blank,
        // so the old isNotBlank() check configured SDKs with invalid keys on Play
        // builds. Treat placeholders as missing.
        try {
            val posthogKey = BuildConfig.POSTHOG_API_KEY
            if (posthogKey.isNotBlank() && !posthogKey.contains("placeholder", ignoreCase = true)) {
                val config = PostHogAndroidConfig(
                    apiKey = posthogKey,
                    host = "https://us.i.posthog.com"
                ).apply {
                    captureScreenViews = true
                    captureApplicationLifecycleEvents = true
                    sessionReplay = true
                }
                PostHogAndroid.setup(this, config)
            } else {
                Log.w("PiggyLedgerApp", "PostHog disabled: no real API key")
            }
        } catch (e: Exception) {
            Log.e("PiggyLedgerApp", "Failed to initialize PostHog", e)
        }

        try {
            val revenuecatKey = BuildConfig.REVENUECAT_API_KEY
            if (revenuecatKey.isNotBlank() && !revenuecatKey.contains("placeholder", ignoreCase = true)) {
                com.revenuecat.purchases.Purchases.logLevel = com.revenuecat.purchases.LogLevel.INFO
                com.revenuecat.purchases.Purchases.configure(
                    com.revenuecat.purchases.PurchasesConfiguration.Builder(this, revenuecatKey).build()
                )
            } else {
                Log.w("PiggyLedgerApp", "RevenueCat disabled: no real API key")
            }
        } catch (e: Exception) {
            Log.w("PiggyLedgerApp", "RevenueCat Purchases initialization skipped/failed: ${e.message}")
        }

        val clerkKey = BuildConfig.CLERK_PUBLISHABLE_KEY
        // FIX: debug mode was hardcoded ON in production; only enable for debug builds.
        if (clerkKey.isNotBlank() && !clerkKey.contains("placeholder", ignoreCase = true)) {
            try {
                Clerk.initialize(
                    this,
                    clerkKey,
                    options = ClerkConfigurationOptions(enableDebugMode = BuildConfig.DEBUG),
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
        
        // Schedule Custom Push Notifications Pull
        try {
            // Immediate startup sync
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isPremium = if (com.revenuecat.purchases.Purchases.isConfigured) {
                        kotlin.coroutines.suspendCoroutine { continuation ->
                            com.revenuecat.purchases.Purchases.sharedInstance.getCustomerInfo(
                                object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                                    override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo) {
                                        continuation.resume(customerInfo.entitlements["pro"]?.isActive == true)
                                    }
                                    override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                                        continuation.resume(false)
                                    }
                                }
                            )
                        }
                    } else false
                    com.oryno.piggy_ledger.data.NotificationRemoteManager.fetchAndShowNotifications(this@PiggyLedgerApplication, isPremium)
                } catch (e: Exception) {
                    Log.e("PiggyLedgerApp", "Immediate notification fetch error", e)
                }
            }

            val periodicNotifSync = androidx.work.PeriodicWorkRequestBuilder<com.oryno.piggy_ledger.data.NotificationWorker>(
                4, java.util.concurrent.TimeUnit.HOURS
            ).build()
            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "CustomPushNotificationsSync",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                periodicNotifSync
            )
        } catch (e: Exception) {
            Log.e("PiggyLedgerApp", "Failed to schedule NotificationWorker", e)
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

        // FIX(sync): periodic cloud sync (6h, CONNECTED, UPDATE) so data converges
        // even without edits; one-time triggers cover immediacy. Best-effort.
        try {
            com.oryno.piggy_ledger.service.SyncWorker.schedulePeriodic(this)
        } catch (e: Exception) {
            Log.e("PiggyLedgerApp", "Failed to schedule SyncWorker", e)
        }

        // FIX(sync): if previous sync attempts were capped, force a full resync now
        try {
            if (com.oryno.piggy_ledger.service.SyncWorker.needsFullResync(this)) {
                Log.i("PiggyLedgerApp", "Full resync flagged, triggering immediate sync")
                com.oryno.piggy_ledger.service.SyncWorker.scheduleOneTime(this)
                com.oryno.piggy_ledger.service.SyncWorker.clearFullResyncFlag(this)
            }
        } catch (e: Exception) {
            Log.e("PiggyLedgerApp", "Failed to check or trigger full resync", e)
        }

        // FIX(sync): register network callback to trigger sync when connectivity
        // is restored. This ensures offline changes sync as soon as WiFi returns,
        // even if WorkManager periodic/one-time work gave up or is delayed.
        try {
            registerNetworkCallback()
        } catch (e: Exception) {
            Log.e("PiggyLedgerApp", "Failed to register network callback", e)
        }
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.i("PiggyLedgerApp", "Network became available, triggering sync")
                    // Small delay to let Clerk session stabilize if needed
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            kotlinx.coroutines.delay(2000)
                            com.oryno.piggy_ledger.service.SyncWorker.scheduleOneTime(this@PiggyLedgerApplication)
                        } catch (e: Exception) {
                            Log.e("PiggyLedgerApp", "Network callback sync trigger failed", e)
                        }
                    }
                }

                override fun onLost(network: Network) {
                    Log.i("PiggyLedgerApp", "Network lost")
                }
            }

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        } catch (e: Exception) {
            Log.e("PiggyLedgerApp", "Failed to register network callback", e)
        }
    }
}