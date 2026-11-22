package com.oryno.piggy_ledger.widget

import android.content.Context
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.StreakManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HeatmapDataProvider {

    private const val TOTAL_DAYS = 365

    suspend fun dailyActivityCounts(context: Context, daysCount: Int = TOTAL_DAYS): List<Int> {
        val safeDays = daysCount.coerceAtLeast(1)
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -(safeDays - 1))
        }
        val sinceMillis = startCal.timeInMillis

        val countsByDate = HashMap<String, Int>(safeDays)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun recordTimestamp(ts: Long) {
            if (ts < sinceMillis) return
            val key = dateFormat.format(Date(ts))
            countsByDate[key] = (countsByDate[key] ?: 0) + 1
        }

        val dao = PiggyLedgerDatabase.getInstance(context.applicationContext).piggyLedgerDao()

        try {
            dao.getAccountTransactionTimestampsSince(sinceMillis).forEach(::recordTimestamp)
            dao.getGoalTransactionTimestampsSince(sinceMillis).forEach(::recordTimestamp)
            dao.getLoanTimestampsSince(sinceMillis).forEach(::recordTimestamp)
            dao.getLoanPaymentTimestampsSince(sinceMillis).forEach(::recordTimestamp)
            dao.getJournalEntryTimestampsSince(sinceMillis).forEach(::recordTimestamp)
        } catch (e: Exception) {
            android.util.Log.e("HeatmapDataProvider", "Failed to query Room timestamps", e)
        }

        // Ensure streak dates (including cloud-synced or account-creation days) appear as active (>= 1)
        try {
            val streakDates = LinkedHashSet<String>()
            streakDates.addAll(StreakManager.getActionDates(context))
            streakDates.addAll(dao.getActiveStreakDateStrings())
            for (dateStr in streakDates) {
                if ((countsByDate[dateStr] ?: 0) == 0) {
                    countsByDate[dateStr] = 1
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HeatmapDataProvider", "Failed to merge streak dates", e)
        }

        val result = ArrayList<Int>(safeDays)
        val cursor = startCal.clone() as Calendar
        for (i in 0 until safeDays) {
            val key = dateFormat.format(cursor.time)
            result.add(countsByDate[key] ?: 0)
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }
}
