package com.oryno.piggy_ledger.service

import android.content.Context
import android.util.Log
import com.oryno.piggy_ledger.data.Loan
import com.oryno.piggy_ledger.data.LoanPayment
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.SubscriptionEntity
import com.oryno.piggy_ledger.data.UserPreferences
import com.oryno.piggy_ledger.ui.NotificationHelper
import com.oryno.piggy_ledger.ui.getCurrencySymbol
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object DeadlineAndReminderUtils {

    private const val PREFS_NAME = "piggy_deadline_reminder_prefs"
    private const val KEY_RENEWAL_LEAD_PREFIX = "sub_renewal_leads_"
    private const val KEY_TRIAL_ENABLED_PREFIX = "sub_trial_enabled_"
    private const val KEY_TRIAL_LEAD_PREFIX = "sub_trial_leads_"
    private const val KEY_SUB_LAST_NOTIF_PREFIX = "sub_last_notif_"
    private const val KEY_LOAN_LAST_NOTIF_PREFIX = "loan_last_notif_"

    fun computeRemainingLoanAmount(loan: Loan, payments: List<LoanPayment>): Double {
        val paid = payments
            .asSequence()
            .filter { it.loanId == loan.id && !it.is_deleted }
            .sumOf { it.amount }
        return (loan.amount - paid).coerceAtLeast(0.0)
    }

    fun filterOverdueLoansWithRemaining(
        loans: List<Loan>,
        payments: List<LoanPayment>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Pair<Loan, Double>> {
        return loans
            .asSequence()
            .filter { !it.is_deleted && !it.isPaidOff && it.deadline != null && it.deadline < nowMillis }
            .map { loan -> loan to computeRemainingLoanAmount(loan, payments) }
            .filter { (_, remaining) -> remaining > 0.0001 }
            .toList()
    }

    /**
     * Calendar-day difference between [fromMillis] and [toMillis] in the default timezone.
     * Returns 0 if same calendar day, >0 if [toMillis] is in future days, <0 if overdue.
     */
    fun calendarDaysBetween(fromMillis: Long, toMillis: Long): Int {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = toMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMs = endCal.timeInMillis - startCal.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
    }

    fun parseReminderOptionToDays(option: String): Int? {
        return when (option.trim()) {
            "On renewal day" -> 0
            "1 day before" -> 1
            "2 days before" -> 2
            "3 days before" -> 3
            "1 week before" -> 7
            else -> null
        }
    }

    fun daysToReminderOption(days: Int): String {
        return when (days) {
            0 -> "On renewal day"
            1 -> "1 day before"
            2 -> "2 days before"
            3 -> "3 days before"
            7 -> "1 week before"
            else -> "1 day before"
        }
    }

    fun saveSubscriptionReminderConfig(
        context: Context,
        subscriptionId: String,
        renewalLeadDays: List<Int>,
        trialReminderEnabled: Boolean = true,
        trialLeadDays: List<Int> = emptyList()
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleanRenewal = renewalLeadDays.distinct().sorted()
        val cleanTrial = trialLeadDays.distinct().sorted()
        prefs.edit()
            .putString(KEY_RENEWAL_LEAD_PREFIX + subscriptionId, cleanRenewal.joinToString(","))
            .putBoolean(KEY_TRIAL_ENABLED_PREFIX + subscriptionId, trialReminderEnabled)
            .putString(KEY_TRIAL_LEAD_PREFIX + subscriptionId, cleanTrial.joinToString(","))
            .apply()
    }

    fun getSubscriptionRenewalLeadDays(
        context: Context,
        subscription: SubscriptionEntity
    ): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RENEWAL_LEAD_PREFIX + subscription.id, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct()
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(subscription.reminderLeadDays.coerceAtLeast(0))
    }

    fun isSubscriptionTrialReminderEnabled(context: Context, subscriptionId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TRIAL_ENABLED_PREFIX + subscriptionId, true)
    }

    fun getSubscriptionTrialLeadDays(context: Context, subscriptionId: String): List<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TRIAL_LEAD_PREFIX + subscriptionId, null)
        if (!raw.isNullOrBlank()) {
            val parsed = raw.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct()
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(1, 3, 7)
    }

    fun clearSubscriptionPrefs(context: Context, subscriptionId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .remove(KEY_RENEWAL_LEAD_PREFIX + subscriptionId)
            .remove(KEY_TRIAL_ENABLED_PREFIX + subscriptionId)
            .remove(KEY_TRIAL_LEAD_PREFIX + subscriptionId)
        prefs.all.keys
            .filter { it.startsWith("${KEY_SUB_LAST_NOTIF_PREFIX}${subscriptionId}_") }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    fun shouldSendLoanDeadlineNotification(
        context: Context,
        loanId: String,
        nowMillis: Long,
        minIntervalMs: Long
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSent = prefs.getLong(KEY_LOAN_LAST_NOTIF_PREFIX + loanId, 0L)
        return (nowMillis - lastSent) >= minIntervalMs
    }

    fun markLoanDeadlineNotified(context: Context, loanId: String, nowMillis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LOAN_LAST_NOTIF_PREFIX + loanId, nowMillis).apply()
    }

    fun clearLoanDeadlineNotified(context: Context, loanId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LOAN_LAST_NOTIF_PREFIX + loanId).apply()
    }

    fun shouldSendSubscriptionNotification(
        context: Context,
        subscriptionId: String,
        tag: String,
        nowMillis: Long,
        minIntervalMs: Long
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSent = prefs.getLong("${KEY_SUB_LAST_NOTIF_PREFIX}${subscriptionId}_$tag", 0L)
        return (nowMillis - lastSent) >= minIntervalMs
    }

    fun markSubscriptionNotified(
        context: Context,
        subscriptionId: String,
        tag: String,
        nowMillis: Long
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong("${KEY_SUB_LAST_NOTIF_PREFIX}${subscriptionId}_$tag", nowMillis).apply()
    }

    fun formatAmountWithCurrency(amount: Double, currency: String): String {
        return "${String.format(Locale.getDefault(), "%,.2f", amount)} $currency"
    }
}

object LoanDeadlineNotifier {

    private const val TAG = "LoanDeadlineNotifier"
    private val notifyMutex = Mutex()

    suspend fun notifyOverdueLoans(
        context: Context,
        minIntervalMs: Long
    ) = notifyMutex.withLock {
        try {
            if (UserPreferences.getSavedAppLanguageSync(context).isNullOrBlank()) {
                try {
                    val dsLang = UserPreferences(context).appLanguage.first()
                    if (!dsLang.isNullOrBlank()) {
                        UserPreferences.saveAppLanguageSync(context, dsLang)
                    }
                } catch (_: Exception) { }
            }

            val db = PiggyLedgerDatabase.getInstance(context)
            val dao = db.piggyLedgerDao()
            val allLoans = dao.getAllLoansSync()
            val allPayments = dao.getAllLoanPaymentsSync()
            val now = System.currentTimeMillis()

            val currencyCode = try {
                UserPreferences(context).appCurrency.first()
            } catch (_: Exception) {
                "EGP"
            }
            val currencySymbol = getCurrencySymbol(currencyCode)
            val notificationHelper = NotificationHelper(context)

            // Cancel notifications for loans that were deleted, paid off, or fully settled via logs
            val overduePairs = DeadlineAndReminderUtils.filterOverdueLoansWithRemaining(allLoans, allPayments, now)
            val overdueIds = overduePairs.map { it.first.id }.toSet()
            val overdueContactKeys = overduePairs
                .map { it.first.contactName.trim().lowercase(Locale.getDefault()) }
                .toSet()

            allLoans.filter { it.id !in overdueIds }.forEach { settledOrInactive ->
                val contactKey = settledOrInactive.contactName.trim().lowercase(Locale.getDefault())
                notificationHelper.cancelDeadlineNotification(
                    loanId = settledOrInactive.id,
                    legacyContactName = settledOrInactive.contactName,
                    cancelLegacyContactId = contactKey !in overdueContactKeys
                )
                if (settledOrInactive.is_deleted || settledOrInactive.isPaidOff) {
                    DeadlineAndReminderUtils.clearLoanDeadlineNotified(context, settledOrInactive.id)
                }
            }

            val validNotifIds = overduePairs.map { it.first.id.hashCode() }.toMutableSet()

            overduePairs.forEach { (loan, _) ->
                // Re-verify live DB state immediately before posting so concurrent deletes/settlements never notify
                val freshLoan = dao.getLoanById(loan.id)
                if (freshLoan == null || freshLoan.is_deleted || freshLoan.isPaidOff || freshLoan.deadline == null || freshLoan.deadline >= System.currentTimeMillis()) {
                    validNotifIds.remove(loan.id.hashCode())
                    notificationHelper.cancelDeadlineNotification(loan.id)
                    DeadlineAndReminderUtils.clearLoanDeadlineNotified(context, loan.id)
                    return@forEach
                }

                val freshPayments = dao.getAllLoanPaymentsSync()
                val freshRemaining = DeadlineAndReminderUtils.computeRemainingLoanAmount(freshLoan, freshPayments)
                if (freshRemaining <= 0.0001) {
                    validNotifIds.remove(loan.id.hashCode())
                    notificationHelper.cancelDeadlineNotification(loan.id)
                    return@forEach
                }

                val currentNow = System.currentTimeMillis()
                if (DeadlineAndReminderUtils.shouldSendLoanDeadlineNotification(context, freshLoan.id, currentNow, minIntervalMs)) {
                    notificationHelper.showDeadlineNotification(
                        contactName = freshLoan.contactName,
                        amount = freshRemaining,
                        currencySymbol = currencySymbol,
                        notificationId = freshLoan.id.hashCode()
                    )
                    DeadlineAndReminderUtils.markLoanDeadlineNotified(context, freshLoan.id, currentNow)
                }
            }

            // Reconcile any orphaned notifications still sitting in the deadline channel
            notificationHelper.cancelOrphanedDeadlineNotifications(validNotifIds)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to evaluate or notify overdue loans", e)
        }
    }
}
