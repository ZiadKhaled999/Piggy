package com.oryno.piggy_ledger.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.StreakManager
import com.oryno.piggy_ledger.ui.NotificationHelper

class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Ensure synchronous language preference is populated from DataStore if needed
        if (com.oryno.piggy_ledger.data.UserPreferences.getSavedAppLanguageSync(context).isNullOrBlank()) {
            try {
                val dsLang = com.oryno.piggy_ledger.data.UserPreferences(context).appLanguage.first()
                if (!dsLang.isNullOrBlank()) {
                    com.oryno.piggy_ledger.data.UserPreferences.saveAppLanguageSync(context, dsLang)
                }
            } catch (_: Exception) { }
        }

        val notificationHelper = NotificationHelper(context)
        val type = inputData.getString(KEY_NOTIF_TYPE) ?: return Result.success()

        when (type) {
            TYPE_STREAK -> {
                val missed = StreakManager.getConsecutiveMissedDays(context)
                if (missed >= 2) {
                    notificationHelper.showStreakWarningNotification(missed)
                }
            }
            TYPE_GOAL -> {
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
                        val firstGoal = goals.first()
                        notificationHelper.showGoalReminderNotification(firstGoal.name, "${currencySymbol}0.00")
                    }
                } else {
                    val goalMsg = NotificationHelper.getLocalizedContext(context).getString(R.string.my_goals)
                    notificationHelper.showGoalReminderNotification(goalMsg, "")
                }
            }
            TYPE_DEADLINE -> {
                val db = PiggyLedgerDatabase.getInstance(context)
                val now = System.currentTimeMillis()
                val overdue = db.piggyLedgerDao().getAllLoansSync().filter { !it.is_deleted && !it.isPaidOff && it.deadline != null && it.deadline < now }
                overdue.forEach { loan ->
                    try { notificationHelper.showDeadlineNotification(loan.contactName, loan.amount) } catch (e: Exception) { }
                }
            }
            TYPE_MOTIVATION -> {
                val randomQuote = NotificationHelper.getRandomMotivationQuote(context)
                notificationHelper.showMotivationNotification(randomQuote)
            }
        }

        return Result.success()
    }

    companion object {
        const val KEY_NOTIF_TYPE = "notif_type"
        const val TYPE_STREAK = "streak_warning"
        const val TYPE_GOAL = "goal_reminder"
        const val TYPE_MOTIVATION = "motivation"
        const val TYPE_DEADLINE = "deadline_reminder"
    }
}
