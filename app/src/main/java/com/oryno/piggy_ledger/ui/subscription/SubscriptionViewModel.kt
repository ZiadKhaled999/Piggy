package com.oryno.piggy_ledger.ui.subscription

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountTransaction
import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.data.PiggyLedgerDao
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.data.SubscriptionEntity
import com.oryno.piggy_ledger.ui.buildLogoDevUrl
import com.oryno.piggy_ledger.widget.StreakWidgetProvider
import com.oryno.piggy_ledger.widget.SummaryWidgetProvider
import com.posthog.PostHog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class SubscriptionTab {
    ACTIVE,
    PAUSED,
    ARCHIVED
}

data class CatalogItem(
    val name: String,
    val domain: String,
    val defaultAmountEgp: Double,
    val defaultAmountUsd: Double,
    val defaultCategory: String = "subscriptions"
)

private data class LocalUiFlags(
    val selectedTab: SubscriptionTab = SubscriptionTab.ACTIVE,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val paymentSuccessMessage: String? = null
)

data class SubscriptionUiState(
    val allSubscriptions: List<SubscriptionEntity> = emptyList(),
    val activeSubscriptions: List<SubscriptionEntity> = emptyList(),
    val pausedSubscriptions: List<SubscriptionEntity> = emptyList(),
    val archivedSubscriptions: List<SubscriptionEntity> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedTab: SubscriptionTab = SubscriptionTab.ACTIVE,
    val totalMonthlyCost: Double = 0.0,
    val totalYearlyCost: Double = 0.0,
    val dueSoonSubscription: SubscriptionEntity? = null,
    val dueSoonDays: Int = -1,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val paymentSuccessMessage: String? = null
)

class SubscriptionViewModel(
    private val repository: PiggyLedgerRepository,
    private val dao: PiggyLedgerDao
) : ViewModel() {

    val catalogServices = listOf(
        CatalogItem("Netflix", "netflix.com", 250.0, 15.49, "entertainment"),
        CatalogItem("Spotify", "spotify.com", 75.0, 10.99, "entertainment"),
        CatalogItem("YouTube Premium", "youtube.com", 65.0, 13.99, "entertainment"),
        CatalogItem("Figma", "figma.com", 600.0, 12.00, "work"),
        CatalogItem("ChatGPT Plus", "openai.com", 1000.0, 20.00, "work"),
        CatalogItem("iCloud+", "apple.com", 45.0, 2.99, "subscriptions"),
        CatalogItem("Amazon Prime", "amazon.com", 30.0, 14.99, "shopping"),
        CatalogItem("Shahid VIP", "shahid.mbc.net", 90.0, 8.99, "entertainment"),
        CatalogItem("Disney+", "disneyplus.com", 120.0, 9.99, "entertainment"),
        CatalogItem("Apple Music", "apple.com", 90.0, 10.99, "entertainment")
    )

    private val _flags = MutableStateFlow(LocalUiFlags())

    val uiState: StateFlow<SubscriptionUiState> = combine(
        repository.allSubscriptions,
        repository.allAccounts,
        _flags
    ) { subs, accounts, flags ->
        val validSubs = subs.filter { !it.is_deleted }
        val active = validSubs.filter { it.status.equals("ACTIVE", ignoreCase = true) && it.isActive }
        val paused = validSubs.filter { it.status.equals("PAUSED", ignoreCase = true) || (!it.isActive && !it.status.equals("ARCHIVED", ignoreCase = true)) }
        val archived = validSubs.filter { it.status.equals("ARCHIVED", ignoreCase = true) }

        val monthlyTotal = active.sumOf { sub ->
            when (sub.billingCycle.uppercase()) {
                "YEARLY" -> sub.amount / 12.0
                "WEEKLY" -> sub.amount * 4.33
                else -> sub.amount
            }
        }
        val yearlyTotal = monthlyTotal * 12.0

        // Find nearest due active subscription (within next 3 days)
        val now = System.currentTimeMillis()
        val dueSoonPair = active
            .map { sub ->
                val diffDays = TimeUnit.MILLISECONDS.toDays(sub.nextRenewalDate - now).toInt()
                Pair(sub, diffDays)
            }
            .filter { it.second in 0..3 }
            .minByOrNull { it.second }

        SubscriptionUiState(
            allSubscriptions = validSubs,
            activeSubscriptions = active,
            pausedSubscriptions = paused,
            archivedSubscriptions = archived,
            accounts = accounts.filter { !it.isSystemAccount && !it.is_deleted },
            selectedTab = flags.selectedTab,
            totalMonthlyCost = monthlyTotal,
            totalYearlyCost = yearlyTotal,
            dueSoonSubscription = dueSoonPair?.first,
            dueSoonDays = dueSoonPair?.second ?: -1,
            isSubmitting = flags.isSubmitting,
            errorMessage = flags.errorMessage,
            paymentSuccessMessage = flags.paymentSuccessMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubscriptionUiState()
    )

    fun selectTab(tab: SubscriptionTab) {
        _flags.update { it.copy(selectedTab = tab) }
    }

    fun clearMessages() {
        _flags.update { it.copy(errorMessage = null, paymentSuccessMessage = null) }
    }

    fun addSubscription(
        name: String,
        domain: String,
        amount: Double,
        currency: String = "EGP",
        billingCycle: String = "MONTHLY",
        accountId: String = "",
        category: String = "subscriptions",
        hasFreeTrial: Boolean = false,
        trialDays: Int = 0
    ) {
        if (name.isBlank() || amount <= 0) return

        viewModelScope.launch {
            _flags.update { it.copy(isSubmitting = true) }
            val logoUrl = if (domain.isNotBlank()) buildLogoDevUrl(domain) else null

            val now = System.currentTimeMillis()
            val nextRenewal = if (hasFreeTrial && trialDays > 0) {
                now + (trialDays.toLong() * 24L * 60L * 60L * 1000L)
            } else {
                calculateNextCycleTimestamp(now, billingCycle)
            }

            val sub = SubscriptionEntity(
                name = name.trim(),
                logoUrl = logoUrl,
                amount = amount,
                currency = currency.uppercase(),
                billingCycle = billingCycle.uppercase(),
                nextRenewalDate = nextRenewal,
                accountId = accountId,
                autoDebit = false,
                isActive = true,
                category = category,
                status = "ACTIVE",
                hasFreeTrial = hasFreeTrial,
                trialDays = trialDays,
                trialEndsAt = if (hasFreeTrial && trialDays > 0) nextRenewal else 0L,
                totalPaidCount = if (hasFreeTrial) 0 else 1,
                totalSpent = if (hasFreeTrial) 0.0 else amount,
                createdAt = now,
                updatedAt = now
            )

            repository.insertSubscription(sub)
            PostHog.capture(
                event = "subscription_added",
                properties = mapOf("name" to name, "amount" to amount, "currency" to currency)
            )
            _flags.update { it.copy(isSubmitting = false) }
        }
    }

    fun updateSubscription(
        id: String,
        name: String,
        domain: String?,
        amount: Double,
        currency: String,
        billingCycle: String,
        accountId: String,
        category: String
    ) {
        viewModelScope.launch {
            val existing = uiState.value.allSubscriptions.find { it.id == id } ?: return@launch
            val logoUrl = if (!domain.isNullOrBlank()) buildLogoDevUrl(domain) else existing.logoUrl
            val updated = existing.copy(
                name = name.trim(),
                logoUrl = logoUrl,
                amount = amount,
                currency = currency.uppercase(),
                billingCycle = billingCycle.uppercase(),
                accountId = accountId,
                category = category,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSubscription(updated)
        }
    }

    fun toggleAutoRenew(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            val updated = subscription.copy(
                autoDebit = !subscription.autoDebit,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSubscription(updated)
        }
    }

    fun updateLinkedAccount(subscription: SubscriptionEntity, newAccountId: String) {
        viewModelScope.launch {
            val updated = subscription.copy(
                accountId = newAccountId,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSubscription(updated)
        }
    }

    /**
     * Executes real payment:
     * 1. Creates AccountTransaction (deducting real balance from account)
     * 2. Deducts account.current_balance
     * 3. Advances subscription's nextRenewalDate by 1 cycle
     * 4. Increments totalPaidCount and totalSpent
     * 5. Triggers widgets and sync
     */
    fun payNow(
        subscription: SubscriptionEntity,
        overrideAccountId: String? = null,
        context: Context? = null
    ) {
        val targetAccountId = if (!overrideAccountId.isNullOrBlank()) overrideAccountId else subscription.accountId
        if (targetAccountId.isBlank()) {
            _flags.update { it.copy(errorMessage = "Please link a wallet to pay this subscription") }
            return
        }

        viewModelScope.launch {
            _flags.update { it.copy(isSubmitting = true) }
            val account = dao.getAccountByIdSync(targetAccountId)
            if (account == null) {
                _flags.update { it.copy(errorMessage = "Selected wallet not found", isSubmitting = false) }
                return@launch
            }

            val now = System.currentTimeMillis()
            val expenseAmount = subscription.amount

            // 1. Create real AccountTransaction (expense is negative)
            val tx = AccountTransaction(
                account_id = targetAccountId,
                amount = -expenseAmount,
                merchant = "${subscription.category}|${subscription.name} (Subscription)",
                source = "SUBSCRIPTION",
                timestamp = now
            )
            repository.insertAccountTransaction(tx)

            // 2. Deduct real balance from wallet
            val newBalance = account.current_balance - expenseAmount
            val newCredit = if (account.type == AccountType.CARD && account.available_credit != null) {
                account.available_credit - expenseAmount
            } else {
                account.available_credit
            }
            repository.updateAccount(
                account.copy(
                    current_balance = newBalance,
                    available_credit = newCredit,
                    isSynced = false,
                    updatedAt = now
                )
            )

            // 3. Advance subscription renewal date and update spend stats
            val nextRenewal = calculateNextCycleTimestamp(
                if (subscription.nextRenewalDate > now) subscription.nextRenewalDate else now,
                subscription.billingCycle
            )

            val updatedSub = subscription.copy(
                accountId = targetAccountId,
                nextRenewalDate = nextRenewal,
                hasFreeTrial = false, // Trial ends upon payment
                totalPaidCount = subscription.totalPaidCount + 1,
                totalSpent = subscription.totalSpent + expenseAmount,
                updatedAt = now
            )
            repository.updateSubscription(updatedSub)

            // 4. Update widgets and PostHog
            context?.let { ctx ->
                SummaryWidgetProvider.triggerUpdate(ctx)
                StreakWidgetProvider.triggerUpdate(ctx)
            }
            PostHog.capture(
                event = "subscription_paid",
                properties = mapOf(
                    "subscription_name" to subscription.name,
                    "amount" to expenseAmount,
                    "currency" to subscription.currency,
                    "account_name" to account.name
                )
            )

            _flags.update {
                it.copy(
                    isSubmitting = false,
                    paymentSuccessMessage = "${subscription.name}: ${String.format(java.util.Locale.getDefault(), "%,.2f", expenseAmount)} ${subscription.currency}"
                )
            }
        }
    }

    fun updateReminderSettings(
        subscription: SubscriptionEntity,
        enabled: Boolean,
        leadDays: Int,
        dailyRepeat: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            val updated = subscription.copy(
                reminderEnabled = enabled,
                reminderLeadDays = leadDays,
                reminderDailyRepeat = dailyRepeat,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSubscription(updated)
            if (enabled && updated.status.equals("ACTIVE", ignoreCase = true) && updated.isActive) {
                com.oryno.piggy_ledger.service.SubscriptionNotificationScheduler.schedule(context, updated)
            } else {
                com.oryno.piggy_ledger.service.SubscriptionNotificationScheduler.cancel(context, updated.id)
            }
        }
    }

    fun pauseSubscription(subscription: SubscriptionEntity, context: Context? = null) {
        viewModelScope.launch {
            val updated = subscription.copy(
                status = "PAUSED",
                isActive = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSubscription(updated)
            context?.let { com.oryno.piggy_ledger.service.SubscriptionNotificationScheduler.cancel(it, subscription.id) }
            PostHog.capture(
                event = "subscription_paused",
                properties = mapOf("name" to subscription.name)
            )
        }
    }

    fun resumeSubscription(subscription: SubscriptionEntity, context: Context? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val nextRenewal = if (subscription.nextRenewalDate <= now) {
                calculateNextCycleTimestamp(now, subscription.billingCycle)
            } else {
                subscription.nextRenewalDate
            }
            val updated = subscription.copy(
                status = "ACTIVE",
                isActive = true,
                nextRenewalDate = nextRenewal,
                updatedAt = now
            )
            repository.updateSubscription(updated)
            context?.let { com.oryno.piggy_ledger.service.SubscriptionNotificationScheduler.schedule(it, updated) }
            PostHog.capture(
                event = "subscription_resumed",
                properties = mapOf("name" to subscription.name)
            )
        }
    }

    fun archiveSubscription(subscription: SubscriptionEntity, context: Context? = null) {
        viewModelScope.launch {
            val updated = subscription.copy(
                status = "ARCHIVED",
                isActive = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateSubscription(updated)
            context?.let { com.oryno.piggy_ledger.service.SubscriptionNotificationScheduler.cancel(it, subscription.id) }
            PostHog.capture(
                event = "subscription_archived",
                properties = mapOf("name" to subscription.name)
            )
        }
    }

    fun reactivateSubscription(subscription: SubscriptionEntity, context: Context? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val nextRenewal = calculateNextCycleTimestamp(now, subscription.billingCycle)
            val updated = subscription.copy(
                status = "ACTIVE",
                isActive = true,
                nextRenewalDate = nextRenewal,
                updatedAt = now
            )
            repository.updateSubscription(updated)
            context?.let { com.oryno.piggy_ledger.service.SubscriptionNotificationScheduler.schedule(it, updated) }
            PostHog.capture(
                event = "subscription_reactivated",
                properties = mapOf("name" to subscription.name)
            )
        }
    }

    fun deleteSubscription(id: String, context: Context? = null) {
        viewModelScope.launch {
            context?.let { com.oryno.piggy_ledger.service.SubscriptionNotificationScheduler.cancel(it, id) }
            repository.deleteSubscription(id)
        }
    }

    private fun calculateNextCycleTimestamp(baseTime: Long, billingCycle: String): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = baseTime }
        when (billingCycle.uppercase()) {
            "YEARLY" -> cal.add(Calendar.YEAR, 1)
            "WEEKLY" -> cal.add(Calendar.DAY_OF_YEAR, 7)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }
}
