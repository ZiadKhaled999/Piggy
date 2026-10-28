package com.oryno.piggy_ledger.domain.usecase

import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.data.JournalEntryEntity
import com.oryno.piggy_ledger.data.JournalType
import com.oryno.piggy_ledger.data.PiggyLedgerDao
import com.oryno.piggy_ledger.data.PostingEntity
import java.util.UUID

class InsufficientFundsException(message: String) : Exception(message)

class CreateTransferUseCase(private val dao: PiggyLedgerDao) {

    suspend operator fun invoke(
        fromAccountId: String,
        toAccountId: String,
        amount: Double,
        description: String = "",
        date: Long = System.currentTimeMillis()
    ): Result<JournalEntryEntity> {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("Transfer amount must be greater than zero."))
        }
        if (fromAccountId == toAccountId) {
            return Result.failure(IllegalArgumentException("Source and destination accounts cannot be the same."))
        }

        val fromAccount = dao.getAccountByIdSync(fromAccountId)
            ?: return Result.failure(IllegalArgumentException("Source account not found."))
        val toAccount = dao.getAccountByIdSync(toAccountId)
            ?: return Result.failure(IllegalArgumentException("Destination account not found."))

        // Insufficient funds guardrail (unless Credit Card / Liability)
        val allowOverdraft = fromAccount.type == AccountType.CARD || fromAccount.type == AccountType.LIABILITY
        if (!allowOverdraft && fromAccount.current_balance < amount) {
            return Result.failure(InsufficientFundsException("Insufficient balance in ${fromAccount.name}. Available: ${fromAccount.current_balance} ${fromAccount.currency}"))
        }

        val journalEntryId = UUID.randomUUID().toString()
        val desc = description.ifBlank { "Transfer from ${fromAccount.name} to ${toAccount.name}" }

        val journalEntry = JournalEntryEntity(
            id = journalEntryId,
            date = date,
            description = desc,
            type = JournalType.TRANSFER
        )

        // Double-entry postings: Debit ToAccount (+), Credit FromAccount (-)
        val debitPosting = PostingEntity(
            journalEntryId = journalEntryId,
            accountId = toAccountId,
            amount = amount,
            isDebit = true
        )

        val creditPosting = PostingEntity(
            journalEntryId = journalEntryId,
            accountId = fromAccountId,
            amount = amount,
            isDebit = false
        )

        dao.insertJournalEntry(journalEntry)
        dao.insertPostings(listOf(debitPosting, creditPosting))

        // Update account balances
        val updatedFrom = fromAccount.copy(
            current_balance = fromAccount.current_balance - amount,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        val updatedTo = toAccount.copy(
            current_balance = toAccount.current_balance + amount,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )

        dao.updateAccount(updatedFrom)
        dao.updateAccount(updatedTo)

        return Result.success(journalEntry)
    }
}
