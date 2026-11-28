package com.oryno.piggy_ledger.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.UserPreferences
import com.oryno.piggy_ledger.ui.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SubscriptionReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (UserPreferences.getSavedAppLanguageSync(context).isNullOrBlank()) {
                try {
                    val dsLang = UserPreferences(context).appLanguage.first()
                    if (!dsLang.isNullOrBlank()) {
                        UserPreferences.saveAppLanguageSync(context, dsLang)
                    }
                } catch (_: Exception) { }
            }

            val db = PiggyLedgerDatabase.getInstance(context)
            val subs = db.piggyLedgerDao().getAllSubscriptionsSyncFull()
            val now = System.currentTimeMillis()
            val notificationHelper = NotificationHelper(context)

            subs.forEach { sub ->
                if (sub.is_deleted || !sub.isActive || !sub.status.equals("ACTIVE", ignoreCase = true) || !sub.reminderEnabled) {
                    SubscriptionNotificationScheduler.cancel(context, sub.id)
                    if (sub.is_deleted) {
                        DeadlineAndReminderUtils.clearSubscriptionPrefs(context, sub.id)
                    }
                    return@forEach
                }

                // Skip auto-debit subscriptions that are already past due (handled by auto-debit)
                if (sub.autoDebit && sub.nextRenewalDate < now) {
                    return@forEach
                }

                val renewalLeadDays = DeadlineAndReminderUtils.getSubscriptionRenewalLeadDays(context, sub)
                val daysUntilRenewal = DeadlineAndReminderUtils.calendarDaysBetween(now, sub.nextRenewalDate)

                val isDueOrOverdue = daysUntilRenewal <= 0
                val isLeadDayMatch = daysUntilRenewal > 0 && daysUntilRenewal in renewalLeadDays
                val isDailyRepeatInWindow = sub.reminderDailyRepeat &&
                    renewalLeadDays.any { lead -> daysUntilRenewal <= lead }

                if (isDueOrOverdue || isLeadDayMatch || isDailyRepeatInWindow) {
                    val tag = if (isDueOrOverdue) "due" else "lead_$daysUntilRenewal"
                    val cooldownMs = if (isDueOrOverdue || isDailyRepeatInWindow) {
                        TimeUnit.HOURS.toMillis(4)
                    } else {
                        TimeUnit.HOURS.toMillis(18)
                    }
                    if (DeadlineAndReminderUtils.shouldSendSubscriptionNotification(context, sub.id, tag, now, cooldownMs)) {
                        notificationHelper.showSubscriptionDueNotification(
                            subscriptionId = sub.id,
                            serviceName = sub.name,
                            amountStr = DeadlineAndReminderUtils.formatAmountWithCurrency(sub.amount, sub.currency),
                            daysUntilDue = daysUntilRenewal,
                            isTrial = false
                        )
                        DeadlineAndReminderUtils.markSubscriptionNotified(context, sub.id, tag, now)
                    }
                } else if (sub.hasFreeTrial && sub.trialEndsAt > 0L &&
                    DeadlineAndReminderUtils.isSubscriptionTrialReminderEnabled(context, sub.id)
                ) {
                    val trialLeads = DeadlineAndReminderUtils.getSubscriptionTrialLeadDays(context, sub.id)
                    val daysUntilTrialEnd = DeadlineAndReminderUtils.calendarDaysBetween(now, sub.trialEndsAt)
                    if (daysUntilTrialEnd <= 0 || daysUntilTrialEnd in trialLeads) {
                        val tag = "trial_$daysUntilTrialEnd"
                        val cooldownMs = if (daysUntilTrialEnd <= 0) TimeUnit.HOURS.toMillis(4) else TimeUnit.HOURS.toMillis(18)
                        if (DeadlineAndReminderUtils.shouldSendSubscriptionNotification(context, sub.id, tag, now, cooldownMs)) {
                            notificationHelper.showSubscriptionDueNotification(
                                subscriptionId = sub.id,
                                serviceName = sub.name,
                                amountStr = DeadlineAndReminderUtils.formatAmountWithCurrency(sub.amount, sub.currency),
                                daysUntilDue = daysUntilTrialEnd,
                                isTrial = true
                            )
                            DeadlineAndReminderUtils.markSubscriptionNotified(context, sub.id, tag, now)
                        }
                    }
                }

                // Keep alarms up to date
                SubscriptionNotificationScheduler.schedule(context, sub)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error running SubscriptionReminderWorker", e)
            Result.success()
        }
    }

    companion object {
        private const val TAG = "SubReminderWorker"
        const val UNIQUE_WORK_NAME = "SubscriptionRemindersPeriodic6h"
    }
}
