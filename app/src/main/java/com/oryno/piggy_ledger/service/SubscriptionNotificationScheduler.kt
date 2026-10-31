package com.oryno.piggy_ledger.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.SubscriptionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object SubscriptionNotificationScheduler {
    private const val TAG = "SubNotifScheduler"

    fun schedule(context: Context, subscription: SubscriptionEntity) {
        if (!subscription.reminderEnabled || !subscription.isActive || !subscription.status.equals("ACTIVE", ignoreCase = true)) {
            cancel(context, subscription.id)
            return
        }

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, SubscriptionAlarmReceiver::class.java).apply {
                putExtra(SubscriptionAlarmReceiver.EXTRA_SUB_ID, subscription.id)
                putExtra(SubscriptionAlarmReceiver.EXTRA_SUB_NAME, subscription.name)
                putExtra(SubscriptionAlarmReceiver.EXTRA_SUB_AMOUNT, "${subscription.amount} ${subscription.currency}")
            }

            val requestCode = subscription.id.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Compute trigger time: renewal date minus lead days, set at 9:00 AM
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = subscription.nextRenewalDate
                add(Calendar.DAY_OF_YEAR, -subscription.reminderLeadDays)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val now = System.currentTimeMillis()
            var triggerTime = targetCal.timeInMillis

            // If computed reminder time is in the past, but the renewal is still in the future:
            // trigger tomorrow at 9:00 AM (or today if before 9:00 AM)
            if (triggerTime <= now && subscription.nextRenewalDate > now) {
                val nextMorning = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= now) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                triggerTime = nextMorning.timeInMillis
            }

            if (triggerTime > now) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Scheduled subscription reminder for ${subscription.name} at ${Calendar.getInstance().apply { timeInMillis = triggerTime }.time}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule subscription reminder for ${subscription.name}", e)
        }
    }

    fun cancel(context: Context, subscriptionId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, SubscriptionAlarmReceiver::class.java)
            val requestCode = subscriptionId.hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled subscription reminder for $subscriptionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel reminder for $subscriptionId", e)
        }
    }

    fun rescheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PiggyLedgerDatabase.getInstance(context)
                val subs = db.piggyLedgerDao().getAllSubscriptionsSync()
                subs.filter { it.status.equals("ACTIVE", ignoreCase = true) && it.isActive && it.reminderEnabled }.forEach { sub ->
                    schedule(context, sub)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling all subscription reminders", e)
            }
        }
    }
}
