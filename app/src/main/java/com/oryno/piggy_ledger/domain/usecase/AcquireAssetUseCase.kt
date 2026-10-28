package com.oryno.piggy_ledger.domain.usecase

import com.oryno.piggy_ledger.data.AccountType
import com.oryno.piggy_ledger.data.AssetEntity
import com.oryno.piggy_ledger.data.JournalEntryEntity
import com.oryno.piggy_ledger.data.JournalType
import com.oryno.piggy_ledger.data.PiggyLedgerDao
import com.oryno.piggy_ledger.data.PostingEntity
import com.oryno.piggy_ledger.data.SystemAccountsHelper
import java.util.UUID

class AcquireAssetUseCase(private val dao: PiggyLedgerDao) {

    suspend operator fun invoke(
        asset: AssetEntity,
        fundingAccountId: String? = null
    ): Result<AssetEntity> {
        if (asset.purchasePrice < 0) {
            return Result.failure(IllegalArgumentException("Purchase price cannot be negative."))
        }

        SystemAccountsHelper.ensureSystemAccountsCreated(dao)
        val journalEntryId = UUID.randomUUID().toString()

        if (!fundingAccountId.isNullOrBlank()) {
            // Scenario A: Bought with a tracked Bank/Cash account
            val bankAccount = dao.getAccountByIdSync(fundingAccountId)
                ?: return Result.failure(IllegalArgumentException("Funding account not found."))

            val allowOverdraft = bankAccount.type == AccountType.CARD || bankAccount.type == AccountType.LIABILITY
            if (!allowOverdraft && bankAccount.current_balance < asset.purchasePrice) {
                return Result.failure(
                    InsufficientFundsException("Insufficient funds in ${bankAccount.name}. Available: ${bankAccount.current_balance} ${bankAccount.currency}")
                )
            }

            val journalEntry = JournalEntryEntity(
                id = journalEntryId,
                date = asset.startDate,
                description = "Acquired asset: ${asset.name}",
                type = JournalType.ASSET_BUY
            )

            // Double-entry postings: Debit Asset (+), Credit Bank (-)
            val debitAsset = PostingEntity(
                journalEntryId = journalEntryId,
                accountId = asset.id,
                amount = asset.purchasePrice,
                isDebit = true
            )
            val creditBank = PostingEntity(
                journalEntryId = journalEntryId,
                accountId = fundingAccountId,
                amount = asset.purchasePrice,
                isDebit = false
            )

            dao.insertJournalEntry(journalEntry)
            dao.insertPostings(listOf(debitAsset, creditBank))

            // Update funding bank account balance
            val updatedAccount = bankAccount.copy(
                current_balance = bankAccount.current_balance - asset.purchasePrice,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
            dao.updateAccount(updatedAccount)

        } else {
            // Scenario B: Pre-owned / External (Gift / Inheritance)
            val journalEntry = JournalEntryEntity(
                id = journalEntryId,
                date = asset.startDate,
                description = "Pre-owned asset registered: ${asset.name}",
                type = JournalType.ASSET_BUY
            )

            val debitAsset = PostingEntity(
                journalEntryId = journalEntryId,
                accountId = asset.id,
                amount = asset.purchasePrice,
                isDebit = true
            )
            val creditEquity = PostingEntity(
                journalEntryId = journalEntryId,
                accountId = SystemAccountsHelper.SYS_OPENING_BALANCE_ID,
                amount = asset.purchasePrice,
                isDebit = false
            )

            dao.insertJournalEntry(journalEntry)
            dao.insertPostings(listOf(debitAsset, creditEquity))
        }

        dao.insertAsset(asset)
        return Result.success(asset)
    }
}
