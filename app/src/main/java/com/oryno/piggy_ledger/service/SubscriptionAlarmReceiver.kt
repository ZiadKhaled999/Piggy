package com.oryno.piggy_ledger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.UserPreferences
import com.oryno.piggy_ledger.ui.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SubscriptionAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val subId = intent.getStringExtra(EXTRA_SUB_ID) ?: return
        val isTrial = intent.getBooleanExtra(EXTRA_IS_TRIAL, false)
        Log.d("SubAlarmReceiver", "Received subscription reminder alarm for id: $subId (trial=$isTrial)")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (UserPreferences.getSavedAppLanguageSync(context).isNullOrBlank()) {
                    try {
                        val dsLang = UserPreferences(context).appLanguage.first()
                        if (!dsLang.isNullOrBlank()) {
                            UserPreferences.saveAppLanguageSync(context, dsLang)
                        }
                    } catch (_: Exception) { }
                }

                val db = PiggyLedgerDatabase.getInstance(context)
                val sub = db.piggyLedgerDao().getSubscriptionByIdSync(subId)
                val notificationHelper = NotificationHelper(context)

                if (sub != null &&
                    !sub.is_deleted &&
                    sub.status.equals("ACTIVE", ignoreCase = true) &&
                    sub.isActive &&
                    sub.reminderEnabled
                ) {
                    val now = System.currentTimeMillis()
                    if (sub.autoDebit && sub.nextRenewalDate < now) {
                        return@launch
                    }

                    val targetDate = if (isTrial && sub.hasFreeTrial && sub.trialEndsAt > 0L) {
                        sub.trialEndsAt
                    } else {
                        sub.nextRenewalDate
                    }
                    val daysUntilDue = DeadlineAndReminderUtils.calendarDaysBetween(now, targetDate)

                    notificationHelper.showSubscriptionDueNotification(
                        subscriptionId = sub.id,
                        serviceName = sub.name,
                        amountStr = DeadlineAndReminderUtils.formatAmountWithCurrency(sub.amount, sub.currency),
                        daysUntilDue = daysUntilDue,
                        isTrial = isTrial
                    )

                    val tag = if (isTrial) {
                        "trial_$daysUntilDue"
                    } else if (daysUntilDue <= 0) {
                        "due"
                    } else {
                        "lead_$daysUntilDue"
                    }
                    DeadlineAndReminderUtils.markSubscriptionNotified(context, sub.id, tag, now)

                    // Schedule next alarm / multi-daily repeat slot
                    SubscriptionNotificationScheduler.schedule(context, sub)
                } else {
                    SubscriptionNotificationScheduler.cancel(context, subId)
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
        const val EXTRA_IS_TRIAL = "extra_is_trial"
    }
}
