package com.oryno.piggy_ledger.domain.usecase

import com.oryno.piggy_ledger.data.Account
import com.oryno.piggy_ledger.data.AccountType

data class NetWorthBreakdown(
    val totalNetWorth: Double,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val liquidCash: Double,
    val investmentsAndAssets: Double
)

class CalculateNetWorthUseCase {

    operator fun invoke(accounts: List<Account>): NetWorthBreakdown {
        var totalAssets = 0.0
        var totalLiabilities = 0.0
        var liquidCash = 0.0
        var investmentsAndAssets = 0.0

        accounts.filter { !it.exclude_from_all && !it.isSystemAccount && !it.is_deleted }.forEach { account ->
            when (account.type) {
                AccountType.BANK, AccountType.CASH, AccountType.WALLET -> {
                    if (account.current_balance >= 0) {
                        totalAssets += account.current_balance
                        liquidCash += account.current_balance
                    } else {
                        totalLiabilities += kotlin.math.abs(account.current_balance)
                    }
                }
                AccountType.INVESTMENT, AccountType.ASSET_GOLD, AccountType.ASSET_SILVER, AccountType.ASSET_PROPERTY, AccountType.CERTIFICATE -> {
                    totalAssets += account.current_balance
                    investmentsAndAssets += account.current_balance
                }
                AccountType.CARD, AccountType.LIABILITY -> {
                    if (account.current_balance < 0) {
                        totalLiabilities += kotlin.math.abs(account.current_balance)
                    } else if (account.type == AccountType.CARD && account.credit_limit != null && account.available_credit != null) {
                        // Used credit is credit_limit - available_credit
                        val used = account.credit_limit - account.available_credit
                        if (used > 0) {
                            totalLiabilities += used
                        }
                    } else if (account.current_balance > 0 && account.type == AccountType.LIABILITY) {
                        totalLiabilities += account.current_balance
                    }
                }
                AccountType.EXPENSE_SYSTEM, AccountType.INCOME_SYSTEM, AccountType.EQUITY_SYSTEM -> {
                    // System accounts ignored for net worth calculation
                }
            }
        }

        val totalNetWorth = totalAssets - totalLiabilities

        return NetWorthBreakdown(
            totalNetWorth = totalNetWorth,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            liquidCash = liquidCash,
            investmentsAndAssets = investmentsAndAssets
        )
    }
}
