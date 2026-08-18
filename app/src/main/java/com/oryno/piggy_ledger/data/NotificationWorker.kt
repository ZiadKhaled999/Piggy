package com.oryno.piggy_ledger.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.revenuecat.purchases.Purchases
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("NotificationWorker", "Running scheduled notification check...")
        return try {
            val isPremium = checkPremiumStatus()
            val success = NotificationRemoteManager.fetchAndShowNotifications(context, isPremium)
            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error in worker", e)
            Result.retry()
        }
    }

    private suspend fun checkPremiumStatus(): Boolean = suspendCoroutine { continuation ->
        try {
            if (Purchases.isConfigured) {
                Purchases.sharedInstance.getCustomerInfo(
                    object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                        override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo) {
                            val isPremium = customerInfo.entitlements["pro"]?.isActive == true
                            continuation.resume(isPremium)
                        }

                        override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                            Log.e("NotificationWorker", "Failed to check RevenueCat: \${error.message}")
                            continuation.resume(false)
                        }
                    }
                )
            } else {
                continuation.resume(false)
            }
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Exception checking RevenueCat", e)
            continuation.resume(false)
        }
    }
}
