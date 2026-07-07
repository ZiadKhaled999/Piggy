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
import com.oryno.piggy_ledger.MainActivity
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.util.Locale

class GoalsWidgetProvider : AppWidgetProvider() {

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

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val db = PiggyLedgerDatabase.getInstance(context)
        val dao = db.piggyLedgerDao()

        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }

        for (appWidgetId in appWidgetIds) {
            val selectedGoalId = prefs.getString(getKeySelectedGoal(appWidgetId), null)
            val views = RemoteViews(context.packageName, R.layout.widget_goals)

            // Bind dynamic labels
            views.setTextViewText(R.id.tv_header_my_goals, localizedContext.getString(R.string.widget_my_goals))
            views.setTextViewText(R.id.tv_empty_goals, localizedContext.getString(R.string.widget_no_goals))
            views.setTextViewText(R.id.tv_header_goal_progress, localizedContext.getString(R.string.widget_goal_progress))
            views.setTextViewText(R.id.tv_header_total_balance, localizedContext.getString(R.string.widget_total_balance))

            if (selectedGoalId != null) {
                // Show Detail View
                views.setViewVisibility(R.id.layout_goals_list, View.GONE)
                views.setViewVisibility(R.id.layout_goal_detail, View.VISIBLE)

                // Query goal and transactions synchronously
                var goalName = ""
                var targetText = ""
                var savedText = ""
                var percentageText = "0%"
                var progressValue = 0

                runBlocking {
                    val goals = dao.getAllGoalsSync()
                    val goal = goals.find { it.id == selectedGoalId }
                    if (goal != null) {
                        goalName = goal.name
                        val transactions = dao.getAllTransactions().filter { it.goalId == selectedGoalId }
                        val savedAmount = transactions.sumOf { it.amount }
                        val isOpen = goal.targetAmount <= 0.0

                        targetText = if (isOpen) "" else " / $${formatter.format(goal.targetAmount)}"
                        savedText = "$${formatter.format(savedAmount)}"

                        if (isOpen) {
                            progressValue = 100
                        } else {
                            val pct = if (goal.targetAmount > 0) (savedAmount / goal.targetAmount * 100).toInt() else 0
                            progressValue = pct.coerceIn(0, 100)
                        }
                    } else {
                        // Goal not found (deleted) - revert to list
                        prefs.edit().remove(getKeySelectedGoal(appWidgetId)).apply()
                        views.setViewVisibility(R.id.layout_goals_list, View.VISIBLE)
                        views.setViewVisibility(R.id.layout_goal_detail, View.GONE)
                    }
                }

                views.setTextViewText(R.id.tv_detail_goal_name, goalName)
                views.setTextViewText(R.id.tv_detail_goal_target, targetText)
                views.setTextViewText(R.id.tv_detail_saved_amount, savedText)
                views.setProgressBar(R.id.pb_detail_progress, 100, progressValue, false)

                // Back Button Intent
                val backIntent = Intent(context, GoalsWidgetProvider::class.java).apply {
                    action = ACTION_GOAL_BACK
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val backPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    backIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btn_back_to_list, backPendingIntent)

            } else {
                // Show List View
                views.setViewVisibility(R.id.layout_goals_list, View.VISIBLE)
                views.setViewVisibility(R.id.layout_goal_detail, View.GONE)

                // Bind ListView to RemoteViewsService
                val serviceIntent = Intent(context, GoalsWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.lv_goals, serviceIntent)
                views.setEmptyView(R.id.lv_goals, R.id.tv_empty_goals)

                // Template for item clicks
                val clickIntent = Intent(context, GoalsWidgetProvider::class.java).apply {
                    action = ACTION_GOAL_CLICK
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val clickPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 10000,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setPendingIntentTemplate(R.id.lv_goals, clickPendingIntent)

                // Empty state click launches Main App
                val emptyIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val emptyPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId + 20000,
                    emptyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.tv_empty_goals, emptyPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        when (intent.action) {
            ACTION_GOAL_CLICK -> {
                val goalId = intent.getStringExtra(EXTRA_GOAL_ID)
                if (goalId != null) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putString(getKeySelectedGoal(appWidgetId), goalId).apply()
                    
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
                }
            }
            ACTION_GOAL_BACK -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().remove(getKeySelectedGoal(appWidgetId)).apply()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_goals)
                onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
            }
        }
    }

    companion object {
        const val PREFS_NAME = "goals_widget_prefs"
        const val ACTION_GOAL_CLICK = "com.aistudio.piggyledger.vpxqwm.widget.ACTION_GOAL_CLICK"
        const val ACTION_GOAL_BACK = "com.aistudio.piggyledger.vpxqwm.widget.ACTION_GOAL_BACK"
        const val EXTRA_GOAL_ID = "com.aistudio.piggyledger.vpxqwm.widget.EXTRA_GOAL_ID"

        private fun getKeySelectedGoal(widgetId: Int) = "selected_goal_id_$widgetId"

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, GoalsWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.lv_goals)
            
            val intent = Intent(context, GoalsWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
