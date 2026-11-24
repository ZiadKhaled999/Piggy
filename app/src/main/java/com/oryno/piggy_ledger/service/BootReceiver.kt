package com.oryno.piggy_ledger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Boot completed or app updated. Rescheduling notifications...")
            NotificationScheduler.scheduleAll(context)
            SubscriptionNotificationScheduler.rescheduleAll(context)
            SubscriptionNotificationScheduler.schedulePeriodicWorker(context)
            
            // FIX(sync): Also schedule cloud sync on boot so offline changes
            // get synced after device restart, even if periodic work was
            // cleared or delayed. Uses APPEND_OR_REPLACE so it won't kill
            // any in-flight sync from network callback.
            try {
                SyncWorker.scheduleOneTime(context)
                SyncWorker.schedulePeriodic(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to schedule SyncWorker", e)
            }
            try {
                val awm = android.appwidget.AppWidgetManager.getInstance(context)
                val ids = awm?.getAppWidgetIds(
                    android.content.ComponentName(
                        context,
                        com.oryno.piggy_ledger.widget.HeatmapWidgetProvider::class.java
                    )
                )
                if (ids != null && ids.isNotEmpty()) {
                    com.oryno.piggy_ledger.widget.HeatmapWidgetWorker.schedulePeriodic(context)
                    com.oryno.piggy_ledger.widget.HeatmapWidgetProvider.triggerUpdate(context)
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to refresh HeatmapWidget", e)
            }
        }
    }
}
