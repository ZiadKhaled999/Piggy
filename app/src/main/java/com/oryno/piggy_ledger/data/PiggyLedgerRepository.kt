package com.oryno.piggy_ledger.data

import kotlinx.coroutines.flow.Flow

class PiggyLedgerRepository(private val dao: PiggyLedgerDao) {
    val allGoals: Flow<List<Goal>> = dao.getAllGoals()
    val allLoans: Flow<List<Loan>> = dao.getAllLoans()
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactionsFlow()
    val allAccounts: Flow<List<Account>> = dao.getAllAccounts()
    val includedAccounts: Flow<List<Account>> = dao.getIncludedAccounts()

    fun getGoalById(id: String) = dao.getGoalById(id)
    fun getTransactionsForGoal(id: String) = dao.getTransactionsForGoal(id)
    
    suspend fun getAccountById(id: Long) = dao.getAccountById(id)
    fun getTransactionsForAccount(id: Long) = dao.getTransactionsForAccount(id)
    fun getAllAccountTransactions() = dao.getAllAccountTransactions()

    suspend fun insertGoal(goal: Goal) = dao.insertGoal(goal)
    suspend fun insertTransaction(transaction: Transaction) = dao.insertTransaction(transaction)
    
    suspend fun insertLoan(loan: Loan) = dao.insertLoan(loan)
    
    suspend fun insertAccount(account: Account) = dao.insertAccount(account)
    suspend fun updateAccount(account: Account) = dao.updateAccount(account)
    suspend fun deleteAccount(id: Long) = dao.deleteAccountById(id)
    suspend fun insertAccountTransaction(transaction: AccountTransaction) = dao.insertAccountTransaction(transaction)
    suspend fun markLoanAsPaid(id: String) {
        val loan = dao.getLoanById(id)
        if (loan != null) {
            dao.updateLoan(loan.copy(isPaidOff = true))
        }
    }
    suspend fun deleteLoan(id: String) = dao.deleteLoanById(id)

    suspend fun deleteGoal(id: String) {
        dao.deleteTransactionsForGoal(id)
        dao.deleteGoalById(id)
    }

    suspend fun getFullBackup(streakDates: Set<String>): BackupData {
        return BackupData(
            goals = dao.getAllGoalsSync(),
            transactions = dao.getAllTransactions(),
            loans = dao.getAllLoansSync(),
            streakDates = streakDates
        )
    }

    suspend fun restoreBackup(data: BackupData) {
        dao.clearGoals()
        dao.clearTransactions()
        dao.clearLoans()
        dao.insertGoals(data.goals)
        dao.insertTransactions(data.transactions)
        dao.insertLoans(data.loans)
    }

    val allPendingTransactions: Flow<List<PendingTransaction>> = dao.getAllPendingTransactionsFlow()
    suspend fun insertPendingTransaction(transaction: PendingTransaction) = dao.insertPendingTransaction(transaction)
    suspend fun deletePendingTransaction(id: Long) = dao.deletePendingTransactionById(id)
    suspend fun resolvePendingTransaction(pendingId: Long, accountId: Long) = dao.resolvePendingTransaction(pendingId, accountId)
}
