package com.oryno.piggy_ledger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.StreakManager
import com.oryno.piggy_ledger.ui.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(KEY_NOTIF_TYPE) ?: return
        val targetHour = intent.getIntExtra(KEY_TARGET_HOUR, 20)

        Log.d("NotificationAlarm", "Alarm triggered for type: $type at hour: $targetHour")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (com.oryno.piggy_ledger.data.UserPreferences.getSavedAppLanguageSync(context).isNullOrBlank()) {
                    try {
                        val dsLang = com.oryno.piggy_ledger.data.UserPreferences(context).appLanguage.first()
                        if (!dsLang.isNullOrBlank()) {
                            com.oryno.piggy_ledger.data.UserPreferences.saveAppLanguageSync(context, dsLang)
                        }
                    } catch (_: Exception) { }
                }

                val notificationHelper = NotificationHelper(context)
                when (type) {
                    NotificationWorker.TYPE_STREAK -> {
                        val missed = StreakManager.getConsecutiveMissedDays(context)
                        if (missed >= 2) {
                            notificationHelper.showStreakWarningNotification(missed)
                        }
                    }
                    NotificationWorker.TYPE_GOAL -> {
                        val db = PiggyLedgerDatabase.getInstance(context)
                        val goals = db.piggyLedgerDao().getActiveGoalsSync()
                        val transactions = db.piggyLedgerDao().getActiveTransactionsSync()

                        if (goals.isNotEmpty()) {
                            val goalsWithProgress = goals.map { goal ->
                                val savedAmount = transactions.filter { it.goalId == goal.id }.sumOf { it.amount }
                                Pair(goal, savedAmount)
                            }
                            val closestGoalPair = goalsWithProgress.filter { it.second < it.first.targetAmount }
                                .minByOrNull { it.first.targetAmount - it.second }

                            val userPrefs = com.oryno.piggy_ledger.data.UserPreferences(context)
                            val currencyCode = userPrefs.appCurrency.first()
                            val currencySymbol = com.oryno.piggy_ledger.ui.getCurrencySymbol(currencyCode)

                            if (closestGoalPair != null) {
                                val amountLeft = closestGoalPair.first.targetAmount - closestGoalPair.second
                                val amountStr = String.format("%s%.2f", currencySymbol, amountLeft)
                                notificationHelper.showGoalReminderNotification(closestGoalPair.first.name, amountStr)
                            } else {
                                // All goals completed or no incomplete goals
                                val firstGoal = goals.first()
                                notificationHelper.showGoalReminderNotification(firstGoal.name, "${currencySymbol}0.00")
                            }
                        } else {
                            // Friendly reminder if no goals created yet
                            val goalMsg = NotificationHelper.getLocalizedContext(context).getString(R.string.my_goals)
                            notificationHelper.showGoalReminderNotification(goalMsg, "")
                        }
                    }
                    NotificationWorker.TYPE_DEADLINE -> {
                        val db = PiggyLedgerDatabase.getInstance(context)
                        val now = System.currentTimeMillis()
                        val overdue = db.piggyLedgerDao().getAllLoansSync().filter { !it.is_deleted && !it.isPaidOff && it.deadline != null && it.deadline < now }
                        overdue.forEach { loan ->
                            try { notificationHelper.showDeadlineNotification(loan.contactName, loan.amount) } catch (e: Exception) { }
                        }
                    }
                    NotificationWorker.TYPE_MOTIVATION -> {
                        val randomQuote = NotificationHelper.getRandomMotivationQuote(context)
                        notificationHelper.showMotivationNotification(randomQuote)
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationAlarm", "Error posting notification in receiver", e)
            } finally {
                // Reschedule for next day at same hour
                NotificationScheduler.scheduleNextAlarm(context, type, targetHour)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val KEY_NOTIF_TYPE = "key_notif_type"
        const val KEY_TARGET_HOUR = "key_target_hour"
    }
}
