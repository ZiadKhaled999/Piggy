package com.oryno.piggy_ledger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oryno.piggy_ledger.data.PiggyLedgerRepository
import com.oryno.piggy_ledger.data.UserPreferences

import android.content.Context

class ViewModelFactory(
    private val repository: PiggyLedgerRepository,
    private val userPreferences: UserPreferences,
    private val context: Context,
    private val database: com.oryno.piggy_ledger.data.PiggyLedgerDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PiggyLedgerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PiggyLedgerViewModel(repository, userPreferences, context) as T
        }
        if (modelClass.isAssignableFrom(com.oryno.piggy_ledger.ai.AiChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val chatRepo = com.oryno.piggy_ledger.ai.AiChatRepository(database.piggyLedgerDao())
            return com.oryno.piggy_ledger.ai.AiChatViewModel(chatRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
