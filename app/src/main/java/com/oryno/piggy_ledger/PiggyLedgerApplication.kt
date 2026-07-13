package com.oryno.piggy_ledger

import android.app.Application
import android.util.Log
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions

class PiggyLedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = PostHogAndroidConfig(
            apiKey = "phc_nkNWGafPjbnfdorkfM2L4ZsNJFo8jbz4Ybjbu9C9tPMp",
            host = "https://us.i.posthog.com"
        )
        PostHogAndroid.setup(this, config)

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
    }
}
