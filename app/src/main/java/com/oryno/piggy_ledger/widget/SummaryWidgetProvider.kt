package com.oryno.piggy_ledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.oryno.piggy_ledger.MainActivity
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Loan
import com.oryno.piggy_ledger.data.LoanType
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.math.abs

class SummaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PiggyLedgerDatabase.getInstance(context).piggyLedgerDao()
                val loans = db.getAllLoansSync().filter { !it.isPaidOff && !it.is_deleted }
                val payments = db.getAllLoanPaymentsSync()

                fun getRemainingAmount(loan: Loan): Double {
                    val paid = payments.filter { it.loanId == loan.id && !it.is_deleted }.sumOf { it.amount }
                    return (loan.amount - paid).coerceAtLeast(0.0)
                }

                val owedToMe = loans.filter { it.type == LoanType.LENT }.sumOf { getRemainingAmount(it) }
                val iOwe = loans.filter { it.type == LoanType.BORROWED }.sumOf { getRemainingAmount(it) }
                val netLedger = owedToMe - iOwe

                val userPrefs = UserPreferences(context)
                val code = userPrefs.appCurrency.firstOrNull() ?: "EGP"
                val appCurrency = if (code.isBlank()) "EGP" else code
                val currencySymbol = WidgetUtils.getWidgetCurrencySymbol(context, appCurrency)
                val localizedContext = WidgetUtils.getLocalizedContext(context)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_summary)

                    // Localize all labels
                    views.setTextViewText(R.id.tv_header_payoffs_loans, localizedContext.getString(R.string.payoffs_loans))
                    views.setTextViewText(R.id.tv_label_owed_to_me, localizedContext.getString(R.string.widget_owed_to_me))
                    views.setTextViewText(R.id.tv_label_i_owe, localizedContext.getString(R.string.widget_i_owe))
                    views.setTextViewText(R.id.tv_net_label, localizedContext.getString(R.string.widget_net_ledger))

                    // Format amounts with localized currency
                    views.setTextViewText(R.id.tv_owed_to_me, WidgetUtils.formatWidgetAmount(owedToMe, currencySymbol))
                    views.setTextViewText(R.id.tv_i_owe, WidgetUtils.formatWidgetAmount(iOwe, currencySymbol))

                    val netPrefix = if (netLedger < 0.0) "-" else ""
                    val absNet = abs(netLedger)
                    views.setTextViewText(R.id.tv_net_ledger, "$netPrefix${WidgetUtils.formatWidgetAmount(absNet, currencySymbol)}")

                    if (netLedger >= 0.0) {
                        views.setTextColor(R.id.tv_net_ledger, Color.parseColor("#10B981"))
                    } else {
                        views.setTextColor(R.id.tv_net_ledger, Color.parseColor("#EF4444"))
                    }

                    // Tapping the widget opens the app
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)

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
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, SummaryWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, SummaryWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
