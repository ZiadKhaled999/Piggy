package com.oryno.piggy_ledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
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

        for (appWidgetId in appWidgetIds) {
            val regularViews = RemoteViews(context.packageName, R.layout.widget_streak).apply {
                setImageViewResource(R.id.iv_streak_status_icon, displayInfo.badgeResource)
                setTextViewText(R.id.tv_streak_count, displayInfo.streakCount.toString())
                setTextViewText(R.id.tv_streak_message, displayInfo.speechMessage)
                if (displayInfo.mascotBitmap != null) {
                    setImageViewBitmap(R.id.iv_streak_mascot, displayInfo.mascotBitmap)
                } else {
                    setImageViewResource(R.id.iv_streak_mascot, displayInfo.mascotResource)
                }
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }

            val compactViews = RemoteViews(context.packageName, R.layout.widget_streak_compact).apply {
                setImageViewResource(R.id.iv_streak_status_icon, displayInfo.badgeResource)
                setTextViewText(R.id.tv_streak_count, displayInfo.streakCount.toString())
                setTextViewText(R.id.tv_streak_message, displayInfo.speechMessage)
                if (displayInfo.mascotBitmap != null) {
                    setImageViewBitmap(R.id.iv_streak_mascot, displayInfo.mascotBitmap)
                } else {
                    setImageViewResource(R.id.iv_streak_mascot, displayInfo.mascotResource)
                }
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }

            val finalViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RemoteViews(
                    mapOf(
                        SizeF(110f, 40f) to compactViews,
                        SizeF(130f, 65f) to regularViews
                    )
                )
            } else {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                if (minHeight in 1..60) compactViews else regularViews
            }

            appWidgetManager.updateAppWidget(appWidgetId, finalViews)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
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
