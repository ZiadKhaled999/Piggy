package com.oryno.piggy_ledger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryno.piggy_ledger.data.Goal
import com.oryno.piggy_ledger.data.Loan
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.data.Transaction
import com.oryno.piggy_ledger.data.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.Context
import java.util.UUID
import com.posthog.PostHog

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.oryno.piggy_ledger.data.BackupData

class PiggyLedgerViewModel(
    private val repository: PiggyLedgerRepository,
    private val userPreferences: UserPreferences,
    private val context: Context
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    fun exportData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val streakDates = com.oryno.piggy_ledger.data.StreakManager.getActionDates(context)
            val backup = repository.getFullBackup(streakDates)
            val jsonString = json.encodeToString(backup)
            PostHog.capture(
                event = "data_exported",
                properties = mapOf(
                    "total_goals" to backup.goals.size,
                    "total_loans" to backup.loans.size,
                    "total_transactions" to backup.transactions.size
                )
            )
            onResult(jsonString)
        }
    }

    fun importData(jsonString: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = json.decodeFromString<BackupData>(jsonString)
                repository.restoreBackup(data)
                com.oryno.piggy_ledger.data.StreakManager.setActionDates(context, data.streakDates)
                com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
                com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
                com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
                PostHog.capture(
                    event = "data_imported",
                    properties = mapOf(
                        "success" to true,
                        "total_goals" to data.goals.size,
                        "total_loans" to data.loans.size,
                        "total_transactions" to data.transactions.size
                    )
                )
                onComplete()
            } catch (e: Exception) {
                PostHog.capture(
                    event = "data_imported",
                    properties = mapOf(
                        "success" to false,
                        "error" to (e.message ?: "Unknown error")
                    )
                )
                onError(e.message ?: "Unknown error during import")
            }
        }
    }


    val hasOnboarded = userPreferences.hasOnboarded.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    
    val hasLanguageSelected = userPreferences.hasLanguageSelected.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val isAuthenticated = userPreferences.isAuthenticated.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val authUserEmail = userPreferences.authUserEmail.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val authUserName = userPreferences.authUserName.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val authUserPhotoUrl = userPreferences.authUserPhotoUrl.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val isBiometricLockEnabled = userPreferences.isBiometricLockEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val lockTimeoutSeconds = userPreferences.lockTimeoutSeconds.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
    )

    val isScreenshotProtectionEnabled = userPreferences.isScreenshotProtectionEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val pinLock = userPreferences.pinLock.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    fun signInWithGoogle(email: String, name: String, photoUrl: String) {
        viewModelScope.launch {
            userPreferences.saveAuthentication(true, email, name, photoUrl)
            PostHog.identify(email, mapOf("name" to name))
            PostHog.capture(
                event = "user_sign_in",
                properties = mapOf("email" to email, "name" to name, "method" to "google")
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userPreferences.saveAuthentication(false, "", "", "")
            PostHog.capture("user_sign_out")
            PostHog.reset()
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.saveBiometricLockEnabled(enabled)
            PostHog.capture(
                event = "security_setting_changed",
                properties = mapOf("setting" to "biometric_lock", "enabled" to enabled)
            )
        }
    }

    fun setLockTimeout(seconds: Long) {
        viewModelScope.launch {
            userPreferences.saveLockTimeout(seconds)
            PostHog.capture(
                event = "security_setting_changed",
                properties = mapOf("setting" to "lock_timeout", "timeout_seconds" to seconds)
            )
        }
    }

    fun setScreenshotProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.saveScreenshotProtectionEnabled(enabled)
            PostHog.capture(
                event = "security_setting_changed",
                properties = mapOf("setting" to "screenshot_protection", "enabled" to enabled)
            )
        }
    }

    fun setPinLock(pin: String?) {
        viewModelScope.launch {
            userPreferences.savePinLock(pin)
            PostHog.capture(
                event = "security_setting_changed",
                properties = mapOf("setting" to "pin_lock", "enabled" to (pin != null))
            )
        }
    }

    val goals: StateFlow<List<Goal>> = repository.allGoals.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )
    
    val loans: StateFlow<List<Loan>> = repository.allLoans.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val allAccounts: StateFlow<List<com.oryno.piggy_ledger.data.Account>> = repository.allAccounts.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val includedAccounts: StateFlow<List<com.oryno.piggy_ledger.data.Account>> = repository.includedAccounts.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    fun getDomainForBank(bankName: String): String {
        val clean = bankName.trim().lowercase()
        return when {
            clean.contains("cib") || clean.contains("commercial international bank") -> "cibeg.com"
            clean.contains("qnb") -> "qnbalahli.com"
            clean.contains("misr") || clean.contains("banque misr") -> "banquemisr.com"
            clean.contains("nbe") || clean.contains("ahly") || clean.contains("national bank") -> "nbe.com.eg"
            clean.contains("hsbc") -> "hsbc.com"
            clean.contains("alex") || clean.contains("alexandria") -> "alexbank.com"
            clean.contains("arab bank") -> "arabbank.com.eg"
            clean.contains("faisal") -> "faisalbank.com.eg"
            clean.contains("adib") || clean.contains("abu dhabi islamic") -> "adib.eg"
            clean.contains("audi") -> "bankaudi.com.eg"
            clean.contains("eg bank") || clean.contains("egyptian gulf") -> "eg-bank.com"
            clean.contains("saib") -> "saib.com.eg"
            clean.contains("barclays") -> "barclays.co.uk"
            clean.contains("citi") -> "citibank.com"
            clean.contains("chase") -> "chase.com"
            clean.contains("bofa") || clean.contains("america") -> "bankofamerica.com"
            clean.contains("wells") -> "wellsfargo.com"
            clean.contains("scb") || clean.contains("standard chartered") -> "sc.com"
            clean.contains("db") || clean.contains("deutsche") -> "db.com"
            clean.contains("fnb") -> "fnb.co.za"
            clean.contains("standard") -> "standardbank.co.za"
            clean.contains("nedbank") -> "nedbank.co.za"
            clean.contains("absa") -> "absa.co.za"
            clean.contains("vodafone") -> "vodafone.com.eg"
            clean.contains("orange") -> "orange.eg"
            clean.contains("etisalat") || clean.contains("e&") -> "etisalat.eg"
            clean.contains("we pay") || clean.contains("telecom egypt") -> "te.eg"
            clean.contains("telda") -> "telda.app"
            clean.contains("klivvr") -> "klivvr.com"
            clean.contains("instapay") -> "instapay.eg"
            clean.contains(".") -> bankName.trim()
            else -> "$clean.com"
        }
    }

    private suspend fun downloadAndSaveLogo(context: Context, domain: String): String? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val logoUrlSpec = "https://www.google.com/s2/favicons?sz=128&domain=$domain"
                val url = java.net.URL(logoUrlSpec)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                
                if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val directory = java.io.File(context.filesDir, "bank_logos")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val filename = "logo_${domain.replace(".", "_")}.png"
                    val file = java.io.File(directory, filename)
                    
                    connection.inputStream.use { input ->
                        java.io.FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    file.absolutePath
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun addAccount(account: com.oryno.piggy_ledger.data.Account) {
        viewModelScope.launch {
            PostHog.capture(
                event = "account_created",
                properties = mapOf(
                    "account_name" to account.name,
                    "account_type" to account.type.name,
                    "provider" to (account.provider ?: ""),
                    "currency" to account.currency,
                    "initial_balance" to account.current_balance
                )
            )
            if (account.type == com.oryno.piggy_ledger.data.AccountType.BANK || 
                account.type == com.oryno.piggy_ledger.data.AccountType.CARD ||
                account.type == com.oryno.piggy_ledger.data.AccountType.WALLET) {
                
                val providerOrName = account.provider?.takeIf { it.isNotBlank() } ?: account.name
                val domain = getDomainForBank(providerOrName)
                val logoUrl = "https://www.google.com/s2/favicons?sz=128&domain=$domain"
                val initialAccount = account.copy(logo_url = logoUrl)
                val rowId = repository.insertAccount(initialAccount)
                
                // Fetch and save logo once online in background
                viewModelScope.launch {
                    val localPath = downloadAndSaveLogo(context, domain)
                    if (localPath != null) {
                        val acc = repository.getAccountById(rowId)
                        if (acc != null) {
                            repository.updateAccount(acc.copy(local_logo_path = localPath))
                        }
                    }
                }
            } else {
                repository.insertAccount(account)
            }
        }
    }

    fun updateAccount(account: com.oryno.piggy_ledger.data.Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
            PostHog.capture(
                event = "account_updated",
                properties = mapOf(
                    "account_name" to account.name,
                    "account_type" to account.type.name,
                    "provider" to (account.provider ?: ""),
                    "currency" to account.currency
                )
            )
        }
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            repository.deleteAccount(id)
            PostHog.capture(
                event = "account_deleted",
                properties = mapOf("account_id" to id)
            )
        }
    }

    val overdueLoans: StateFlow<List<Loan>> = loans.map { list ->
        val now = System.currentTimeMillis()
        list.filter { !it.isPaidOff && it.deadline != null && it.deadline < now }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            delay(5000)
            while (true) {
                val currentOverdue = overdueLoans.value
                currentOverdue.forEach {
                    NotificationHelper(context).showDeadlineNotification(it.contactName, it.amount)
                }
                delay(60000 * 60) // Check every hour
            }
        }
    }

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val allAccountTransactions: StateFlow<List<com.oryno.piggy_ledger.data.AccountTransaction>> = repository.getAllAccountTransactions().stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId: StateFlow<Long?> = _selectedAccountId

    fun selectAccount(accountId: Long?) {
        _selectedAccountId.value = accountId
        PostHog.capture(
            event = "account_selected",
            properties = mapOf("account_id" to (accountId ?: -1))
        )
    }

    private val _monthlyBudget = MutableStateFlow<Double>(10000.0)
    val monthlyBudget: StateFlow<Double> = _monthlyBudget

    fun setMonthlyBudget(budget: Double) {
        _monthlyBudget.value = budget
        PostHog.capture(
            event = "budget_updated",
            properties = mapOf("budget_amount" to budget)
        )
    }

    fun addAccountTransaction(accountId: Long, amount: Double, merchant: String, source: String = "MANUAL", timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertAccountTransaction(
                com.oryno.piggy_ledger.data.AccountTransaction(
                    account_id = accountId,
                    amount = amount,
                    merchant = merchant,
                    source = source,
                    timestamp = timestamp
                )
            )
            PostHog.capture(
                event = "account_transaction_added",
                properties = mapOf(
                    "account_id" to accountId,
                    "amount" to amount,
                    "merchant" to merchant,
                    "source" to source
                )
            )
            val account = repository.getAccountById(accountId)
            if (account != null) {
                val newBalance = account.current_balance + amount
                val newAvailable = if (account.type == com.oryno.piggy_ledger.data.AccountType.CARD && account.available_credit != null) {
                    account.available_credit + amount
                } else {
                    account.available_credit
                }
                repository.updateAccount(account.copy(current_balance = newBalance, available_credit = newAvailable))
            }
        }
    }

    fun getTransactionsForGoal(goalId: String): Flow<List<Transaction>> {
        return allTransactions.map { list -> list.filter { it.goalId == goalId } }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferences.saveOnboarding(true)
            PostHog.capture("onboarding_completed")
        }
    }
    
    fun completeLanguageSelection() {
        viewModelScope.launch {
            userPreferences.saveLanguageSelected(true)
            PostHog.capture("language_selection_completed")
        }
    }

    fun addGoal(name: String, targetAmount: Double) {
        viewModelScope.launch {
            repository.insertGoal(
                Goal(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    targetAmount = targetAmount
                )
            )
            PostHog.capture(
                event = "goal_created",
                properties = mapOf("goal_name" to name, "target_amount" to targetAmount)
            )
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }

    fun addTransaction(goalId: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    goalId = goalId,
                    amount = amount,
                    note = note
                )
            )
            PostHog.capture(
                event = "goal_transaction_added",
                properties = mapOf("goal_id" to goalId, "amount" to amount, "note" to note)
            )
            com.oryno.piggy_ledger.data.StreakManager.recordAction(context)
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }

    fun addLoan(loan: Loan) {
        viewModelScope.launch {
            repository.insertLoan(loan)
            PostHog.capture(
                event = "loan_created",
                properties = mapOf(
                    "contact_name" to loan.contactName,
                    "amount" to loan.amount,
                    "type" to loan.type.name
                )
            )
            com.oryno.piggy_ledger.data.StreakManager.recordAction(context)
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }

    fun markLoanAsPaid(id: String) {
        viewModelScope.launch {
            repository.markLoanAsPaid(id)
            PostHog.capture(
                event = "loan_paid",
                properties = mapOf("loan_id" to id)
            )
            com.oryno.piggy_ledger.data.StreakManager.recordAction(context)
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }
    
    fun deleteLoan(id: String) {
        viewModelScope.launch {
            repository.deleteLoan(id)
            PostHog.capture(
                event = "loan_deleted",
                properties = mapOf("loan_id" to id)
            )
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            repository.deleteGoal(id)
            PostHog.capture(
                event = "goal_deleted",
                properties = mapOf("goal_id" to id)
            )
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }

    val allPendingTransactions: StateFlow<List<com.oryno.piggy_ledger.data.PendingTransaction>> = repository.allPendingTransactions.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    fun resolvePendingTransaction(pendingId: Long, accountId: Long) {
        viewModelScope.launch {
            repository.resolvePendingTransaction(pendingId, accountId)
            PostHog.capture(
                event = "pending_transaction_resolved",
                properties = mapOf("pending_id" to pendingId, "account_id" to accountId)
            )
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }

    fun deletePendingTransaction(pendingId: Long) {
        viewModelScope.launch {
            repository.deletePendingTransaction(pendingId)
            PostHog.capture(
                event = "pending_transaction_deleted",
                properties = mapOf("pending_id" to pendingId)
            )
        }
    }
}
