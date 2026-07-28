package com.oryno.piggy_ledger.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.StreakManager
import com.oryno.piggy_ledger.ui.NotificationHelper

class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationHelper = NotificationHelper(context)
        val type = inputData.getString(KEY_NOTIF_TYPE) ?: return Result.success()

        when (type) {
            TYPE_STREAK -> {
                // If user hasn't recorded an entry today, warn them to maintain streak
                if (!StreakManager.hasActionToday(context)) {
                    notificationHelper.showStreakWarningNotification()
                }
            }
            TYPE_GOAL -> {
                val db = PiggyLedgerDatabase.getInstance(context)
                val goals = db.piggyLedgerDao().getAllGoalsSync()
                val transactions = db.piggyLedgerDao().getAllTransactions()

                if (goals.isNotEmpty()) {
                    val goalsWithProgress = goals.map { goal ->
                        val savedAmount = transactions.filter { it.goalId == goal.id }.sumOf { it.amount }
                        Pair(goal, savedAmount)
                    }

                    val closestGoalPair = goalsWithProgress.filter { it.second < it.first.targetAmount }
                        .minByOrNull { it.first.targetAmount - it.second }

                    if (closestGoalPair != null) {
                        val amountLeft = closestGoalPair.first.targetAmount - closestGoalPair.second
                        val amountStr = String.format("$%.2f", amountLeft)
                        notificationHelper.showGoalReminderNotification(closestGoalPair.first.name, amountStr)
                    } else {
                        val firstGoal = goals.first()
                        notificationHelper.showGoalReminderNotification(firstGoal.name, "$0.00")
                    }
                } else {
                    val goalMsg = context.getString(R.string.my_goals)
                    notificationHelper.showGoalReminderNotification(goalMsg, "")
                }
            }
            TYPE_MOTIVATION -> {
                val quotes = context.resources.getStringArray(R.array.streak_statements_happy)
                val randomQuote = if (quotes.isNotEmpty()) quotes.random() else "Stay on track with Piggy Ledger! 🐷"
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
    }
}
