package com.oryno.piggy_ledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.oryno.piggy_ledger.R

class GoalsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            showGoalsList(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val appWidgetManager = AppWidgetManager.getInstance(context)

        if (action == WidgetUtils.ACTION_GOAL_CLICK || action.endsWith("ACTION_GOAL_CLICK")) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val goalName = intent.getStringExtra("extra_goal_name") ?: ""
                val targetAmount = intent.getDoubleExtra("extra_target_amount", 0.0)
                val savedAmount = intent.getDoubleExtra("extra_saved_amount", 0.0)
                val progress = intent.getIntExtra("extra_progress", 0)
                val currencySymbol = intent.getStringExtra("extra_currency_symbol") ?: ""

                showGoalDetail(context, appWidgetManager, appWidgetId, goalName, targetAmount, savedAmount, progress, currencySymbol)
            }
        } else if (action == WidgetUtils.ACTION_GOAL_BACK || action.endsWith("ACTION_GOAL_BACK")) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                showGoalsList(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun showGoalsList(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val localizedContext = WidgetUtils.getLocalizedContext(context)
        val views = RemoteViews(context.packageName, R.layout.widget_goals)

        // View Visibility: Show list, hide detail
        views.setViewVisibility(R.id.layout_goals_list, View.VISIBLE)
        views.setViewVisibility(R.id.layout_goal_detail, View.GONE)

        // Localized Strings
        views.setTextViewText(R.id.tv_header_my_goals, localizedContext.getString(R.string.widget_my_goals))
        views.setTextViewText(R.id.tv_empty_goals, localizedContext.getString(R.string.widget_no_goals))

        // Remote adapter for goals list
        val serviceIntent = Intent(context, GoalsWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.lv_goals, serviceIntent)
        views.setEmptyView(R.id.lv_goals, R.id.tv_empty_goals)

        // Set pending intent template for list item clicks
        val clickIntent = Intent(context, GoalsWidgetProvider::class.java).apply {
            action = WidgetUtils.ACTION_GOAL_CLICK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val clickPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.lv_goals, clickPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun showGoalDetail(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        goalName: String,
        targetAmount: Double,
        savedAmount: Double,
        progress: Int,
        currencySymbol: String
    ) {
        val localizedContext = WidgetUtils.getLocalizedContext(context)
        val views = RemoteViews(context.packageName, R.layout.widget_goals)

        // View Visibility: Show detail, hide list
        views.setViewVisibility(R.id.layout_goals_list, View.GONE)
        views.setViewVisibility(R.id.layout_goal_detail, View.VISIBLE)

        // Localized Strings
        views.setTextViewText(R.id.tv_header_goal_progress, localizedContext.getString(R.string.widget_goal_progress))
        views.setTextViewText(R.id.tv_header_total_balance, localizedContext.getString(R.string.widget_total_balance))

        // Data bindings
        views.setTextViewText(R.id.tv_detail_goal_name, goalName)
        views.setTextViewText(R.id.tv_detail_saved_amount, WidgetUtils.formatWidgetAmount(savedAmount, currencySymbol))

        val isOpen = targetAmount <= 0.0
        if (isOpen) {
            views.setViewVisibility(R.id.tv_header_goal_progress, View.GONE)
            views.setViewVisibility(R.id.tv_detail_goal_target, View.GONE)
            views.setViewVisibility(R.id.pb_detail_progress, View.GONE)
        } else {
            views.setViewVisibility(R.id.tv_header_goal_progress, View.VISIBLE)
            views.setViewVisibility(R.id.tv_detail_goal_target, View.VISIBLE)
            views.setViewVisibility(R.id.pb_detail_progress, View.VISIBLE)
            views.setTextViewText(R.id.tv_detail_goal_target, " / " + WidgetUtils.formatWidgetAmount(targetAmount, currencySymbol))
            views.setProgressBar(R.id.pb_detail_progress, 100, progress.coerceIn(0, 100), false)
        }

        // Setup back button
        val backIntent = Intent(context, GoalsWidgetProvider::class.java).apply {
            action = WidgetUtils.ACTION_GOAL_BACK
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val backPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            backIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_back_to_list, backPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, GoalsWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, GoalsWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.lv_goals)
            }
        }
    }
}
