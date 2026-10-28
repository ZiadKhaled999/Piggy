package com.oryno.piggy_ledger.data.repository

import com.oryno.piggy_ledger.data.LiveMarketRatesEntity
import com.oryno.piggy_ledger.data.PiggyLedgerDao
import com.oryno.piggy_ledger.data.remote.ZahabPriceService
import kotlinx.coroutines.flow.Flow

class MarketRatesRepository(private val dao: PiggyLedgerDao) {

    val liveRatesFlow: Flow<LiveMarketRatesEntity?> = dao.getLiveMarketRatesFlow()

    suspend fun getOrFetchMarketRates(countryCode: String = "EG", forceRefresh: Boolean = false): LiveMarketRatesEntity {
        val cached = dao.getLiveMarketRatesSync()
        val now = System.currentTimeMillis()

        if (!forceRefresh && cached != null && (now - cached.lastUpdated) < 60_000L) {
            return cached
        }

        return try {
            val goldPrices = ZahabPriceService.getGoldPrice(countryCode, forceRefresh = true)
            val silverPrices = ZahabPriceService.getSilverPrice(countryCode, forceRefresh = true)

            val updatedEntity = LiveMarketRatesEntity(
                id = "singleton_rates",
                gold24k = goldPrices.price24k,
                gold21k = goldPrices.price21k,
                gold18k = goldPrices.price18k,
                silver999 = silverPrices.pricePerGram,
                silver925 = silverPrices.pricePerGram * 0.925,
                silver900 = silverPrices.pricePerGram * 0.900,
                lastUpdated = now
            )

            dao.insertOrUpdateMarketRates(updatedEntity)
            updatedEntity
        } catch (e: Exception) {
            cached ?: LiveMarketRatesEntity(lastUpdated = now).also {
                dao.insertOrUpdateMarketRates(it)
            }
        }
    }
}
