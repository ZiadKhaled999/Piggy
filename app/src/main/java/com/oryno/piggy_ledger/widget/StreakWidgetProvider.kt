package com.oryno.piggy_ledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.oryno.piggy_ledger.MainActivity
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.StreakManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val streak = StreakManager.getStreak(context)
        val hasActionToday = StreakManager.hasActionToday(context)
        val piggyState = StreakManager.getPiggyState(context)
        val piggyRes = StreakManager.getPiggyResource(piggyState)

        // Determine message
        val message = when (piggyState) {
            StreakManager.PiggyState.SUCCESS -> "Streak Active!"
            StreakManager.PiggyState.LOST -> "Streak Broken"
            StreakManager.PiggyState.HAPPY -> "Keep it up!"
            StreakManager.PiggyState.WORRIED -> "Streak at risk!"
            StreakManager.PiggyState.PANIC -> "SAVE NOW!"
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_streak)

            // Update text
            views.setTextViewText(R.id.tv_streak_count, "$streak days")
            views.setTextViewText(R.id.tv_streak_message, message)
            
            // Update mascot
            views.setImageViewResource(R.id.iv_streak_mascot, piggyRes)

            // Update Weekday Indicators
            val activeDates = StreakManager.getActionDates(context)
            val calendar = Calendar.getInstance()
            // Go to start of current week (Sunday)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }

            val dayIds = arrayOf(
                R.id.tv_day_sun, R.id.tv_day_mon, R.id.tv_day_tue,
                R.id.tv_day_wed, R.id.tv_day_thu, R.id.tv_day_fri, R.id.tv_day_sat
            )

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayDateStr = sdf.format(Date())
            val todayCalendar = Calendar.getInstance()
            
            for (i in 0 until 7) {
                val dateStr = sdf.format(calendar.time)
                val isActive = activeDates.contains(dateStr)
                val isPast = calendar.before(todayCalendar) && dateStr != todayDateStr
                
                val (bgRes, textColor) = when {
                    isActive -> R.drawable.bg_streak_day_active to 0xFFFFFFFF.toInt()
                    isPast -> R.drawable.bg_streak_day_missed to 0xFFFFFFFF.toInt()
                    else -> R.drawable.bg_streak_day_inactive to 0xFF9CA3AF.toInt()
                }

                views.setInt(dayIds[i], "setBackgroundResource", bgRes)
                views.setTextColor(dayIds[i], textColor)
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Intent to launch MainActivity when clicking the widget
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1, // unique request code to avoid collision with summary widget
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, StreakWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, StreakWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
