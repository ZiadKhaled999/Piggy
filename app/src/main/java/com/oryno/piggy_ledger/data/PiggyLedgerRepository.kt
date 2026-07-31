package com.oryno.piggy_ledger.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PiggyLedgerRepository(private val dao: PiggyLedgerDao, private val context: android.content.Context) {

    private fun triggerSync() {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.oryno.piggy_ledger.service.SyncWorker>().build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncWork",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.oryno.piggy_ledger.service.SyncManager(context).syncAll()
            } catch (e: Exception) {
                android.util.Log.e("PiggyLedgerRepository", "Direct sync trigger failed", e)
            }
        }
    }

    val allGoals: Flow<List<Goal>> = dao.getAllGoals()
    val allLoans: Flow<List<Loan>> = dao.getAllLoans()
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactionsFlow()
    val allAccounts: Flow<List<Account>> = dao.getAllAccounts()
    val includedAccounts: Flow<List<Account>> = dao.getIncludedAccounts()

    fun getGoalById(id: String) = dao.getGoalById(id)
    fun getTransactionsForGoal(id: String) = dao.getTransactionsForGoal(id)
    
    suspend fun getAccountById(id: String) = dao.getAccountById(id)
    fun getTransactionsForAccount(id: String) = dao.getTransactionsForAccount(id)
    fun getAllAccountTransactions() = dao.getAllAccountTransactions()

    suspend fun insertGoal(goal: Goal) { dao.insertGoal(goal); triggerSync() }
    suspend fun insertTransaction(transaction: Transaction) { dao.insertTransaction(transaction); triggerSync() }
    
    suspend fun insertLoan(loan: Loan) { dao.insertLoan(loan); triggerSync() }
    val allLoanPayments: Flow<List<LoanPayment>> = dao.getAllLoanPaymentsFlow()
    fun getPaymentsForLoan(loanId: String) = dao.getPaymentsForLoan(loanId)
    suspend fun insertLoanPayment(payment: LoanPayment) { dao.insertLoanPayment(payment); triggerSync() }
    suspend fun deleteLoanPayment(id: String) { dao.deleteLoanPaymentById(id); triggerSync() }
    
    suspend fun insertAccount(account: Account) { dao.insertAccount(account); triggerSync() }
    suspend fun updateAccount(account: Account) { dao.updateAccount(account); triggerSync() }
    suspend fun deleteAccount(id: String) { dao.deleteAccountById(id); triggerSync() }
    suspend fun insertAccountTransaction(transaction: AccountTransaction) { dao.insertAccountTransaction(transaction); triggerSync() }
    suspend fun markLoanAsPaid(id: String) {
        val loan = dao.getLoanById(id)
        if (loan != null) {
            dao.updateLoan(loan.copy(isPaidOff = true))
            triggerSync()
        }
    }
    suspend fun deleteLoan(id: String) { dao.deleteLoanById(id); triggerSync() }

    suspend fun deleteGoal(id: String) {
        dao.deleteTransactionsForGoal(id)
        dao.deleteGoalById(id)
    }

    suspend fun getFullBackup(streakDates: Set<String>): BackupData {
        return BackupData(
            goals = dao.getAllGoalsSync(),
            transactions = dao.getAllTransactions(),
            loans = dao.getAllLoansSync(),
            loanPayments = dao.getAllLoanPaymentsSync(),
            streakDates = streakDates
        )
    }

    suspend fun restoreBackup(data: BackupData) {
        dao.clearGoals()
        dao.clearTransactions()
        dao.clearLoans()
        dao.clearLoanPayments()
        dao.insertGoals(data.goals)
        dao.insertTransactions(data.transactions)
        dao.insertLoans(data.loans)
        dao.insertLoanPayments(data.loanPayments)
    }

    suspend fun getFullDatabaseBackup(streakDates: Set<String>): FullBackupData {
        return FullBackupData(
            goals = dao.getAllGoalsSync(),
            transactions = dao.getAllTransactions(),
            loans = dao.getAllLoansSync(),
            loanPayments = dao.getAllLoanPaymentsSync(),
            accounts = dao.getAllAccountsSync(),
            accountTransactions = dao.getAllAccountTransactionsSync(),
            pendingTransactions = dao.getAllPendingTransactionsSync(),
            streakDates = streakDates
        )
    }

    suspend fun restoreFullDatabaseBackup(data: FullBackupData) {
        dao.clearGoals()
        dao.clearTransactions()
        dao.clearLoans()
        dao.clearLoanPayments()
        dao.clearAccounts()
        dao.clearAccountTransactions()
        dao.clearPendingTransactions()

        dao.insertGoals(data.goals)
        dao.insertTransactions(data.transactions)
        dao.insertLoans(data.loans)
        dao.insertLoanPayments(data.loanPayments)
        dao.insertAccounts(data.accounts)
        dao.insertAccountTransactions(data.accountTransactions)
        dao.insertPendingTransactions(data.pendingTransactions)
    }

    val allPendingTransactions: Flow<List<PendingTransaction>> = dao.getAllPendingTransactionsFlow()
    suspend fun insertPendingTransaction(transaction: PendingTransaction) { dao.insertPendingTransaction(transaction); triggerSync() }
    suspend fun deletePendingTransaction(id: String) { dao.deletePendingTransactionById(id); triggerSync() }
    suspend fun resolvePendingTransaction(pendingId: String, accountId: String) { dao.resolvePendingTransaction(pendingId, accountId); triggerSync() }
}
