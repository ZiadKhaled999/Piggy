package com.oryno.piggy_ledger.data.repository

import com.oryno.piggy_ledger.data.LiveMarketRatesEntity
import com.oryno.piggy_ledger.data.PiggyLedgerDao
import com.oryno.piggy_ledger.data.remote.ZahabClient
import com.oryno.piggy_ledger.data.remote.ZahabError
import kotlinx.coroutines.flow.Flow

class MarketRatesRepository(private val dao: PiggyLedgerDao) {

    private val client = ZahabClient()

    val liveRatesFlow: Flow<LiveMarketRatesEntity?> = dao.getLiveMarketRatesFlow()

    suspend fun getOrFetchMarketRates(countryCode: String = "EG", forceRefresh: Boolean = false): LiveMarketRatesEntity {
        val cached = dao.getLiveMarketRatesSync()
        val now = System.currentTimeMillis()

        if (!forceRefresh && cached != null && (now - cached.lastUpdated) < 60_000L) {
            return cached
        }

        return try {
            val snapshot = client.fetchPrices(countryCode, forceRefresh = true)
            val gold24 = snapshot.goldPerGram(24) ?: cached?.gold24k ?: 4120.0
            val gold21 = snapshot.goldPerGram(21) ?: cached?.gold21k ?: 3605.0
            val gold18 = snapshot.goldPerGram(18) ?: cached?.gold18k ?: 3090.0
            val silver999 = snapshot.silverPerGram(999) ?: cached?.silver999 ?: 52.0
            val silver925 = snapshot.silverPerGram(925) ?: (silver999 * 0.925)
            val silver900 = snapshot.silverPerGram(900) ?: (silver999 * 0.900)

            val updatedEntity = LiveMarketRatesEntity(
                id = "singleton_rates",
                gold24k = gold24,
                gold21k = gold21,
                gold18k = gold18,
                silver999 = silver999,
                silver925 = silver925,
                silver900 = silver900,
                lastUpdated = now
            )

            dao.insertOrUpdateMarketRates(updatedEntity)
            updatedEntity
        } catch (e: ZahabError) {
            cached ?: LiveMarketRatesEntity(lastUpdated = now).also {
                dao.insertOrUpdateMarketRates(it)
            }
        } catch (e: Exception) {
            cached ?: LiveMarketRatesEntity(lastUpdated = now).also {
                dao.insertOrUpdateMarketRates(it)
            }
        }
    }
}
