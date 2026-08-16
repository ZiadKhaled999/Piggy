package com.oryno.piggy_ledger.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleAll(context: Context) {
        // 1. Schedule via WorkManager (Background periodic backup)
        scheduleWorkManager(context, NotificationWorker.TYPE_STREAK, 18)   // 6:00 PM for streak warning
        scheduleWorkManager(context, NotificationWorker.TYPE_GOAL, 19)     // 7:00 PM for goal reminder
        scheduleWorkManager(context, NotificationWorker.TYPE_MOTIVATION, 20) // 8:00 PM for motivation

        // 2. Schedule via AlarmManager (Guaranteed execution even when app is closed/killed)
        scheduleAlarm(context, NotificationWorker.TYPE_STREAK, 18)
        scheduleAlarm(context, NotificationWorker.TYPE_GOAL, 19)
        scheduleAlarm(context, NotificationWorker.TYPE_MOTIVATION, 20)
    }

    private fun scheduleWorkManager(context: Context, type: String, targetHour: Int) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

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
            ExistingPeriodicWorkPolicy.KEEP, // Use KEEP so app relaunch doesn't reset delay
            dailyWorkRequest
        )
    }

    fun scheduleAlarm(context: Context, type: String, targetHour: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                putExtra(NotificationAlarmReceiver.KEY_NOTIF_TYPE, type)
                putExtra(NotificationAlarmReceiver.KEY_TARGET_HOUR, targetHour)
            }

            val requestCode = when (type) {
                NotificationWorker.TYPE_STREAK -> 2001
                NotificationWorker.TYPE_GOAL -> 2002
                NotificationWorker.TYPE_MOTIVATION -> 2003
                else -> 2000
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("NotificationScheduler", "Scheduled Alarm for $type at ${calendar.time}")
        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Failed to schedule Alarm for $type", e)
        }
    }

    fun scheduleNextAlarm(context: Context, type: String, targetHour: Int) {
        scheduleAlarm(context, type, targetHour)
    }
}
