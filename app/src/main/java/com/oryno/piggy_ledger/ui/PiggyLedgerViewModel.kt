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
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error during import")
            }
        }
    }

    init {
        viewModelScope.launch {
            delay(5000)
            while (true) {
                val currentLoans = loans.value
                val now = System.currentTimeMillis()
                currentLoans.filter { !it.isPaidOff && it.deadline != null && it.deadline < now }.forEach {
                    NotificationHelper(context).showDeadlineNotification(it.contactName, it.amount)
                }
                delay(60000 * 60) // Check every hour
            }
        }
    }

    val hasOnboarded = userPreferences.hasOnboarded.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val goals: StateFlow<List<Goal>> = repository.allGoals.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )
    
    val loans: StateFlow<List<Loan>> = repository.allLoans.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    fun getTransactionsForGoal(goalId: String): Flow<List<Transaction>> {
        return allTransactions.map { list -> list.filter { it.goalId == goalId } }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferences.saveOnboarding(true)
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
            com.oryno.piggy_ledger.data.StreakManager.recordAction(context)
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }

    fun addLoan(loan: Loan) {
        viewModelScope.launch {
            repository.insertLoan(loan)
            com.oryno.piggy_ledger.data.StreakManager.recordAction(context)
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }

    fun markLoanAsPaid(id: String) {
        viewModelScope.launch {
            repository.markLoanAsPaid(id)
            com.oryno.piggy_ledger.data.StreakManager.recordAction(context)
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }
    
    fun deleteLoan(id: String) {
        viewModelScope.launch {
            repository.deleteLoan(id)
            com.oryno.piggy_ledger.widget.SummaryWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.StreakWidgetProvider.triggerUpdate(context)
            com.oryno.piggy_ledger.widget.GoalsWidgetProvider.triggerUpdate(context)
        }
    }
}
