package com.oryno.piggy_ledger.data

object SystemAccountsHelper {
    const val SYS_UNCAT_EXPENSE_ID = "SYS_UNCAT_EXPENSE"
    const val SYS_UNCAT_INCOME_ID = "SYS_UNCAT_INCOME"
    const val SYS_OPENING_BALANCE_ID = "SYS_OPENING_BALANCE"

    val SYS_UNCAT_EXPENSE = Account(
        id = SYS_UNCAT_EXPENSE_ID,
        name = "Uncategorized Expenses",
        type = AccountType.EXPENSE_SYSTEM,
        icon_color = "#FF5722",
        currency = "EGP",
        starting_balance = 0.0,
        current_balance = 0.0,
        isSystemAccount = true,
        exclude_from_all = true
    )

    val SYS_UNCAT_INCOME = Account(
        id = SYS_UNCAT_INCOME_ID,
        name = "Uncategorized Income",
        type = AccountType.INCOME_SYSTEM,
        icon_color = "#4CAF50",
        currency = "EGP",
        starting_balance = 0.0,
        current_balance = 0.0,
        isSystemAccount = true,
        exclude_from_all = true
    )

    val SYS_OPENING_BALANCE = Account(
        id = SYS_OPENING_BALANCE_ID,
        name = "Opening Balance Equity",
        type = AccountType.EQUITY_SYSTEM,
        icon_color = "#2196F3",
        currency = "EGP",
        starting_balance = 0.0,
        current_balance = 0.0,
        isSystemAccount = true,
        exclude_from_all = true
    )

    suspend fun ensureSystemAccountsCreated(dao: PiggyLedgerDao) {
        if (dao.getAccountByIdSync(SYS_UNCAT_EXPENSE_ID) == null) {
            dao.insertAccount(SYS_UNCAT_EXPENSE)
        }
        if (dao.getAccountByIdSync(SYS_UNCAT_INCOME_ID) == null) {
            dao.insertAccount(SYS_UNCAT_INCOME)
        }
        if (dao.getAccountByIdSync(SYS_OPENING_BALANCE_ID) == null) {
            dao.insertAccount(SYS_OPENING_BALANCE)
        }
    }
}
