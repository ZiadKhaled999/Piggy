package com.oryno.piggy_ledger

import android.app.Application
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

class PiggyLedgerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = PostHogAndroidConfig(
            apiKey = "phc_nkNWGafPjbnfdorkfM2L4ZsNJFo8jbz4Ybjbu9C9tPMp",
            host = "https://us.i.posthog.com"
        )
        PostHogAndroid.setup(this, config)
    }
}
