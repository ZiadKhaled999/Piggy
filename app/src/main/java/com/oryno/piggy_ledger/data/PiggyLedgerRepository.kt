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

    suspend fun getPendingUploadCount(): Int = dao.getTotalUnsyncedCount()

    fun getGoalById(id: String) = dao.getGoalById(id)
    fun getTransactionsForGoal(id: String) = dao.getTransactionsForGoal(id)
    
    suspend fun getAccountById(id: String) = dao.getAccountById(id)
    fun getTransactionsForAccount(id: String) = dao.getTransactionsForAccount(id)
    fun getAllAccountTransactions() = dao.getAllAccountTransactions()

    suspend fun insertGoal(goal: Goal) {
        val user = com.clerk.api.Clerk.userFlow.value
        val userId = user?.id ?: "local_user"
        val updatedGoal = if (goal.userId.isBlank()) goal.copy(userId = userId) else goal
        dao.insertGoal(updatedGoal)
        triggerSync()
    }
    suspend fun insertTransaction(transaction: Transaction) {
        val user = com.clerk.api.Clerk.userFlow.value
        val userId = user?.id ?: "local_user"
        val updatedTransaction = if (transaction.userId.isBlank()) transaction.copy(userId = userId) else transaction
        dao.insertTransaction(updatedTransaction)
        triggerSync()
    }
    
    suspend fun insertLoan(loan: Loan) {
        val user = com.clerk.api.Clerk.userFlow.value
        val userId = user?.id ?: "local_user"
        val updatedLoan = if (loan.userId.isBlank()) loan.copy(userId = userId) else loan
        dao.insertLoan(updatedLoan)
        triggerSync()
    }
    val allLoanPayments: Flow<List<LoanPayment>> = dao.getAllLoanPaymentsFlow()
    fun getPaymentsForLoan(loanId: String) = dao.getPaymentsForLoan(loanId)
    suspend fun insertLoanPayment(payment: LoanPayment) {
        val user = com.clerk.api.Clerk.userFlow.value
        val userId = user?.id ?: "local_user"
        val updatedPayment = if (payment.userId.isBlank()) payment.copy(userId = userId) else payment
        dao.insertLoanPayment(updatedPayment)
        triggerSync()
    }
    suspend fun deleteLoanPayment(id: String) { dao.deleteLoanPaymentById(id); try { com.oryno.piggy_ledger.service.SyncManager(context).deleteFromCloud("loan_payments", id) } catch(e: Exception){}; triggerSync() }
    
    suspend fun insertAccount(account: Account) {
        val user = com.clerk.api.Clerk.userFlow.value
        val userId = user?.id ?: "local_user"
        val updatedAccount = if (account.userId.isBlank()) account.copy(userId = userId) else account
        dao.insertAccount(updatedAccount)
        triggerSync()
    }
    suspend fun updateAccount(account: Account) {
        val user = com.clerk.api.Clerk.userFlow.value
        val userId = user?.id ?: "local_user"
        val updatedAccount = if (account.userId.isBlank()) account.copy(userId = userId, isSynced = false) else account.copy(isSynced = false)
        dao.updateAccount(updatedAccount)
        triggerSync()
    }
    suspend fun deleteAccount(id: String) { dao.deleteTransactionsForAccount(id); dao.deleteAccountById(id); try { com.oryno.piggy_ledger.service.SyncManager(context).deleteFromCloud("accounts", id) } catch(e: Exception){}; triggerSync() }
    suspend fun insertAccountTransaction(transaction: AccountTransaction) {
        val user = com.clerk.api.Clerk.userFlow.value
        val userId = user?.id ?: "local_user"
        val updatedTransaction = if (transaction.userId.isBlank()) transaction.copy(userId = userId) else transaction
        dao.insertAccountTransaction(updatedTransaction)
        triggerSync()
    }
    suspend fun markLoanAsPaid(id: String) {
        val loan = dao.getLoanById(id)
        if (loan != null) {
            dao.updateLoan(loan.copy(isPaidOff = true, isSynced = false))
            triggerSync()
        }
    }
    suspend fun deleteLoan(id: String) { dao.deleteLoanById(id); try { com.oryno.piggy_ledger.service.SyncManager(context).deleteFromCloud("loans", id) } catch(e: Exception){}; triggerSync() }

    suspend fun deleteGoal(id: String) {
        dao.deleteTransactionsForGoal(id)
        dao.deleteGoalById(id)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.oryno.piggy_ledger.service.SyncManager(context).deleteFromCloud("goals", id)
        }
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


    val allPendingTransactions: Flow<List<PendingTransaction>> = dao.getAllPendingTransactionsFlow()
    suspend fun insertPendingTransaction(transaction: PendingTransaction) {
        val user = com.clerk.api.Clerk.userFlow.value
        val userId = user?.id ?: "local_user"
        val updatedTransaction = if (transaction.userId.isBlank()) transaction.copy(userId = userId) else transaction
        dao.insertPendingTransaction(updatedTransaction)
        triggerSync()
    }
    suspend fun deletePendingTransaction(id: String) { dao.deletePendingTransactionById(id); try { com.oryno.piggy_ledger.service.SyncManager(context).deleteFromCloud("pending_transactions", id) } catch(e: Exception){}; triggerSync() }
    suspend fun resolvePendingTransaction(pendingId: String, accountId: String) { dao.resolvePendingTransaction(pendingId, accountId); triggerSync() }

    suspend fun saveOnboardingAnswer(key: String, value: String) {
        val now = System.currentTimeMillis()
        val answer = OnboardingAnswer(
            id = "${now}_$key",
            userId = null,
            key = key,
            value = value,
            updatedAt = now,
            isSynced = false
        )
        dao.insertOnboardingAnswer(answer)
        triggerSync()
    }

    suspend fun saveOnboardingAnswers(answers: Map<String, String>) {
        val now = System.currentTimeMillis()
        val list = answers.map { (k, v) ->
            OnboardingAnswer(
                id = "${now}_$k",
                userId = null,
                key = k,
                value = v,
                updatedAt = now,
                isSynced = false
            )
        }
        dao.insertOnboardingAnswers(list)
        triggerSync()
    }
}
