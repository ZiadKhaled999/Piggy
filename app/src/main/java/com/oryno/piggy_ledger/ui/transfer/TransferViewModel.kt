package com.oryno.piggy_ledger.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.JournalEntryEntity
import com.oryno.piggy_ledger.data.JournalType
import com.oryno.piggy_ledger.data.PiggyLedgerDao
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.domain.usecase.CreateTransferUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransferUiState(
    val accounts: List<Account> = emptyList(),
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    val amountText: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val recentTransfers: List<JournalEntryEntity> = emptyList()
)

class TransferViewModel(
    private val repository: PiggyLedgerRepository,
    private val dao: PiggyLedgerDao
) : ViewModel() {

    private val createTransferUseCase = CreateTransferUseCase(dao)

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = combine(
        _uiState,
        repository.allAccounts,
        repository.allJournalEntries
    ) { state, allAccounts, allJournalEntries ->
        val userAccounts = allAccounts.filter { !it.isSystemAccount && !it.is_deleted }
        val transfers = allJournalEntries.filter { it.type == JournalType.TRANSFER && !it.is_deleted }

        val defaultFrom = state.fromAccountId ?: userAccounts.firstOrNull()?.id
        val defaultTo = state.toAccountId ?: userAccounts.getOrNull(1)?.id ?: defaultFrom

        state.copy(
            accounts = userAccounts,
            fromAccountId = defaultFrom,
            toAccountId = defaultTo,
            recentTransfers = transfers
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransferUiState()
    )

    fun selectFromAccount(id: String) {
        _uiState.update { it.copy(fromAccountId = id, errorMessage = null) }
    }

    fun selectToAccount(id: String) {
        _uiState.update { it.copy(toAccountId = id, errorMessage = null) }
    }

    fun swapAccounts() {
        _uiState.update {
            val oldFrom = it.fromAccountId
            val oldTo = it.toAccountId
            it.copy(fromAccountId = oldTo, toAccountId = oldFrom, errorMessage = null)
        }
    }

    fun updateAmount(value: String) {
        if (value.isBlank() || value.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
            _uiState.update { it.copy(amountText = value, errorMessage = null) }
        }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun executeTransfer(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val fromId = currentState.fromAccountId
        val toId = currentState.toAccountId
        val amount = currentState.amountText.toDoubleOrNull() ?: 0.0

        if (fromId == null || toId == null) {
            _uiState.update { it.copy(errorMessage = "Please select source and destination accounts.") }
            return
        }
        if (amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid transfer amount greater than 0.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            val result = createTransferUseCase(
                fromAccountId = fromId,
                toAccountId = toId,
                amount = amount,
                description = currentState.description
            )

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        amountText = "",
                        description = "",
                        successMessage = "Transfer completed successfully!"
                    )
                }
                onSuccess()
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = ex.message ?: "Transfer failed. Please check account balances."
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
