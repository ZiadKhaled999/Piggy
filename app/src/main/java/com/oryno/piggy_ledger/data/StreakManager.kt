package com.oryno.piggy_ledger.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StreakManager {
    private const val PREFS_NAME = "piggy_streak_prefs"
    private const val KEY_ACTION_DATES = "action_dates"

    fun recordAction(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dates = prefs.getStringSet(KEY_ACTION_DATES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (dates.add(todayStr)) {
            prefs.edit().putStringSet(KEY_ACTION_DATES, dates).apply()
        }
        // Trigger update for both widgets
        com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
        com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
    }

    fun getStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dates = prefs.getStringSet(KEY_ACTION_DATES, emptySet()) ?: return 0
        if (dates.isEmpty()) return 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Calendar.getInstance()
        val todayStr = dateFormat.format(today.time)

        // Check if today has an action
        val hasActionToday = dates.contains(todayStr)

        val checkCalendar = Calendar.getInstance()
        if (!hasActionToday) {
            // If not today, check yesterday. If yesterday also has no action, streak is 0
            checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(checkCalendar.time)
            if (!dates.contains(yesterdayStr)) {
                return 0
            }
        }

        // Calculate consecutive days backwards
        var streak = 0
        val countCalendar = Calendar.getInstance()
        if (!hasActionToday) {
            countCalendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val dateStr = dateFormat.format(countCalendar.time)
            if (dates.contains(dateStr)) {
                streak++
                countCalendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    fun hasActionToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dates = prefs.getStringSet(KEY_ACTION_DATES, emptySet()) ?: return false
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return dates.contains(todayStr)
    }

    fun getActionDates(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_ACTION_DATES, emptySet()) ?: emptySet()
    }

    fun setActionDates(context: Context, dates: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_ACTION_DATES, dates).apply()
        com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
        com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
    }

    enum class PiggyState {
        HAPPY, WORRIED, PANIC, SUCCESS, LOST
    }

    fun getPiggyState(context: Context): PiggyState {
        if (hasActionToday(context)) {
            return PiggyState.SUCCESS
        }

        val streak = getStreak(context)
        if (streak == 0) {
            // Check if we had an action yesterday. If not, and no action today, it's LOST.
            // Actually getStreak already returns 0 if no action today and yesterday.
            // But if we just started, streak might be 0.
            // The logic says: "If the clock hits 00:00 and lastLessonTimestamp belongs to the day before yesterday, the streak resets to 0."
            val dates = getActionDates(context)
            if (dates.isNotEmpty()) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val yesterday = Calendar.getInstance()
                yesterday.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayStr = dateFormat.format(yesterday.time)
                
                if (!dates.contains(yesterdayStr)) {
                    return PiggyState.LOST
                }
            }
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 18 -> PiggyState.HAPPY
            hour < 22 -> PiggyState.WORRIED
            else -> PiggyState.PANIC
        }
    }

    fun getPiggyResource(state: PiggyState): Int {
        return when (state) {
            PiggyState.HAPPY -> com.oryno.piggy_ledger.R.drawable.ic_piggy_happy
            PiggyState.WORRIED -> com.oryno.piggy_ledger.R.drawable.ic_piggy_worried
            PiggyState.PANIC -> com.oryno.piggy_ledger.R.drawable.ic_piggy_panic
            PiggyState.SUCCESS -> com.oryno.piggy_ledger.R.drawable.ic_piggy_success
            PiggyState.LOST -> com.oryno.piggy_ledger.R.drawable.ic_piggy_lost
        }
    }
}
