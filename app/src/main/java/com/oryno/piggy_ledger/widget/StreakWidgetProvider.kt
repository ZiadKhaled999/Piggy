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
        // Create localized context
        val locales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        val config = android.content.res.Configuration(context.resources.configuration)
        if (!locales.isEmpty) {
            config.setLocales(android.os.LocaleList.forLanguageTags(locales.toLanguageTags()))
        }
        val localizedContext = context.createConfigurationContext(config)

        val streakCount = StreakManager.getStreak(context)
        val piggyState = StreakManager.getPiggyState(context)
        val piggyRes = StreakManager.getPiggyResource(piggyState)

        // Determine adaptive message
        val message = getStreakStatement(localizedContext, piggyState)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_streak)

            // Update text
            views.setTextViewText(R.id.tv_streak_count, localizedContext.getString(R.string.days_count, streakCount))
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
            val isArabic = locales.toLanguageTags().contains("ar") || 
                (locales.isEmpty && java.util.Locale.getDefault().language == "ar")
            
            val layoutDir = if (isArabic) {
                android.view.View.LAYOUT_DIRECTION_RTL
            } else {
                android.view.View.LAYOUT_DIRECTION_LTR
            }
            val gravity = if (isArabic) {
                android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
            } else {
                android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            }
            
            views.setInt(R.id.ll_streak_info, "setGravity", gravity)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                views.setInt(R.id.ll_streak_info, "setLayoutDirection", layoutDir)
                views.setInt(R.id.ll_week_streak, "setLayoutDirection", layoutDir)
            }

            val dayLetters = if (isArabic) {
                arrayOf("ح", "ن", "ث", "ر", "خ", "ج", "س")
            } else {
                arrayOf("S", "M", "T", "W", "T", "F", "S")
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayDateStr = sdf.format(Date())
            val todayCalendar = Calendar.getInstance()
            
            for (i in 0 until 7) {
                val dateStr = sdf.format(calendar.time)
                val isActive = activeDates.contains(dateStr)
                val isPast = calendar.before(todayCalendar) && dateStr != todayDateStr
                
                val (bgRes, dayText) = when {
                    isActive -> R.drawable.ic_streak_check to ""
                    isPast -> R.drawable.ic_streak_x to ""
                    else -> R.drawable.bg_streak_day_future to dayLetters[i]
                }

                views.setInt(dayIds[i], "setBackgroundResource", bgRes)
                views.setTextColor(dayIds[i], 0xFFFFFFFF.toInt()) // Always white for better contrast
                views.setTextViewText(dayIds[i], dayText)
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Intent to launch MainActivity when clicking the widget
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun getStreakStatement(context: Context, state: StreakManager.PiggyState): String {
        val resId = when (state) {
            StreakManager.PiggyState.SUCCESS -> R.array.streak_statements_success
            StreakManager.PiggyState.HAPPY -> R.array.streak_statements_happy
            StreakManager.PiggyState.PANIC -> R.array.streak_statements_panic
            StreakManager.PiggyState.WORRIED -> R.array.streak_statements_worried
            StreakManager.PiggyState.LOST -> R.array.streak_statements_lost
        }
        val statements = context.resources.getStringArray(resId)
        val seed = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return statements[seed % statements.size]
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
