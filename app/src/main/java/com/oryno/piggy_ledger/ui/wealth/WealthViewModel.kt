package com.oryno.piggy_ledger.ui.wealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AssetEntity
import com.oryno.piggy_ledger.data.AssetType
import com.oryno.piggy_ledger.data.LiveMarketRatesEntity
import com.oryno.piggy_ledger.data.PiggyLedgerDao
import com.oryno.piggy_ledger.data.PortfolioHistoryEntity
import com.oryno.piggy_ledger.data.repository.MarketRatesRepository
import com.oryno.piggy_ledger.domain.usecase.AcquireAssetUseCase
import com.oryno.piggy_ledger.domain.usecase.CalculatePortfolioValueUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WealthUiState(
    val liveRates: LiveMarketRatesEntity = LiveMarketRatesEntity(),
    val isLoadingPrices: Boolean = false,
    val errorMessage: String? = null,
    val assets: List<AssetEntity> = emptyList(),
    val bankAccounts: List<Account> = emptyList(),
    val portfolioHistory: List<PortfolioHistoryEntity> = emptyList(),
    val assetValuations: Map<String, Double> = emptyMap(),
    val totalWealthValue: Double = 0.0,
    val totalPurchasePrice: Double = 0.0,
    val overallGainLoss: Double = 0.0,
    val overallGainLossPercentage: Double = 0.0,
    val goldTotalValue: Double = 0.0,
    val silverTotalValue: Double = 0.0,
    val certificatesTotalValue: Double = 0.0
)

class WealthViewModel(
    private val dao: PiggyLedgerDao
) : ViewModel() {

    private val marketRatesRepository = MarketRatesRepository(dao)
    private val calculatePortfolioValueUseCase = CalculatePortfolioValueUseCase()
    private val acquireAssetUseCase = AcquireAssetUseCase(dao)

    private val _uiState = MutableStateFlow(WealthUiState())
    val uiState: StateFlow<WealthUiState> = combine(
        _uiState,
        dao.getAllAssetsFlow(),
        dao.getLiveMarketRatesFlow(),
        dao.getAllAccounts(),
        dao.getPortfolioHistoryFlow()
    ) { state, assetsList, rates, accounts, history ->
        val currentRates = rates ?: state.liveRates
        val valuation = calculatePortfolioValueUseCase(assetsList, currentRates)

        val totalCost = assetsList.filter { !it.is_deleted }.sumOf {
            if (it.type == AssetType.CERTIFICATE) it.principalAmount else it.purchasePrice
        }
        val totalVal = valuation.totalValue
        val gainLoss = totalVal - totalCost
        val gainLossPct = if (totalCost > 0) (gainLoss / totalCost) * 100.0 else 0.0

        val liquidAccounts = accounts.filter { !it.isSystemAccount && !it.is_deleted }

        state.copy(
            liveRates = currentRates,
            assets = assetsList,
            bankAccounts = liquidAccounts,
            portfolioHistory = history,
            assetValuations = valuation.assetValuations,
            totalWealthValue = totalVal,
            totalPurchasePrice = totalCost,
            overallGainLoss = gainLoss,
            overallGainLossPercentage = gainLossPct,
            goldTotalValue = valuation.goldValue,
            silverTotalValue = valuation.silverValue,
            certificatesTotalValue = valuation.certificateValue
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WealthUiState()
    )

    init {
        fetchLivePrices()
        startPeriodicPriceRefresh()
    }

    private fun startPeriodicPriceRefresh() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(50000) // 50 seconds
                fetchLivePrices()
            }
        }
    }

    fun fetchLivePrices(countryCode: String = "EG", forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPrices = true, errorMessage = null) }
            try {
                val rates = marketRatesRepository.getOrFetchMarketRates(countryCode, forceRefresh)
                _uiState.update {
                    it.copy(
                        liveRates = rates,
                        isLoadingPrices = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingPrices = false,
                        errorMessage = "Failed to load live rates. Using cached rates."
                    )
                }
            }
        }
    }

    fun addGoldAsset(
        name: String,
        grams: Double,
        karat: Int = 21,
        purchasePriceOverride: Double? = null,
        purchaseDateMs: Long = System.currentTimeMillis(),
        fundingAccountId: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val purityStr = "${karat}k"
            val rates = uiState.value.liveRates
            val rateForKarat = when (karat) {
                24 -> rates.gold24k
                18 -> rates.gold18k
                else -> rates.gold21k
            }
            val calculatedPrice = grams * rateForKarat
            val finalPurchasePrice = purchasePriceOverride ?: calculatedPrice

            val asset = AssetEntity(
                type = AssetType.GOLD,
                name = name.ifBlank { "Gold ($grams g, ${karat}K)" },
                weightGrams = grams,
                purity = purityStr,
                purchasePrice = finalPurchasePrice,
                startDate = purchaseDateMs,
                createdAt = purchaseDateMs
            )

            val result = acquireAssetUseCase(asset, fundingAccountId)
            result.onSuccess { onSuccess() }.onFailure { ex -> onError(ex.message ?: "Failed to acquire asset") }
        }
    }

    fun addSilverAsset(
        name: String,
        grams: Double,
        purity: String = "999",
        purchasePriceOverride: Double? = null,
        purchaseDateMs: Long = System.currentTimeMillis(),
        fundingAccountId: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val rates = uiState.value.liveRates
            val rateForPurity = when (purity) {
                "925" -> rates.silver925
                "900" -> rates.silver900
                else -> rates.silver999
            }
            val calculatedPrice = grams * rateForPurity
            val finalPurchasePrice = purchasePriceOverride ?: calculatedPrice

            val asset = AssetEntity(
                type = AssetType.SILVER,
                name = name.ifBlank { "Silver ($grams g)" },
                weightGrams = grams,
                purity = purity,
                purchasePrice = finalPurchasePrice,
                startDate = purchaseDateMs,
                createdAt = purchaseDateMs
            )

            val result = acquireAssetUseCase(asset, fundingAccountId)
            result.onSuccess { onSuccess() }.onFailure { ex -> onError(ex.message ?: "Failed to acquire asset") }
        }
    }

    fun addCertificateAsset(
        bankName: String,
        principal: Double,
        annualInterestRate: Double,
        purchaseDateMs: Long = System.currentTimeMillis(),
        fundingAccountId: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val asset = AssetEntity(
                type = AssetType.CERTIFICATE,
                name = "$bankName Certificate (${annualInterestRate}%)",
                principalAmount = principal,
                annualRate = annualInterestRate,
                purchasePrice = principal,
                startDate = purchaseDateMs,
                createdAt = purchaseDateMs
            )

            val result = acquireAssetUseCase(asset, fundingAccountId)
            result.onSuccess { onSuccess() }.onFailure { ex -> onError(ex.message ?: "Failed to acquire asset") }
        }
    }

    fun updateAssetPurchaseDate(assetId: String, newTimestamp: Long) {
        viewModelScope.launch {
            val asset = dao.getAssetByIdSync(assetId)
            if (asset != null) {
                val updated = asset.copy(
                    startDate = newTimestamp,
                    createdAt = newTimestamp,
                    updatedAt = System.currentTimeMillis()
                )
                dao.updateAsset(updated)
            }
        }
    }

    private val sellAssetUseCase = com.oryno.piggy_ledger.domain.usecase.SellAssetUseCase(dao)

    fun sellAsset(
        assetId: String,
        gramsSold: Double,
        actualSaleAmount: Double,
        destinationAccountId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = sellAssetUseCase(
                assetId = assetId,
                quantitySoldGrams = gramsSold,
                actualSaleAmount = actualSaleAmount,
                destinationAccountId = destinationAccountId
            )
            result.onSuccess { onSuccess() }.onFailure { ex -> onError(ex.message ?: "Failed to sell asset") }
        }
    }

    fun deleteAsset(assetId: String) {
        viewModelScope.launch {
            dao.deleteAssetById(assetId)
        }
    }
}
