
package com.oryno.piggy_ledger.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountTransaction
import com.oryno.piggy_ledger.data.BackupData
import com.oryno.piggy_ledger.data.Goal
import com.oryno.piggy_ledger.data.Loan
import com.oryno.piggy_ledger.data.LoanPayment
import com.oryno.piggy_ledger.data.PendingTransaction
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.data.StreakManager
import com.oryno.piggy_ledger.data.Transaction
import com.oryno.piggy_ledger.data.UserPreferences
import com.posthog.PostHog
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class LogoutState {
    object Idle : LogoutState()
    object Syncing : LogoutState()
    object Success : LogoutState()
    data class OfflineError(val unsyncedCount: Int) : LogoutState()
    data class Error(val message: String) : LogoutState()
}

class PiggyLedgerViewModel(
    private val repository: PiggyLedgerRepository,
    private val userPreferences: UserPreferences,
    private val context: Context
) : ViewModel() {

    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _streakCount = MutableStateFlow<Int>(0)
    val streakCount: StateFlow<Int> = _streakCount.asStateFlow()

    fun refreshStreak() {
        viewModelScope.launch {
            _streakCount.value = StreakManager.getStreak(context)
        }
    }

    private fun recordStreakAction() {
        StreakManager.recordAction(context)
        refreshStreak()
    }

    init {
        refreshStreak()
        viewModelScope.launch {
            var hasSyncedOnStart = false
            com.clerk.api.Clerk.sessionFlow.collect { session ->
                if (session != null && !hasSyncedOnStart) {
                    hasSyncedOnStart = true
                    triggerCloudSync()
                }
            }
        }
        viewModelScope.launch {
            userPreferences.isPrivacyModeEnabled.collect { savedState ->
                _isPrivacyModeEnabled.value = savedState
            }
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.oryno.piggy_ledger.service.SyncManager(context).syncAll()
            } catch (e: Exception) {
                android.util.Log.e("PiggyLedgerVM", "Cloud sync failed", e)
            }
        }
    }

    val customIdentifiers: StateFlow<Map<String, List<String>>> = userPreferences.customIdentifiersJson.map { jsonStr ->
        try {
            json.decodeFromString<Map<String, List<String>>>(jsonStr)
        } catch (e: Exception) {
            emptyMap()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addCustomIdentifierKeywords(providerName: String, keywords: List<String>, onComplete: () -> Unit) {
        viewModelScope.launch {
            val currentMap = customIdentifiers.value.toMutableMap()
            val existingKeywords = currentMap[providerName]?.toMutableList() ?: mutableListOf()
            keywords.forEach { kw ->
                val trimmed = kw.trim()
                if (trimmed.isNotEmpty() && !existingKeywords.contains(trimmed)) {
                    existingKeywords.add(trimmed)
                }
            }
            currentMap[providerName] = existingKeywords
            val jsonString = json.encodeToString(currentMap)
            userPreferences.saveCustomIdentifiersJson(jsonString)

            try {
                PostHog.capture(
                    event = "custom_identifier_added",
                    properties = mapOf(
                        "provider" to providerName,
                        "added_keywords" to keywords.joinToString(", "),
                        "total_keywords_for_provider" to existingKeywords.size
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("PostHog", "Failed to capture custom identifier added", e)
            }

            // Upload identifier keywords to central repository post endpoint
            uploadIdentifierKeywordsToPost(providerName, existingKeywords)

            onComplete()
        }
    }

    private fun uploadIdentifierKeywordsToPost(providerName: String, keywords: List<String>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.piggyledger.com/v1/community-identifiers")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val payload = """{"provider":"$providerName","keywords":${json.encodeToString(keywords)}}"""
                conn.outputStream.use { os ->
                    os.write(payload.toByteArray(Charsets.UTF_8))
                }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                // Silently handle offline/mock server exception
            }
        }
    }

    fun exportData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val streakDates = com.oryno.piggy_ledger.data.StreakManager.getActionDates(context)
            val backup = repository.getFullBackup(streakDates)
            val jsonString = json.encodeToString(backup)
            try {
                PostHog.capture(
                    event = "data_exported",
                    properties = mapOf(
                        "total_goals" to backup.goals.size,
                        "total_loans" to backup.loans.size,
                        "total_transactions" to backup.transactions.size
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("PostHog", "Failed to capture data exported", e)
            }
            onResult(jsonString)
        }
    }

    fun exportCSVData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val streakDates = com.oryno.piggy_ledger.data.StreakManager.getActionDates(context)
            val backup = repository.getFullDatabaseBackup(streakDates)
            val csvString = com.oryno.piggy_ledger.data.BackupHelper.generateFullCsv(
                goals = backup.goals,
                transactions = backup.transactions,
                loans = backup.loans,
                loanPayments = backup.loanPayments,
                accounts = backup.accounts,
                accountTransactions = backup.accountTransactions,
                pendingTransactions = backup.pendingTransactions,
                streakDates = backup.streakDates
            )
            try {
                PostHog.capture(
                    event = "csv_data_exported",
                    properties = mapOf(
                        "total_goals" to backup.goals.size,
                        "total_loans" to backup.loans.size,
                        "total_transactions" to backup.transactions.size,
                        "total_accounts" to backup.accounts.size,
                        "total_account_transactions" to backup.accountTransactions.size,
                        "total_pending_transactions" to backup.pendingTransactions.size
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("PostHog", "Failed to capture csv data exported", e)
            }
            onResult(csvString)
        }
    }

    fun exportExcelData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val streakDates = com.oryno.piggy_ledger.data.StreakManager.getActionDates(context)
            val backup = repository.getFullDatabaseBackup(streakDates)
            val excelString = com.oryno.piggy_ledger.data.BackupHelper.generateBeautifulExcel(
                goals = backup.goals,
                transactions = backup.transactions,
                loans = backup.loans,
                loanPayments = backup.loanPayments,
                accounts = backup.accounts,
                accountTransactions = backup.accountTransactions,
                pendingTransactions = backup.pendingTransactions,
                streakDates = backup.streakDates
            )
            try {
                PostHog.capture(
                    event = "excel_data_exported",
                    properties = mapOf(
                        "total_goals" to backup.goals.size,
                        "total_loans" to backup.loans.size,
                        "total_transactions" to backup.transactions.size,
                        "total_accounts" to backup.accounts.size,
                        "total_account_transactions" to backup.accountTransactions.size,
                        "total_pending_transactions" to backup.pendingTransactions.size
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("PostHog", "Failed to capture excel data exported", e)
            }
            onResult(excelString)
        }
    }

    val appCurrency = userPreferences.appCurrency.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "USD"
    )

    val hasOnboarded = userPreferences.hasOnboarded.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    
    val hasLanguageSelected = userPreferences.hasLanguageSelected.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    val hasHeardAboutUs = userPreferences.hasHeardAboutUs.stateIn(
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

    val isPremium = userPreferences.isPremium.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val aiMessagesCount = userPreferences.aiMessagesCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    fun setPremiumStatus(isPremium: Boolean) {
        viewModelScope.launch {
            userPreferences.savePremiumStatus(isPremium)
            try {
                PostHog.capture(
                    event = "premium_status_changed",
                    properties = mapOf("is_premium" to isPremium)
                )
            } catch (e: Exception) {
                // Ignore analytics error
            }
        }
    }

    init {
        checkRevenueCatPremiumStatus()
    }

    fun checkRevenueCatPremiumStatus() {
        try {
            if (com.revenuecat.purchases.Purchases.isConfigured) {
                com.revenuecat.purchases.Purchases.sharedInstance.getCustomerInfo(
                    object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                        override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo) {
                            val isProActive = customerInfo.entitlements["Piggy Ledger Pro"]?.isActive == true || customerInfo.entitlements.all.values.any { it.isActive }
                            viewModelScope.launch {
                                userPreferences.savePremiumStatus(isProActive)
                            }
                        }
                        override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                            // Keep current cached status
                        }
                    }
                )
            }
        } catch (e: Exception) {
            // RevenueCat not initialized or billing unavailable
        }
    }

    fun setAuthUser(email: String, name: String, photoUrl: String, clerkUserId: String = "") {
        viewModelScope.launch {
            userPreferences.saveAuthentication(true, email, name, photoUrl)
            PostHog.identify(email, mapOf("name" to name))
            PostHog.capture(
                event = "user_sign_in",
                properties = mapOf("email" to email, "name" to name, "method" to "clerk")
            )
            com.oryno.piggy_ledger.ui.NotificationHelper(context).showAuthNotification(true)

            if (clerkUserId.isNotBlank()) {
                try {
                    if (com.revenuecat.purchases.Purchases.isConfigured) {
                        com.revenuecat.purchases.Purchases.sharedInstance.logIn(
                            clerkUserId,
                            object : com.revenuecat.purchases.interfaces.LogInCallback {
                                override fun onReceived(
                                    customerInfo: com.revenuecat.purchases.CustomerInfo,
                                    created: Boolean
                                ) {
                                    val isProActive = customerInfo.entitlements["Piggy Ledger Pro"]?.isActive == true || customerInfo.entitlements.all.values.any { it.isActive }
                                    setPremiumStatus(isProActive)
                                }
                                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                                    android.util.Log.w("PiggyLedgerVM", "RevenueCat logIn warning: ${error.message}")
                                }
                            }
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PiggyLedgerVM", "RevenueCat logIn exception: ${e.message}")
                }
            }
        }
    }

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    fun resetLogoutState() {
        _logoutState.value = LogoutState.Idle
    }

    fun performSyncAndLogout(forceDeleteIfOffline: Boolean = false, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _logoutState.value = LogoutState.Syncing
            try {
                val initialUnsyncedCount = repository.getPendingUploadCount()
                val networkAvailable = isNetworkAvailable(context)

                if (initialUnsyncedCount > 0 && !networkAvailable && !forceDeleteIfOffline) {
                    _logoutState.value = LogoutState.OfflineError(initialUnsyncedCount)
                    return@launch
                }

                if (initialUnsyncedCount > 0 && networkAvailable) {
                    val ok = com.oryno.piggy_ledger.service.SyncManager(context).syncAll()
                    val remainingCount = repository.getPendingUploadCount()
                    
                    if ((!ok || remainingCount > 0) && !forceDeleteIfOffline) {
                        _logoutState.value = LogoutState.Error("Upload failed — $remainingCount items kept on device")
                        return@launch
                    }
                }

                wipeLocalData()

                try {
                    com.clerk.api.Clerk.auth.signOut()
                } catch (e: Exception) {
                    android.util.Log.e("PiggyLedgerViewModel", "Clerk sign out failed", e)
                }
                
                try {
                    if (com.revenuecat.purchases.Purchases.isConfigured) {
                        com.revenuecat.purchases.Purchases.sharedInstance.logOut(
                            object : com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback {
                                override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo) {
                                    setPremiumStatus(false)
                                }
                                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                                    android.util.Log.e("PiggyLedgerViewModel", "RevenueCat logOut error: ${error.message}")
                                }
                            }
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PiggyLedgerViewModel", "RevenueCat logOut failed", e)
                }
                userPreferences.saveAuthentication(false, "", "", "")
                PostHog.capture("user_sign_out")
                PostHog.reset()
                com.oryno.piggy_ledger.ui.NotificationHelper(context).showAuthNotification(false)

                _logoutState.value = LogoutState.Success
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("PiggyLedgerViewModel", "Perform sync and logout failed", e)
                _logoutState.value = LogoutState.Error(e.localizedMessage ?: "Logout failed")
            }
        }
    }

    private suspend fun wipeLocalData() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            PiggyLedgerDatabase.getInstance(context.applicationContext).clearAllTables()
        } catch (e: Exception) {
            android.util.Log.e("PiggyLedgerViewModel", "Clear database tables failed", e)
        }

        try {
            userPreferences.clearAll()
        } catch (e: Exception) {
            android.util.Log.e("PiggyLedgerViewModel", "Clear UserPreferences failed", e)
        }

        try {
            context.cacheDir?.deleteRecursively()
        } catch (e: Exception) {
            android.util.Log.e("PiggyLedgerViewModel", "Clear cache failed", e)
        }

        try {
            StreakManager.clear(context)
        } catch (e: Exception) {
            android.util.Log.e("PiggyLedgerViewModel", "Clear streak manager failed", e)
        }

        try {
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        } catch (e: Exception) {
            // Widget update fail silent
        }
    }

    private fun isNetworkAvailable(context: android.content.Context): Boolean {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun signOut() {
        performSyncAndLogout(forceDeleteIfOffline = true)
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

    val goals: StateFlow<List<Goal>> = repository.allGoals.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )
    
    val loans: StateFlow<List<Loan>> = repository.allLoans.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val allLoanPayments: StateFlow<List<com.oryno.piggy_ledger.data.LoanPayment>> = repository.allLoanPayments.stateIn(
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
                repository.insertAccount(initialAccount)
                val rowId = initialAccount.id
                
                recordStreakAction()
                com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
                com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
                
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
                recordStreakAction()
                com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
                com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
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

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            repository.deleteAccount(id)
            if (_selectedAccountId.value == id) {
                selectAccount(null)
            }
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
            userPreferences.preferredAccountId.collect { prefAccId ->
                if (_selectedAccountId.value != prefAccId) {
                    _selectedAccountId.value = prefAccId
                }
            }
        }
        viewModelScope.launch {
            delay(5000)
            while (true) {
                try {
                    val currentOverdue = overdueLoans.value
                    currentOverdue.forEach {
                        try {
                            NotificationHelper(context).showDeadlineNotification(it.contactName, it.amount)
                        } catch (e: Throwable) {
                            android.util.Log.e("PiggyLedgerVM", "Failed to show deadline notification", e)
                        }
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("PiggyLedgerVM", "Error in overdue notifications loop", e)
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

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId: StateFlow<String?> = _selectedAccountId

    fun selectAccount(accountId: String?) {
        _selectedAccountId.value = accountId
        viewModelScope.launch {
            userPreferences.savePreferredAccountId(accountId)
        }
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

    fun addAccountTransaction(accountId: String, amount: Double, merchant: String, source: String = "MANUAL", timestamp: Long = System.currentTimeMillis()) {
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
            recordStreakAction()
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
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

    fun setAppCurrency(currencyCode: String) {
        viewModelScope.launch {
            userPreferences.saveAppCurrency(currencyCode)
        }
    }

    fun completeOnboarding(
        intent: Int,
        intensity: Int,
        savingMode: String,
        relatesToLoans: Boolean? = null,
        relatesToAccounts: Boolean? = null,
        relatesToEmergency: Boolean? = null
    ) {
        viewModelScope.launch {
            userPreferences.saveOnboarding(true)
            userPreferences.savePersonalization(intent, intensity, savingMode)
            
            val answersMap = mutableMapOf(
                "personalized_intent" to intent.toString(),
                "personalized_intensity" to intensity.toString(),
                "saving_mode" to savingMode,
                "completed_at" to System.currentTimeMillis().toString()
            )
            relatesToLoans?.let { answersMap["relates_to_loans"] = it.toString() }
            relatesToAccounts?.let { answersMap["relates_to_accounts"] = it.toString() }
            relatesToEmergency?.let { answersMap["relates_to_emergency"] = it.toString() }

            repository.saveOnboardingAnswers(answersMap)

            PostHog.capture(
                event = "onboarding_completed",
                properties = mapOf(
                    "personalized_intent" to intent,
                    "personalized_intensity" to intensity,
                    "saving_mode" to savingMode
                )
            )
        }
    }
    
    fun completeLanguageSelection() {
        viewModelScope.launch {
            userPreferences.saveLanguageSelected(true)
            val currentLang = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
            repository.saveOnboardingAnswer("language", currentLang)
            PostHog.capture("language_selection_completed", properties = mapOf("\$set" to mapOf("language" to currentLang)))
        }
    }

    fun completeHearAboutUs(source: String) {
        viewModelScope.launch {
            userPreferences.saveHeardAboutUs(true)
            repository.saveOnboardingAnswer("hear_about_us_source", source)
            PostHog.capture("hear_about_us_answered", properties = mapOf("source" to source, "\$set" to mapOf("hear_about_us_source" to source)))
        }
    }

    fun resetAppFlow() {
        viewModelScope.launch {
            userPreferences.saveOnboarding(false)
            userPreferences.saveLanguageSelected(false)
            userPreferences.saveAuthentication(false)
            PostHog.capture("app_flow_reset")
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
                properties = mapOf("goal_name" to name, "target_amount" to targetAmount, "goal_type" to "general", "deadline" to "none")
            )
            recordStreakAction()
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
            val allTx = repository.getTransactionsForGoal(goalId).first()
            val runningTotal = allTx.sumOf { it.amount }
            PostHog.capture(
                event = "goal_transaction_added",
                properties = mapOf("goal_id" to goalId, "amount" to amount, "note" to note, "running_total" to runningTotal)
            )
            recordStreakAction()
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
            recordStreakAction()
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }

    fun getPaymentsForLoan(loanId: String): Flow<List<com.oryno.piggy_ledger.data.LoanPayment>> {
        return repository.getPaymentsForLoan(loanId)
    }

    fun addLoanPayment(loanId: String, amount: Double, note: String?) {
        viewModelScope.launch {
            repository.insertLoanPayment(
                com.oryno.piggy_ledger.data.LoanPayment(
                    loanId = loanId,
                    amount = amount,
                    timestamp = System.currentTimeMillis(),
                    note = note
                )
            )
            PostHog.capture(
                event = "loan_payment_added",
                properties = mapOf<String, Any>("loan_id" to loanId, "amount" to amount, "note" to (note ?: ""))
            )
            recordStreakAction()
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }

    fun deleteLoanPayment(id: String) {
        viewModelScope.launch {
            repository.deleteLoanPayment(id)
            PostHog.capture(
                event = "loan_payment_deleted",
                properties = mapOf("payment_id" to id)
            )
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
            recordStreakAction()
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

    fun resolvePendingTransaction(pendingId: String, accountId: String) {
        viewModelScope.launch {
            repository.resolvePendingTransaction(pendingId, accountId)
            PostHog.capture(
                event = "pending_transaction_resolved",
                properties = mapOf("pending_id" to pendingId, "account_id" to accountId)
            )
            recordStreakAction()
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }

    fun deletePendingTransaction(pendingId: String) {
        viewModelScope.launch {
            repository.deletePendingTransaction(pendingId)
            PostHog.capture(
                event = "pending_transaction_deleted",
                properties = mapOf("pending_id" to pendingId)
            )
        }
    }

    fun canAddAccount(currentCount: Int): Boolean = isPremium.value || currentCount < 2
    fun canAddBudget(): Boolean = isPremium.value
    fun canAddGoal(currentCount: Int): Boolean = isPremium.value || currentCount < 2
    fun canAddLoan(currentCount: Int): Boolean = isPremium.value || currentCount < 2
    fun canAccessFullAnalytics(): Boolean = isPremium.value
    fun canExportData(): Boolean = isPremium.value
    fun canUseCustomCategories(): Boolean = isPremium.value
    fun canSendAiMessage(): Boolean = isPremium.value || (aiMessagesCount.value < 3)

    private val _isPrivacyModeEnabled = MutableStateFlow<Boolean>(false)
    val isPrivacyModeEnabled: StateFlow<Boolean> = _isPrivacyModeEnabled.asStateFlow()

    fun togglePrivacyMode(context: Context) {
        if (!_isPrivacyModeEnabled.value) {
            _isPrivacyModeEnabled.value = true
            viewModelScope.launch {
                userPreferences.savePrivacyModeEnabled(true)
            }
        } else {
            BiometricHelper.authenticateToUnhide(
                context = context,
                onSuccess = {
                    _isPrivacyModeEnabled.value = false
                    viewModelScope.launch {
                        userPreferences.savePrivacyModeEnabled(false)
                    }
                },
                onError = { err ->
                    android.widget.Toast.makeText(
                        context,
                        err.ifBlank { "Authentication required to unhide numbers" },
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}
