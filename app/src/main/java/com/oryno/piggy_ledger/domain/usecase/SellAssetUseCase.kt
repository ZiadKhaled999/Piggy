package com.oryno.piggy_ledger.domain.usecase

import com.oryno.piggy_ledger.data.AssetType
import com.oryno.piggy_ledger.data.JournalEntryEntity
import com.oryno.piggy_ledger.data.JournalType
import com.oryno.piggy_ledger.data.PiggyLedgerDao
import com.oryno.piggy_ledger.data.PostingEntity
import com.oryno.piggy_ledger.data.SystemAccountsHelper
import java.util.UUID

class SellAssetUseCase(private val dao: PiggyLedgerDao) {

    suspend operator fun invoke(
        assetId: String,
        quantitySoldGrams: Double,
        actualSaleAmount: Double,
        destinationAccountId: String
    ): Result<Boolean> {
        val asset = dao.getAssetByIdSync(assetId)
            ?: return Result.failure(IllegalArgumentException("Asset not found."))

        val destinationAccount = dao.getAccountByIdSync(destinationAccountId)
            ?: return Result.failure(IllegalArgumentException("Destination account not found."))

        SystemAccountsHelper.ensureSystemAccountsCreated(dao)

        val totalQuantity = if (asset.type == AssetType.CERTIFICATE) 1.0 else asset.weightGrams
        val validQuantitySold = if (asset.type == AssetType.CERTIFICATE) 1.0 else minOf(quantitySoldGrams, totalQuantity)

        val costBasisSold = if (totalQuantity > 0) (asset.purchasePrice / totalQuantity) * validQuantitySold else asset.purchasePrice
        val realizedProfit = actualSaleAmount - costBasisSold

        val journalEntryId = UUID.randomUUID().toString()
        val journalEntry = JournalEntryEntity(
            id = journalEntryId,
            date = System.currentTimeMillis(),
            description = "Sold asset: ${asset.name}",
            type = JournalType.INCOME
        )

        // Posting 1: Debit destination cash account (+actualSaleAmount)
        val debitDestination = PostingEntity(
            journalEntryId = journalEntryId,
            accountId = destinationAccountId,
            amount = actualSaleAmount,
            isDebit = true
        )

        // Posting 2: Credit asset (-costBasisSold)
        val creditAsset = PostingEntity(
            journalEntryId = journalEntryId,
            accountId = asset.id,
            amount = costBasisSold,
            isDebit = false
        )

        // Posting 3: Credit Realized Gains system account (+realizedProfit) to balance ledger
        val creditGains = PostingEntity(
            journalEntryId = journalEntryId,
            accountId = SystemAccountsHelper.SYS_UNCAT_INCOME_ID,
            amount = realizedProfit,
            isDebit = false
        )

        dao.insertJournalEntry(journalEntry)
        dao.insertPostings(listOf(debitDestination, creditAsset, creditGains))

        // Update cash account balance
        val updatedBank = destinationAccount.copy(
            current_balance = destinationAccount.current_balance + actualSaleAmount,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateAccount(updatedBank)

        // Update or dispose asset
        val remainingGrams = totalQuantity - validQuantitySold
        if (remainingGrams <= 0.001 || asset.type == AssetType.CERTIFICATE) {
            dao.deleteAssetById(assetId)
        } else {
            val remainingRatio = remainingGrams / totalQuantity
            val updatedAsset = asset.copy(
                weightGrams = remainingGrams,
                purchasePrice = asset.purchasePrice * remainingRatio,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            dao.updateAsset(updatedAsset)
        }

        return Result.success(true)
    }
}
