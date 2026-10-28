package com.oryno.piggy_ledger.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oryno.piggy_ledger.data.PiggyLedgerDatabase
import com.oryno.piggy_ledger.data.PortfolioHistoryEntity
import com.oryno.piggy_ledger.data.repository.MarketRatesRepository
import com.oryno.piggy_ledger.domain.usecase.CalculatePortfolioValueUseCase
import java.util.concurrent.TimeUnit

class PortfolioSnapshotWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dao = PiggyLedgerDatabase.getInstance(applicationContext).piggyLedgerDao()
            val marketRatesRepository = MarketRatesRepository(dao)

            val assets = dao.getAllAssetsSync()
            val rates = marketRatesRepository.getOrFetchMarketRates()

            val calculatePortfolioValue = CalculatePortfolioValueUseCase()
            val valuation = calculatePortfolioValue(assets, rates)

            val snapshot = PortfolioHistoryEntity(
                date = System.currentTimeMillis(),
                totalPortfolioValue = valuation.totalValue
            )

            dao.insertPortfolioSnapshot(snapshot)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "portfolio_snapshot_worker"

        fun schedule24HourSnapshot(context: Context) {
            val request = PeriodicWorkRequestBuilder<PortfolioSnapshotWorker>(24, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
