package com.oryno.piggy_ledger.data

import android.content.Context
import com.oryno.piggy_ledger.R
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StreakManager {
    private const val PREFS_NAME = "piggy_streak_prefs"
    private const val KEY_ACTION_DATES = "action_dates"

    data class WidgetDisplayInfo(
        val streakCount: Int,
        val badgeResource: Int,
        val backgroundResource: Int,
        val speechMessage: String,
        val mascotResource: Int,
        val mascotBitmap: android.graphics.Bitmap? = null
    )

    fun recordAction(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dates = prefs.getStringSet(KEY_ACTION_DATES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (dates.add(todayStr)) {
            prefs.edit().putStringSet(KEY_ACTION_DATES, dates).apply()
            syncStreakToDb(context)
        }
        // Trigger update for both widgets
        com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
        com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
    }

    fun removeTodayAction(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dates = prefs.getStringSet(KEY_ACTION_DATES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (dates.remove(todayStr)) {
            prefs.edit().putStringSet(KEY_ACTION_DATES, dates).apply()
            syncStreakToDb(context)
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
                        break
                    }
                }
            }

            if (dateStr < earliestDateStr) {
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
        syncStreakToDb(context)
        com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
        com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
        com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
    }

    fun syncFromCloud(context: Context, remoteDates: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val localDates = prefs.getStringSet(KEY_ACTION_DATES, emptySet()) ?: emptySet()
        val merged = localDates.toMutableSet().apply { addAll(remoteDates) }
        prefs.edit().putStringSet(KEY_ACTION_DATES, merged).apply()
        com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
        com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
    }

    fun syncStreakToDb(context: Context) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val user = com.clerk.api.Clerk.userFlow.value
                val userId = user?.id ?: "local_user"
                val dates = getActionDates(context)
                val entities = dates.map { dateStr ->
                    StreakDateEntity(
                        id = "${userId}_$dateStr",
                        userId = userId,
                        dateStr = dateStr,
                        updatedAt = System.currentTimeMillis(),
                        isSynced = false
                    )
                }
                val dao = PiggyLedgerDatabase.getInstance(context.applicationContext).piggyLedgerDao()
                dao.insertStreakDates(entities)
                
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.oryno.piggy_ledger.service.SyncWorker>().build()
                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                    "SyncWork",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                com.oryno.piggy_ledger.service.SyncManager(context).syncAll()
            } catch (e: Exception) {
                android.util.Log.e("StreakManager", "Failed to sync streak dates to Room", e)
            }
        }
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
            hour < 14 -> PiggyState.HAPPY
            hour < 18 -> PiggyState.WORRIED
            else -> PiggyState.PANIC
        }
    }

    fun getPiggyResource(state: PiggyState): Int {
        return when (state) {
            PiggyState.HAPPY -> R.drawable.ic_piggy_happy
            PiggyState.WORRIED -> R.drawable.ic_piggy_worried
            PiggyState.PANIC -> R.drawable.ic_piggy_panic
            PiggyState.SUCCESS -> R.drawable.ic_piggy_success
            PiggyState.LOST -> R.drawable.ic_piggy_lost
        }
    }

    fun getWidgetDisplayInfo(context: Context): WidgetDisplayInfo {
        val streak = getStreak(context)
        val actionToday = hasActionToday(context)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val frozenDates = getFrozenDates(context)
        val isFrozenToday = frozenDates.contains(todayStr)
        val dates = getActionDates(context)
        val missedDays = getConsecutiveMissedDays(context)

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        val categoryId: Int
        val badgeRes: Int
        val bgRes: Int
        val mascotRes: Int

        when {
            actionToday -> {
                categoryId = 8 // Streak Extended / Logged Today
                badgeRes = R.drawable.streak
                bgRes = R.drawable.bg_widget_card
                mascotRes = R.drawable.ic_piggy_success
            }
            isFrozenToday -> {
                categoryId = 7 // Streak Frozen State
                badgeRes = R.drawable.streak_frozen
                bgRes = R.drawable.bg_widget_card
                mascotRes = R.drawable.ic_piggy_worried
            }
            streak == 0 && dates.isNotEmpty() -> {
                categoryId = if (missedDays > 3) 10 else 9 // Ghosted or Broken
                badgeRes = R.drawable.streak_missed
                bgRes = R.drawable.bg_widget_card
                mascotRes = R.drawable.ic_piggy_lost
            }
            dates.isEmpty() -> {
                categoryId = 12 // Streak Born
                badgeRes = R.drawable.streak
                bgRes = R.drawable.bg_widget_card
                mascotRes = R.drawable.ic_piggy_happy
            }
            else -> {
                when (hour) {
                    in 0..3 -> {
                        categoryId = 1
                        badgeRes = R.drawable.streak
                        bgRes = R.drawable.bg_widget_card
                        mascotRes = R.drawable.ic_piggy_happy
                    }
                    in 4..11 -> {
                        categoryId = 2
                        badgeRes = R.drawable.streak
                        bgRes = R.drawable.bg_widget_card
                        mascotRes = R.drawable.ic_piggy_happy
                    }
                    in 12..15 -> {
                        categoryId = 3
                        badgeRes = R.drawable.streak
                        bgRes = R.drawable.bg_widget_card
                        mascotRes = R.drawable.ic_piggy_happy
                    }
                    in 16..19 -> {
                        categoryId = 4
                        badgeRes = R.drawable.streak
                        bgRes = R.drawable.bg_widget_card
                        mascotRes = R.drawable.ic_piggy_worried
                    }
                    in 20..21 -> {
                        categoryId = 5
                        badgeRes = R.drawable.streak_missed
                        bgRes = R.drawable.bg_widget_card
                        mascotRes = R.drawable.ic_piggy_worried
                    }
                    else -> {
                        categoryId = 6
                        badgeRes = R.drawable.streak_missed
                        bgRes = R.drawable.bg_widget_card
                        mascotRes = R.drawable.ic_piggy_panic
                    }
                }
            }
        }

        val lang = context.resources.configuration.locales.get(0).language
        val isArabic = lang == "ar"

        val remoteResult = PiggyRemoteConfigManager.resolveMascot(
            context = context,
            categoryId = categoryId,
            streak = streak,
            isActionToday = actionToday,
            isFrozen = isFrozenToday,
            isLost = streak == 0 && dates.isNotEmpty(),
            hour = hour,
            languageIsArabic = isArabic
        )

        val rawPhrase = remoteResult.phrase ?: getRandomPhraseFromCategory(context, categoryId, seed = dayOfYear * 24 + hour)
        val userName = getUserName(context)
        val formattedSpeech = rawPhrase
            .replace("[Username]", userName)
            .replace("[USER_NAME]", userName)
            .replace("[Number]", (if (streak > 0) streak else missedDays).toString())
            .replace("[Course]", "Budget")

        return WidgetDisplayInfo(
            streakCount = streak,
            badgeResource = badgeRes,
            backgroundResource = bgRes,
            speechMessage = formattedSpeech,
            mascotResource = mascotRes,
            mascotBitmap = remoteResult.bitmap
        )
    }

    private fun getUserName(context: Context): String {
        return try {
            val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val name = userPrefs.getString("auth_user_name", "") ?: ""
            if (name.isNotBlank()) name else "Saver"
        } catch (e: Exception) {
            "Saver"
        }
    }

    fun getStreakMessagesAsset(context: Context): String {
        val locale = try {
            val locales = context.resources.configuration.locales
            if (!locales.isEmpty) locales.get(0) else Locale.getDefault()
        } catch (e: Exception) {
            Locale.getDefault()
        }
        val tag = locale.toLanguageTag().lowercase()
        val country = locale.country.lowercase()
        val lang = locale.language.lowercase()

        return when {
            tag.contains("eg") || country == "eg" -> "piggy_streak_messages_ar_eg.json"
            lang == "ar" -> "piggy_streak_messages_ar.json"
            else -> "piggy_streak_messages.json"
        }
    }

    private fun getRandomPhraseFromCategory(context: Context, categoryId: Int, seed: Int): String {
        return try {
            val assetName = getStreakMessagesAsset(context)
            val inputStream: InputStream = context.assets.open(assetName)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val categoriesArray = jsonObject.getJSONArray("categories")

            var itemsList = mutableListOf<String>()
            for (i in 0 until categoriesArray.length()) {
                val catObj = categoriesArray.getJSONObject(i)
                if (catObj.optInt("id") == categoryId) {
                    val itemsArr = catObj.getJSONArray("items")
                    for (j in 0 until itemsArr.length()) {
                        itemsList.add(itemsArr.getString(j))
                    }
                    break
                }
            }

            if (itemsList.isEmpty()) {
                getFallbackPhrase(context, categoryId)
            } else {
                val index = Math.abs(seed) % itemsList.size
                itemsList[index]
            }
        } catch (e: Exception) {
            getFallbackPhrase(context, categoryId)
        }
    }

    private fun getFallbackPhrase(context: Context, categoryId: Int): String {
        val assetName = getStreakMessagesAsset(context)
        val isEgyptian = assetName.contains("ar_eg")
        val isArabic = assetName.contains("ar")

        return when (categoryId) {
            1 -> if (isEgyptian) "بتسجل معاملة بالليل كده؟" else if (isArabic) "معاملة في وقت متأخر؟" else "Late night transaction?"
            2 -> if (isEgyptian) "صباح الفل! يلا نسجل مصاريف النهارده." else if (isArabic) "صباح الخير! حان وقت تسجيل المصاريف." else "Morning! Time to log expenses."
            3 -> if (isEgyptian) "معاك 30 ثانية تسجل المصاريف؟" else if (isArabic) "هل لديك 30 ثانية لتسجيل مصاريفك؟" else "Got 30 seconds to log?"
            4 -> if (isEgyptian) "بيجي مستني الفواتير والمصاريف!" else if (isArabic) "بيجي ينتظر فواتيرك ومصاريفك!" else "Piggy is waiting for receipts!"
            5 -> if (isEgyptian) "الوقت بيجري! الحق حافظ على شعلتك." else if (isArabic) "الوقت متأخر! حافظ على شعلتك وسلسلتك." else "It's getting late! Protect your streak."
            6 -> if (isEgyptian) "سجل مصاريفك حالاً قبل ما اليوم يخلص!" else if (isArabic) "سجل معاملاتك الآن قبل انتهاء اليوم!" else "LOG TRANSACTIONS NOW!"
            7 -> if (isEgyptian) "الشعلة اتجمدت في التلج!" else if (isArabic) "السلسلة مجمدة في الجليد!" else "Streak's frozen in ice!"
            8 -> if (isEgyptian) "عاش جداً يا بطل! ليدجرك متظبط أول بأول." else if (isArabic) "عمل رائع! سجلك المالي محدث أولاً بأول." else "Great job keeping your ledger updated!"
            9 -> if (isEgyptian) "يا خسارة! الشعلة انطفت." else if (isArabic) "للأسف! انقطعت سلسلتك." else "Oh no! Your streak broke."
            10 -> if (isEgyptian) "بيجي وحشته جداً! سجل معاملة وارجع كمل." else if (isArabic) "بيجي يفتقدك! سجل معاملة لتستمر." else "Piggy misses you! Log a transaction."
            11 -> if (isEgyptian) "أيام بتعدي من غير تسجيل..." else if (isArabic) "أيام مرت بدون تسجيل..." else "Days without logging..."
            12 -> if (isEgyptian) "يا هلا بيك! يلا نبني شعلة واستمرار مع بعض." else if (isArabic) "أهلاً بك! لنبني سلسلة مميزة معاً." else "Welcome! Let's build a streak."
            else -> if (isEgyptian) "جه وقت تسجيل المصاريف!" else if (isArabic) "حان وقت تسجيل المصاريف!" else "Time to log expenses!"
        }
    }
}
