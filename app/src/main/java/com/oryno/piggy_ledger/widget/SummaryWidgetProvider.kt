package com.oryno.piggy_ledger.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import com.oryno.piggy_ledger.MainActivity
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Loan
import com.oryno.piggy_ledger.data.LoanType
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

                val localizedContext = WidgetUtils.getLocalizedContext(context)

                // Format amounts as numbers only without currency symbol to save space
                val owedToMeStr = WidgetUtils.formatWidgetNumber(owedToMe)
                val iOweStr = WidgetUtils.formatWidgetNumber(iOwe)
                val netPrefix = if (netLedger <= -0.5) "-" else ""
                val absNet = abs(netLedger)
                val netLedgerStr = "$netPrefix${WidgetUtils.formatWidgetNumber(absNet)}"

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_summary)

                    // Localize all labels (header title removed to save space)
                    views.setTextViewText(R.id.tv_label_owed_to_me, localizedContext.getString(R.string.widget_owed_to_me))
                    views.setTextViewText(R.id.tv_label_i_owe, localizedContext.getString(R.string.widget_i_owe))
                    views.setTextViewText(R.id.tv_net_label, localizedContext.getString(R.string.widget_net_ledger))

                    // Set numbers without currency symbol
                    views.setTextViewText(R.id.tv_owed_to_me, owedToMeStr)
                    views.setTextViewText(R.id.tv_i_owe, iOweStr)
                    views.setTextViewText(R.id.tv_net_ledger, netLedgerStr)

                    // Dynamically adapt font size: longer numbers get smaller font size
                    views.setTextViewTextSize(
                        R.id.tv_owed_to_me,
                        TypedValue.COMPLEX_UNIT_SP,
                        getAdaptiveTextSize(owedToMeStr.length)
                    )
                    views.setTextViewTextSize(
                        R.id.tv_i_owe,
                        TypedValue.COMPLEX_UNIT_SP,
                        getAdaptiveTextSize(iOweStr.length)
                    )
                    views.setTextViewTextSize(
                        R.id.tv_net_ledger,
                        TypedValue.COMPLEX_UNIT_SP,
                        getAdaptiveTextSize(netLedgerStr.length)
                    )

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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    companion object {
        fun getAdaptiveTextSize(length: Int): Float {
            return when {
                length <= 4 -> 20f
                length <= 6 -> 17.5f
                length <= 8 -> 15f
                length <= 10 -> 13f
                length <= 12 -> 11.5f
                else -> 10f
            }
        }

        fun triggerUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, SummaryWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val intent = Intent(context, SummaryWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("SummaryWidgetProvider", "triggerUpdate failed", e)
            }
        }
    }
}
