package com.oryno.piggy_ledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.oryno.piggy_ledger.MainActivity
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.LoanType
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SummaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Run update in background coroutine to avoid blocking Main thread (battery and thread safety)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = PiggyLedgerDatabase.getInstance(context)
                val loans = database.piggyLedgerDao().getAllLoansSync()

                val owedToMe = loans.filter { !it.isPaidOff && it.type == LoanType.LENT }.sumOf { it.amount }
                val iOwe = loans.filter { !it.isPaidOff && it.type == LoanType.BORROWED }.sumOf { it.amount }
                val netLedger = owedToMe - iOwe

                val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
                    maximumFractionDigits = 0
                }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_summary)

                    // Bind values
                    views.setTextViewText(R.id.tv_owed_to_me, "$${formatter.format(owedToMe)}")
                    views.setTextViewText(R.id.tv_i_owe, "$${formatter.format(iOwe)}")

                    val netText = if (netLedger >= 0) {
                        "+$${formatter.format(netLedger)}"
                    } else {
                        "-$${formatter.format(-netLedger)}"
                    }
                    views.setTextViewText(R.id.tv_net_ledger, netText)

                    // Accent Net Ledger color based on positivity
                    if (netLedger >= 0) {
                        views.setTextColor(R.id.tv_net_ledger, context.getColor(R.color.widget_green))
                    } else {
                        views.setTextColor(R.id.tv_net_ledger, context.getColor(R.color.widget_red))
                    }

                    // Intent to launch MainActivity when clicking the widget
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, SummaryWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, SummaryWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
