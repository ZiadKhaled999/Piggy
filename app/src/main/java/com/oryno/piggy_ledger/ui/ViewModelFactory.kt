package com.oryno.piggy_ledger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.data.UserPreferences

import android.content.Context

class ViewModelFactory(
    private val repository: PiggyLedgerRepository,
    private val userPreferences: UserPreferences,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PiggyLedgerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PiggyLedgerViewModel(repository, userPreferences, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
