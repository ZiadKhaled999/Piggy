package com.oryno.piggy_ledger.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.SubscriptionEntity
import com.oryno.piggy_ledger.ui.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

object SubscriptionNotificationScheduler {
    private const val TAG = "SubNotifScheduler"
    private val KNOWN_LEAD_DAYS = listOf(0, 1, 2, 3, 7, 14, 30)
    private val REPEAT_ALARM_HOURS = listOf(9, 14, 20)

    private fun renewalLeadRequestCode(subscriptionId: String, leadDays: Int): Int {
        return (subscriptionId.hashCode() * 31) + leadDays
    }

    private fun trialLeadRequestCode(subscriptionId: String, leadDays: Int): Int {
        return (subscriptionId.hashCode() * 31) + 1000 + leadDays
    }

    private fun repeatSlotRequestCode(subscriptionId: String, hour: Int, isTrial: Boolean): Int {
        val base = if (isTrial) 4000 else 3000
        return (subscriptionId.hashCode() * 31) + base + hour
    }

    fun schedule(context: Context, subscription: SubscriptionEntity) {
        if (subscription.is_deleted ||
            !subscription.reminderEnabled ||
            !subscription.isActive ||
            !subscription.status.equals("ACTIVE", ignoreCase = true)
        ) {
            cancel(context, subscription.id)
            return
        }

        // Clear previously scheduled alarms before scheduling updated ones
        cancelAlarmsOnly(context, subscription.id)

        val now = System.currentTimeMillis()
        // Auto-debit subscriptions that are already past due are handled by auto-debit
        if (subscription.autoDebit && subscription.nextRenewalDate < now) {
            return
        }

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            // 1. Schedule exact lead-day alarms (9:00 AM on targetDay = renewalDate - leadDays)
            val renewalLeadDays = DeadlineAndReminderUtils.getSubscriptionRenewalLeadDays(context, subscription)
            renewalLeadDays.forEach { leadDays ->
                val targetCal = Calendar.getInstance().apply {
                    timeInMillis = subscription.nextRenewalDate
                    add(Calendar.DAY_OF_YEAR, -leadDays)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (targetCal.timeInMillis > now) {
                    scheduleExactAlarm(
                        context = context,
                        alarmManager = alarmManager,
                        subscription = subscription,
                        requestCode = renewalLeadRequestCode(subscription.id, leadDays),
                        triggerTimeMs = targetCal.timeInMillis,
                        isTrial = false
                    )
                }
            }

            // 2. If due today / overdue OR daily alerts enabled inside reminder window, schedule recurring alarms
            val daysUntilRenewal = DeadlineAndReminderUtils.calendarDaysBetween(now, subscription.nextRenewalDate)
            val inDailyRepeatWindow = subscription.reminderDailyRepeat &&
                renewalLeadDays.any { lead -> daysUntilRenewal <= lead }

            if (daysUntilRenewal <= 0) {
                // Due today or overdue: repeat across morning/afternoon/evening slots until paid/paused/deleted
                scheduleNextRepeatSlot(
                    context = context,
                    alarmManager = alarmManager,
                    subscription = subscription,
                    hours = REPEAT_ALARM_HOURS,
                    isTrial = false,
                    now = now
                )
            } else if (inDailyRepeatWindow) {
                // Within reminder window before due date with daily alerts enabled: notify each morning at 9:00 AM
                scheduleNextRepeatSlot(
                    context = context,
                    alarmManager = alarmManager,
                    subscription = subscription,
                    hours = listOf(9),
                    isTrial = false,
                    now = now
                )
            }

            // 3. Schedule free trial reminders if trial is active and trial notifications are enabled
            if (subscription.hasFreeTrial &&
                subscription.trialEndsAt > 0L &&
                DeadlineAndReminderUtils.isSubscriptionTrialReminderEnabled(context, subscription.id)
            ) {
                val trialLeads = DeadlineAndReminderUtils.getSubscriptionTrialLeadDays(context, subscription.id)
                trialLeads.forEach { leadDays ->
                    val targetCal = Calendar.getInstance().apply {
                        timeInMillis = subscription.trialEndsAt
                        add(Calendar.DAY_OF_YEAR, -leadDays)
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (targetCal.timeInMillis > now) {
                        scheduleExactAlarm(
                            context = context,
                            alarmManager = alarmManager,
                            subscription = subscription,
                            requestCode = trialLeadRequestCode(subscription.id, leadDays),
                            triggerTimeMs = targetCal.timeInMillis,
                            isTrial = true
                        )
                    }
                }
                val daysUntilTrialEnd = DeadlineAndReminderUtils.calendarDaysBetween(now, subscription.trialEndsAt)
                if (daysUntilTrialEnd <= 0) {
                    scheduleNextRepeatSlot(
                        context = context,
                        alarmManager = alarmManager,
                        subscription = subscription,
                        hours = REPEAT_ALARM_HOURS,
                        isTrial = true,
                        now = now
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule subscription reminder for ${subscription.name}", e)
        }
    }

    private fun scheduleNextRepeatSlot(
        context: Context,
        alarmManager: AlarmManager,
        subscription: SubscriptionEntity,
        hours: List<Int>,
        isTrial: Boolean,
        now: Long
    ) {
        val candidates = mutableListOf<Pair<Int, Long>>()
        hours.forEach { hour ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            candidates.add(hour to cal.timeInMillis)
        }
        val nextSlot = candidates.minByOrNull { it.second } ?: return
        scheduleExactAlarm(
            context = context,
            alarmManager = alarmManager,
            subscription = subscription,
            requestCode = repeatSlotRequestCode(subscription.id, nextSlot.first, isTrial),
            triggerTimeMs = nextSlot.second,
            isTrial = isTrial
        )
    }

    private fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        subscription: SubscriptionEntity,
        requestCode: Int,
        triggerTimeMs: Long,
        isTrial: Boolean
    ) {
        val intent = Intent(context, SubscriptionAlarmReceiver::class.java).apply {
            putExtra(SubscriptionAlarmReceiver.EXTRA_SUB_ID, subscription.id)
            putExtra(SubscriptionAlarmReceiver.EXTRA_SUB_NAME, subscription.name)
            putExtra(
                SubscriptionAlarmReceiver.EXTRA_SUB_AMOUNT,
                DeadlineAndReminderUtils.formatAmountWithCurrency(subscription.amount, subscription.currency)
            )
            putExtra(SubscriptionAlarmReceiver.EXTRA_IS_TRIAL, isTrial)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }
        Log.d(
            TAG,
            "Scheduled sub reminder (${if (isTrial) "trial" else "renewal"}) for ${subscription.name} at ${
                Calendar.getInstance().apply { timeInMillis = triggerTimeMs }.time
            }"
        )
    }

    private fun cancelAlarmsOnly(context: Context, subscriptionId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val codes = mutableSetOf<Int>()
            codes.add(subscriptionId.hashCode()) // Legacy request code
            KNOWN_LEAD_DAYS.forEach { lead ->
                codes.add(renewalLeadRequestCode(subscriptionId, lead))
                codes.add(trialLeadRequestCode(subscriptionId, lead))
            }
            REPEAT_ALARM_HOURS.forEach { hour ->
                codes.add(repeatSlotRequestCode(subscriptionId, hour, isTrial = false))
                codes.add(repeatSlotRequestCode(subscriptionId, hour, isTrial = true))
            }

            codes.forEach { reqCode ->
                val intent = Intent(context, SubscriptionAlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel alarms for $subscriptionId", e)
        }
    }

    fun cancel(context: Context, subscriptionId: String) {
        cancelAlarmsOnly(context, subscriptionId)
        try {
            NotificationHelper(context).cancelSubscriptionNotification(subscriptionId)
        } catch (_: Exception) { }
        Log.d(TAG, "Cancelled subscription reminder for $subscriptionId")
    }

    suspend fun checkAndNotifyIfDueNow(context: Context, subscription: SubscriptionEntity) {
        try {
            if (subscription.is_deleted ||
                !subscription.reminderEnabled ||
                !subscription.isActive ||
                !subscription.status.equals("ACTIVE", ignoreCase = true)
            ) {
                return
            }

            val now = System.currentTimeMillis()
            if (subscription.autoDebit && subscription.nextRenewalDate < now) return

            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val renewalLeadDays = DeadlineAndReminderUtils.getSubscriptionRenewalLeadDays(context, subscription)
            val daysUntilRenewal = DeadlineAndReminderUtils.calendarDaysBetween(now, subscription.nextRenewalDate)

            val isDueOrOverdue = daysUntilRenewal <= 0
            val isLeadTodayAndAfter9 = daysUntilRenewal > 0 &&
                (daysUntilRenewal in renewalLeadDays ||
                    (subscription.reminderDailyRepeat && renewalLeadDays.any { daysUntilRenewal <= it })) &&
                currentHour >= 9

            val notificationHelper = NotificationHelper(context)
            if (isDueOrOverdue || isLeadTodayAndAfter9) {
                val tag = if (isDueOrOverdue) "due" else "lead_$daysUntilRenewal"
                val cooldown = if (isDueOrOverdue) TimeUnit.HOURS.toMillis(4) else TimeUnit.HOURS.toMillis(18)
                if (DeadlineAndReminderUtils.shouldSendSubscriptionNotification(context, subscription.id, tag, now, cooldown)) {
                    notificationHelper.showSubscriptionDueNotification(
                        subscriptionId = subscription.id,
                        serviceName = subscription.name,
                        amountStr = DeadlineAndReminderUtils.formatAmountWithCurrency(subscription.amount, subscription.currency),
                        daysUntilDue = daysUntilRenewal,
                        isTrial = false
                    )
                    DeadlineAndReminderUtils.markSubscriptionNotified(context, subscription.id, tag, now)
                }
                return
            }

            if (subscription.hasFreeTrial &&
                subscription.trialEndsAt > 0L &&
                DeadlineAndReminderUtils.isSubscriptionTrialReminderEnabled(context, subscription.id)
            ) {
                val trialLeads = DeadlineAndReminderUtils.getSubscriptionTrialLeadDays(context, subscription.id)
                val daysUntilTrialEnd = DeadlineAndReminderUtils.calendarDaysBetween(now, subscription.trialEndsAt)
                if (daysUntilTrialEnd <= 0 || (daysUntilTrialEnd in trialLeads && currentHour >= 9)) {
                    val tag = "trial_$daysUntilTrialEnd"
                    val cooldown = if (daysUntilTrialEnd <= 0) TimeUnit.HOURS.toMillis(4) else TimeUnit.HOURS.toMillis(18)
                    if (DeadlineAndReminderUtils.shouldSendSubscriptionNotification(context, subscription.id, tag, now, cooldown)) {
                        notificationHelper.showSubscriptionDueNotification(
                            subscriptionId = subscription.id,
                            serviceName = subscription.name,
                            amountStr = DeadlineAndReminderUtils.formatAmountWithCurrency(subscription.amount, subscription.currency),
                            daysUntilDue = daysUntilTrialEnd,
                            isTrial = true
                        )
                        DeadlineAndReminderUtils.markSubscriptionNotified(context, subscription.id, tag, now)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAndNotifyIfDueNow for ${subscription.name}", e)
        }
    }

    fun schedulePeriodicWorker(context: Context) {
        try {
            val periodicRequest = PeriodicWorkRequestBuilder<SubscriptionReminderWorker>(6, TimeUnit.HOURS)
                .addTag(SubscriptionReminderWorker.UNIQUE_WORK_NAME)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                SubscriptionReminderWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule SubscriptionReminderWorker", e)
        }
    }

    fun rescheduleAll(context: Context) {
        schedulePeriodicWorker(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PiggyLedgerDatabase.getInstance(context)
                val subs = db.piggyLedgerDao().getAllSubscriptionsSyncFull()
                subs.forEach { sub ->
                    if (!sub.is_deleted &&
                        sub.status.equals("ACTIVE", ignoreCase = true) &&
                        sub.isActive &&
                        sub.reminderEnabled
                    ) {
                        schedule(context, sub)
                    } else {
                        cancel(context, sub.id)
                        if (sub.is_deleted) {
                            DeadlineAndReminderUtils.clearSubscriptionPrefs(context, sub.id)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling all subscription reminders", e)
            }
        }
    }
}
