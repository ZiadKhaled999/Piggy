package com.oryno.piggy_ledger.domain.usecase

import com.oryno.piggy_ledger.data.AssetEntity
import com.oryno.piggy_ledger.data.AssetType
import com.oryno.piggy_ledger.data.LiveMarketRatesEntity

data class PortfolioValuation(
    val totalValue: Double,
    val goldValue: Double,
    val silverValue: Double,
    val certificateValue: Double,
    val assetValuations: Map<String, Double> // Map of Asset ID -> Current Valuation
)

class CalculatePortfolioValueUseCase {

    operator fun invoke(
        assets: List<AssetEntity>,
        marketRates: LiveMarketRatesEntity,
        currentDate: Long = System.currentTimeMillis()
    ): PortfolioValuation {
        var totalGold = 0.0
        var totalSilver = 0.0
        var totalCerts = 0.0
        val valuationMap = mutableMapOf<String, Double>()

        assets.filter { !it.is_deleted }.forEach { asset ->
            val value = when (asset.type) {
                AssetType.GOLD -> calculateGoldValue(asset, marketRates)
                AssetType.SILVER -> calculateSilverValue(asset, marketRates)
                AssetType.CERTIFICATE -> calculateCertificateValue(asset, currentDate)
            }

            valuationMap[asset.id] = value

            when (asset.type) {
                AssetType.GOLD -> totalGold += value
                AssetType.SILVER -> totalSilver += value
                AssetType.CERTIFICATE -> totalCerts += value
            }
        }

        val grandTotal = totalGold + totalSilver + totalCerts

        return PortfolioValuation(
            totalValue = grandTotal,
            goldValue = totalGold,
            silverValue = totalSilver,
            certificateValue = totalCerts,
            assetValuations = valuationMap
        )
    }

    private fun calculateGoldValue(asset: AssetEntity, rates: LiveMarketRatesEntity): Double {
        val purityMultiplier = when (asset.purity.lowercase()) {
            "24k", "24" -> 1.0
            "21k", "21" -> 0.875
            "18k", "18" -> 0.75
            else -> 0.875
        }
        val rateForPurity = when (asset.purity.lowercase()) {
            "24k", "24" -> rates.gold24k
            "18k", "18" -> rates.gold18k
            else -> rates.gold21k
        }
        return asset.weightGrams * (rateForPurity * purityMultiplier)
    }

    private fun calculateSilverValue(asset: AssetEntity, rates: LiveMarketRatesEntity): Double {
        val purityMultiplier = when (asset.purity) {
            "999" -> 1.0
            "925" -> 0.925
            "900" -> 0.900
            else -> 1.0
        }
        val rateForPurity = when (asset.purity) {
            "925" -> rates.silver925
            "900" -> rates.silver900
            else -> rates.silver999
        }
        return asset.weightGrams * (rateForPurity * purityMultiplier)
    }

    private fun calculateCertificateValue(asset: AssetEntity, currentDate: Long): Double {
        val daysElapsed = maxOf(0.0, (currentDate - asset.startDate) / 86400000.0)
        val dailyRate = (asset.annualRate / 100.0) / 365.0
        val accruedInterest = asset.principalAmount * dailyRate * daysElapsed
        return asset.principalAmount + accruedInterest
    }
}
