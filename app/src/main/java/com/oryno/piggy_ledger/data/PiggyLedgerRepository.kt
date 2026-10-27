package com.oryno.piggy_ledger.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PiggyLedgerRepository(private val dao: PiggyLedgerDao, private val context: android.content.Context) {

    private fun triggerSync() {
        // Single trigger only — see matching comment in UserPreferences.kt.
        // FIX(first-install-crash): best-effort; a broken WorkManager DB must
        // not crash the DB write that triggered the sync.
        // FIX(sync): delegate to SyncWorker.scheduleOneTime (KEEP + CONNECTED +
        // backoff) so rapid writes coalesce and offline edits wait for reconnect.
        com.oryno.piggy_ledger.service.SyncWorker.scheduleOneTime(context)
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
        // FIX(sync): bump updatedAt so last-writer-wins compares the edit time,
        // not the row's creation time.
        val now = System.currentTimeMillis()
        val updatedAccount = if (account.userId.isBlank()) account.copy(userId = userId, isSynced = false, updatedAt = now) else account.copy(isSynced = false, updatedAt = now)
        dao.updateAccount(updatedAccount)
        triggerSync()
    }
    suspend fun deleteAccount(id: String) {
        dao.deleteTransactionsForAccount(id)
        dao.deleteAccountById(id)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.oryno.piggy_ledger.service.SyncManager(context).deleteFromCloud("accounts", id)
            } catch (e: Exception) {
                android.util.Log.e("Repository", "Failed cloud deletion for account: $id", e)
            }
        }
        // FIX(sync): the tombstone (is_deleted=1, isSynced=0) above is the real
        // delete channel (DELETE route 404s) — it must enqueue a push.
        triggerSync()
    }
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
            // FIX(sync): bump updatedAt, see updateAccount above.
            dao.updateLoan(loan.copy(isPaidOff = true, isSynced = false, updatedAt = System.currentTimeMillis()))
            triggerSync()
        }
    }
    suspend fun deleteLoan(id: String) { dao.deleteLoanPaymentsForLoan(id); dao.deleteLoanById(id); try { com.oryno.piggy_ledger.service.SyncManager(context).deleteFromCloud("loans", id) } catch(e: Exception){}; triggerSync() }

    suspend fun deleteGoal(id: String) {
        dao.deleteTransactionsForGoal(id)
        dao.deleteGoalById(id)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.oryno.piggy_ledger.service.SyncManager(context).deleteFromCloud("goals", id)
        }
        // FIX(sync): enqueue the tombstone push (see deleteAccount above).
        triggerSync()
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
        // FIX(sync): stable id per key. Timestamp ids created a new row per save
        // so duplicates accumulated locally and remotely; reuse the newest row's
        // id (upsert) and tombstone any older duplicates for the same key.
        val existing = dao.getOnboardingAnswersByKey(key)
        val keepId = existing.firstOrNull()?.id ?: "${now}_$key"
        val answer = OnboardingAnswer(
            id = keepId,
            userId = existing.firstOrNull()?.userId,
            key = key,
            value = value,
            updatedAt = now,
            isSynced = false
        )
        dao.insertOnboardingAnswer(answer)
        for (dup in existing.drop(1)) {
            try { dao.deleteOnboardingAnswerById(dup.id, now) } catch (_e: Exception) {}
        }
        triggerSync()
    }

    suspend fun saveOnboardingAnswers(answers: Map<String, String>) {
        val now = System.currentTimeMillis()
        for ((k, v) in answers) {
            val existing = dao.getOnboardingAnswersByKey(k)
            val keepId = existing.firstOrNull()?.id ?: "${now}_$k"
            dao.insertOnboardingAnswer(
                OnboardingAnswer(
                    id = keepId,
                    userId = existing.firstOrNull()?.userId,
                    key = k,
                    value = v,
                    updatedAt = now,
                    isSynced = false
                )
            )
            for (dup in existing.drop(1)) {
                try { dao.deleteOnboardingAnswerById(dup.id, now) } catch (_e: Exception) {}
            }
        }
        triggerSync()
    }
}
