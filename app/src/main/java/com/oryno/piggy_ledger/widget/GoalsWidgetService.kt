package com.oryno.piggy_ledger.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.data.Goal
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.Transaction
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.util.Locale

class GoalsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return GoalsRemoteViewsFactory(applicationContext)
    }
}

class GoalsRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var goalsList = listOf<Goal>()
    private var transactionsList = listOf<Transaction>()
    private val db = PiggyLedgerDatabase.getInstance(context)
    private val dao = db.piggyLedgerDao()

    private val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate() {
        // No initial setup needed
    }

    override fun onDataSetChanged() {
        runBlocking {
            goalsList = dao.getAllGoalsSync()
            transactionsList = dao.getAllTransactions()
        }
    }

    override fun onDestroy() {
        goalsList = emptyList()
        transactionsList = emptyList()
    }

    override fun getCount(): Int = goalsList.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= goalsList.size) return null

        val locales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        val config = android.content.res.Configuration(context.resources.configuration)
        if (!locales.isEmpty) {
            config.setLocales(android.os.LocaleList.forLanguageTags(locales.toLanguageTags()))
        }
        val localizedContext = context.createConfigurationContext(config)

        val goal = goalsList[position]
        val goalTransactions = transactionsList.filter { it.goalId == goal.id }
        val savedAmount = goalTransactions.sumOf { it.amount }
        val isOpen = goal.targetAmount <= 0.0

        val targetText = if (isOpen) localizedContext.getString(R.string.widget_open_savings) else "/ $${formatter.format(goal.targetAmount)}"
        val savedText = localizedContext.getString(R.string.widget_amount_saved, "$${formatter.format(savedAmount)}")

        val percentageText = if (isOpen) {
            localizedContext.getString(R.string.widget_open_label)
        } else {
            val pct = if (goal.targetAmount > 0) (savedAmount / goal.targetAmount * 100).toInt() else 0
            "${pct.coerceIn(0, 100)}%"
        }

        val views = RemoteViews(context.packageName, R.layout.item_widget_goal)
        views.setTextViewText(R.id.tv_goal_name, goal.name)
        views.setTextViewText(R.id.tv_goal_target, targetText)
        views.setTextViewText(R.id.tv_goal_saved, savedText)
        views.setTextViewText(R.id.tv_goal_percentage, percentageText)

        val fillInIntent = Intent().apply {
            putExtra(GoalsWidgetProvider.EXTRA_GOAL_ID, goal.id)
        }
        views.setOnClickFillInIntent(R.id.item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = goalsList.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
