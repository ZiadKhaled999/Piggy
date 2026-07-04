package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Goal
import com.example.data.Loan
import com.example.data.PiggyLedgerRepository
import com.example.data.Transaction
import com.example.data.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.Context
import java.util.UUID

class PiggyLedgerViewModel(
    private val repository: PiggyLedgerRepository,
    private val userPreferences: UserPreferences,
    private val context: Context
) : ViewModel() {

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
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    
    val loans: StateFlow<List<Loan>> = repository.allLoans.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun getTransactionsForGoal(goalId: String): StateFlow<List<Transaction>> {
        return repository.getTransactionsForGoal(goalId).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
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
        }
    }

    fun addLoan(loan: Loan) {
        viewModelScope.launch {
            repository.insertLoan(loan)
        }
    }

    fun markLoanAsPaid(id: String) {
        viewModelScope.launch {
            repository.markLoanAsPaid(id)
        }
    }
    
    fun deleteLoan(id: String) {
        viewModelScope.launch {
            repository.deleteLoan(id)
        }
    }
}
