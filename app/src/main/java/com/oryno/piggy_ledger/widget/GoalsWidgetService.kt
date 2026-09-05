package com.oryno.piggy_ledger.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Goal
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.Transaction
import com.oryno.piggy_ledger.data.UserPreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class GoalsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return GoalsRemoteViewsFactory(this.applicationContext)
    }
}

class GoalsRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var goals: List<Goal> = emptyList()
    private var transactions: List<Transaction> = emptyList()
    private var currencySymbol: String = "EGP"
    private var db: PiggyLedgerDatabase? = null

    override fun onCreate() {
        db = PiggyLedgerDatabase.getInstance(context)
    }

    override fun onDataSetChanged() {
        runBlocking {
            try {
                db?.piggyLedgerDao()?.let { dao ->
                    goals = dao.getActiveGoalsSync().filter { !it.is_deleted }
                    transactions = dao.getActiveTransactionsSync()
                    
                    val userPrefs = UserPreferences(context)
                    val code = userPrefs.appCurrency.firstOrNull() ?: "EGP"
                    val appCurrency = if (code.isBlank()) "EGP" else code
                    currencySymbol = WidgetUtils.getWidgetCurrencySymbol(context, appCurrency)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        goals = emptyList()
        transactions = emptyList()
    }

    override fun getCount(): Int = goals.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= goals.size) return RemoteViews(context.packageName, R.layout.item_widget_goal)
        
        val goal = goals[position]
        val views = RemoteViews(context.packageName, R.layout.item_widget_goal)
        val localizedContext = WidgetUtils.getLocalizedContext(context)
        
        val savedAmount = transactions.filter { it.goalId == goal.id && !it.is_deleted }.sumOf { it.amount }
        val isOpen = goal.targetAmount <= 0.0
        val progress = if (!isOpen) ((savedAmount / goal.targetAmount) * 100).toInt() else 0
        val displayProgress = progress.coerceIn(0, 100)
        
        views.setTextViewText(R.id.tv_goal_name, goal.name)
        if (isOpen) {
            views.setTextViewText(R.id.tv_goal_target, "/ " + localizedContext.getString(R.string.widget_open_savings))
            views.setTextViewText(R.id.tv_goal_percentage, localizedContext.getString(R.string.widget_open_label))
        } else {
            views.setTextViewText(R.id.tv_goal_target, "/ " + WidgetUtils.formatWidgetAmount(goal.targetAmount, currencySymbol))
            views.setTextViewText(R.id.tv_goal_percentage, "$displayProgress%")
        }
        
        val formattedSaved = WidgetUtils.formatWidgetAmount(savedAmount, currencySymbol)
        val savedText = try {
            localizedContext.getString(R.string.widget_amount_saved, formattedSaved)
        } catch (e: Exception) {
            "$formattedSaved saved"
        }
        views.setTextViewText(R.id.tv_goal_saved, savedText)
        
        // Fill-in Intent for opening goal details inside the widget on click!
        val fillInIntent = Intent().apply {
            putExtra("extra_goal_name", goal.name)
            putExtra("extra_target_amount", goal.targetAmount)
            putExtra("extra_saved_amount", savedAmount)
            putExtra("extra_progress", displayProgress)
            putExtra("extra_currency_symbol", currencySymbol)
        }
        views.setOnClickFillInIntent(R.id.item_root, fillInIntent)
        
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
