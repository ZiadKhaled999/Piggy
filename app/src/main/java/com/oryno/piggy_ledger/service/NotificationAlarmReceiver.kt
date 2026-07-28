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
import kotlinx.coroutines.launch

class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(KEY_NOTIF_TYPE) ?: return
        val targetHour = intent.getIntExtra(KEY_TARGET_HOUR, 20)

        Log.d("NotificationAlarm", "Alarm triggered for type: $type at hour: $targetHour")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationHelper = NotificationHelper(context)
                when (type) {
                    NotificationWorker.TYPE_STREAK -> {
                        // Warn if they haven't recorded an expense/income today
                        if (!StreakManager.hasActionToday(context)) {
                            notificationHelper.showStreakWarningNotification()
                        }
                    }
                    NotificationWorker.TYPE_GOAL -> {
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
                                // All goals completed or no incomplete goals
                                val firstGoal = goals.first()
                                notificationHelper.showGoalReminderNotification(firstGoal.name, "$0.00")
                            }
                        } else {
                            // Friendly reminder if no goals created yet
                            val goalTitle = context.getString(R.string.notif_goal_title)
                            val goalMsg = context.getString(R.string.my_goals)
                            notificationHelper.showGoalReminderNotification(goalMsg, "")
                        }
                    }
                    NotificationWorker.TYPE_MOTIVATION -> {
                        val quotes = context.resources.getStringArray(R.array.streak_statements_happy)
                        val randomQuote = if (quotes.isNotEmpty()) quotes.random() else "Stay on track with Piggy Ledger! 🐷"
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
