import re

with open('app/src/main/java/com/oryno/piggy_ledger/PiggyLedgerApplication.kt', 'r') as f:
    content = f.read()

addition = """
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
"""

content = content.replace('// Schedule Remote Config & Mascot Asset Sync', addition + '\n        // Schedule Remote Config & Mascot Asset Sync')
content = content.replace('import kotlinx.coroutines.launch', 'import kotlinx.coroutines.launch\nimport kotlin.coroutines.resume')

with open('app/src/main/java/com/oryno/piggy_ledger/PiggyLedgerApplication.kt', 'w') as f:
    f.write(content)
