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

class StreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val displayInfo = StreakManager.getWidgetDisplayInfo(context)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_streak)

            // Set widget background artwork image based on status/time tier
            views.setImageViewResource(R.id.iv_widget_bg, displayInfo.backgroundResource)

            // Update top bar: Streak status PNG image + Active Streak Count number
            views.setImageViewResource(R.id.iv_streak_status_icon, displayInfo.badgeResource)
            views.setTextViewText(R.id.tv_streak_count, displayInfo.streakCount.toString())

            // Update speech bubble text in the center
            views.setTextViewText(R.id.tv_streak_message, displayInfo.speechMessage)

            // Update mascot image at the bottom
            views.setImageViewResource(R.id.iv_streak_mascot, displayInfo.mascotResource)

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
