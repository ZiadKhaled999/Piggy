package com.oryno.piggy_ledger.service

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    fun scheduleAll(context: Context) {
        scheduleNotification(context, NotificationWorker.TYPE_STREAK, 18) // 6:00 PM for streak warning
        scheduleNotification(context, NotificationWorker.TYPE_GOAL, 19)   // 7:00 PM for goal reminder
        scheduleNotification(context, NotificationWorker.TYPE_MOTIVATION, 20) // 8:00 PM for motivation
    }

    private fun scheduleNotification(context: Context, type: String, targetHour: Int) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        // Set Execution around targetHour (e.g., 19 for 7 PM)
        dueDate.set(Calendar.HOUR_OF_DAY, targetHour)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val data = Data.Builder()
            .putString(NotificationWorker.KEY_NOTIF_TYPE, type)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(type)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            type,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }
}
