package com.oryno.piggy_ledger.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.PiggyLedgerDao
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.data.SubscriptionEntity
import com.oryno.piggy_ledger.ui.LOGO_DEV_TOKEN
import com.oryno.piggy_ledger.ui.buildLogoDevUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogItem(
    val name: String,
    val domain: String,
    val defaultAmount: Double,
    val currency: String = "EGP"
)

data class SubscriptionUiState(
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val totalMonthlyCost: Double = 0.0,
    val totalYearlyCost: Double = 0.0,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class SubscriptionViewModel(
    private val repository: PiggyLedgerRepository,
    private val dao: PiggyLedgerDao
) : ViewModel() {

    val catalogServices = listOf(
        CatalogItem("Netflix", "netflix.com", 250.0),
        CatalogItem("Spotify", "spotify.com", 75.0),
        CatalogItem("YouTube Premium", "youtube.com", 65.0),
        CatalogItem("ChatGPT Plus", "openai.com", 1000.0),
        CatalogItem("Shahid VIP", "shahid.mbc.net", 90.0),
        CatalogItem("OSN+", "osn.com", 120.0),
        CatalogItem("Amazon Prime", "amazon.com", 30.0),
        CatalogItem("Apple Music", "apple.com", 90.0)
    )

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = combine(
        _uiState,
        repository.allSubscriptions,
        repository.allAccounts
    ) { state, subs, accounts ->
        val activeSubs = subs.filter { !it.is_deleted && it.isActive }
        val monthlyTotal = activeSubs.sumOf { sub ->
            when (sub.billingCycle.uppercase()) {
                "YEARLY" -> sub.amount / 12.0
                "WEEKLY" -> sub.amount * 4.33
                else -> sub.amount
            }
        }
        val yearlyTotal = monthlyTotal * 12.0

        state.copy(
            subscriptions = activeSubs,
            accounts = accounts.filter { !it.isSystemAccount && !it.is_deleted },
            totalMonthlyCost = monthlyTotal,
            totalYearlyCost = yearlyTotal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubscriptionUiState()
    )

    fun addSubscription(
        name: String,
        domain: String,
        amount: Double,
        billingCycle: String = "MONTHLY",
        accountId: String = "",
        autoDebit: Boolean = false
    ) {
        if (name.isBlank() || amount <= 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val logoUrl = if (domain.isNotBlank()) buildLogoDevUrl(domain) else null

            val sub = SubscriptionEntity(
                name = name,
                logoUrl = logoUrl,
                amount = amount,
                billingCycle = billingCycle,
                accountId = accountId,
                autoDebit = autoDebit,
                nextRenewalDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            )

            repository.insertSubscription(sub)
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun deleteSubscription(id: String) {
        viewModelScope.launch {
            repository.deleteSubscription(id)
        }
    }
}
