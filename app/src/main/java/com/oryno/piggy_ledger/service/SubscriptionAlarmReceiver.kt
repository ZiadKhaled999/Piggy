package com.oryno.piggy_ledger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.ui.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SubscriptionAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val subId = intent.getStringExtra(EXTRA_SUB_ID) ?: return
        Log.d("SubAlarmReceiver", "Received subscription reminder alarm for id: $subId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PiggyLedgerDatabase.getInstance(context)
                val sub = db.piggyLedgerDao().getSubscriptionByIdSync(subId)
                if (sub != null && sub.status.equals("ACTIVE", ignoreCase = true) && sub.isActive && sub.reminderEnabled) {
                    val now = System.currentTimeMillis()
                    val diffDays = TimeUnit.MILLISECONDS.toDays(sub.nextRenewalDate - now).toInt()
                    val dueText = when {
                        diffDays <= 0 -> context.getString(R.string.sub_due_today)
                        diffDays == 1 -> context.getString(R.string.sub_due_tomorrow)
                        else -> context.getString(R.string.sub_due_in_days, diffDays)
                    }

                    val notificationHelper = NotificationHelper(context)
                    notificationHelper.showSubscriptionDueNotification(
                        serviceName = sub.name,
                        amountStr = "${sub.amount} ${sub.currency}",
                        dueRelativeText = dueText
                    )

                    // If daily alert repeat is enabled and renewal date is still in the future:
                    if (sub.reminderDailyRepeat && sub.nextRenewalDate > (now + TimeUnit.HOURS.toMillis(24))) {
                        SubscriptionNotificationScheduler.schedule(context, sub)
                    }
                }
            } catch (e: Exception) {
                Log.e("SubAlarmReceiver", "Error processing subscription alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SUB_ID = "extra_sub_id"
        const val EXTRA_SUB_NAME = "extra_sub_name"
        const val EXTRA_SUB_AMOUNT = "extra_sub_amount"
    }
}
