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

    fun getStreakAndFrozenDates(context: Context): Pair<Int, Set<String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dates = prefs.getStringSet(KEY_ACTION_DATES, emptySet()) ?: return Pair(0, emptySet())
        if (dates.isEmpty()) return Pair(0, emptySet())

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        
        val currentDay = Calendar.getInstance()
        var streak = 0
        var consecutiveFreezes = 0
        val monthFreezes = mutableMapOf<String, Int>()
        var isFirstDay = true

        val frozenDates = mutableSetOf<String>()
        val pendingFreezes = mutableSetOf<String>()

        // Find the earliest date to avoid infinite loop safely
        var earliestDateStr = "9999-99-99"
        for (d in dates) {
            if (d < earliestDateStr) earliestDateStr = d
        }

        while (true) {
            val dateStr = dateFormat.format(currentDay.time)
            val monthStr = monthFormat.format(currentDay.time)

            if (dates.contains(dateStr)) {
                streak++
                consecutiveFreezes = 0
                frozenDates.addAll(pendingFreezes)
                pendingFreezes.clear()
            } else {
                if (!isFirstDay) {
                    val freezes = monthFreezes[monthStr] ?: 0
                    if (consecutiveFreezes < 3 && freezes < 5) {
                        consecutiveFreezes++
                        monthFreezes[monthStr] = freezes + 1
                        pendingFreezes.add(dateStr)
                    } else {
                        // Streak broken
                        break
                    }
                }
            }

            if (dateStr < earliestDateStr) {
                // Traversed past the earliest known action
                break
            }

            currentDay.add(Calendar.DAY_OF_YEAR, -1)
            isFirstDay = false
        }
        
        return Pair(streak, frozenDates)
    }

    fun getStreak(context: Context): Int {
        return getStreakAndFrozenDates(context).first
    }

    fun getLongestStreak(context: Context): Int {
        val dates = getActionDates(context)
        if (dates.isEmpty()) return 0
        val sortedDates = dates.sorted()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        var maxStreak = 0
        var currentStreak = 0
        var prevCal: Calendar? = null

        for (dateStr in sortedDates) {
            val date = try { dateFormat.parse(dateStr) } catch (e: Exception) { null } ?: continue
            val currCal = Calendar.getInstance().apply { time = date }
            
            if (prevCal == null) {
                currentStreak = 1
            } else {
                val diffDays = ((currCal.timeInMillis - prevCal.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
                if (diffDays == 1) {
                    currentStreak++
                } else if (diffDays > 1) {
                    currentStreak = 1
                }
            }
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
            prevCal = currCal
        }
        val activeStreak = getStreak(context)
        return maxOf(maxStreak, activeStreak)
    }

    fun getFrozenDates(context: Context): Set<String> {
        return getStreakAndFrozenDates(context).second
    }

    fun hasActionToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dates = prefs.getStringSet(KEY_ACTION_DATES, emptySet()) ?: return false
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return dates.contains(todayStr)
    }

    fun getConsecutiveMissedDays(context: Context): Int {
        val dates = getActionDates(context)
        if (dates.isEmpty()) return 0
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val currentDay = Calendar.getInstance()
        var missed = 0
        
        while (true) {
            val dateStr = dateFormat.format(currentDay.time)
            if (dates.contains(dateStr)) {
                break
            }
            missed++
            currentDay.add(Calendar.DAY_OF_YEAR, -1)
            
            // Limit to 100 days backwards just in case
            if (missed > 100) break
        }
        return missed
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
            val dates = getActionDates(context)
            if (dates.isNotEmpty()) {
                return PiggyState.LOST
            }
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 14 -> PiggyState.HAPPY // Morning to early afternoon
            hour < 18 -> PiggyState.WORRIED // Late afternoon
            else -> PiggyState.PANIC // Evening
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
