package com.oryno.piggy_ledger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PiggyLedgerDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id")
    fun getGoalById(id: String): Flow<Goal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Query("SELECT * FROM transactions WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun getTransactionsForGoal(goalId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM loans ORDER BY isPaidOff ASC, timestamp DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: String): Loan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan)
    
    @Update
    suspend fun updateLoan(loan: Loan)

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY timestamp DESC")
    fun getPaymentsForLoan(loanId: String): Flow<List<LoanPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanPayment(payment: LoanPayment)

    @Query("DELETE FROM loan_payments WHERE id = :id")
    suspend fun deleteLoanPaymentById(id: Long)

    @Query("SELECT * FROM loan_payments")
    suspend fun getAllLoanPaymentsSync(): List<LoanPayment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanPayments(payments: List<LoanPayment>)

    @Query("DELETE FROM loan_payments")
    suspend fun clearLoanPayments()

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: String)

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE exclude_from_all = 0 ORDER BY name ASC")
    fun getIncludedAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    @Query("SELECT * FROM account_transactions WHERE account_id = :accountId ORDER BY timestamp DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<AccountTransaction>>

    @Query("SELECT * FROM account_transactions ORDER BY timestamp DESC")
    fun getAllAccountTransactions(): Flow<List<AccountTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountTransaction(transaction: AccountTransaction)

    @androidx.room.Transaction
    suspend fun processSmsTransaction(accountId: Long, amount: Double, merchant: String, applyInstaPayFee: Boolean) {
        val account = getAccountById(accountId) ?: return
        
        var finalAmount = amount
        if (applyInstaPayFee) {
            val fee = minOf(amount * 0.001, 20.0)
            finalAmount += fee
        }

        val newTransaction = AccountTransaction(
            account_id = accountId,
            amount = -finalAmount, // Assuming SMS are expenses. We might need to refine this later if there are income SMS. Let's assume expense for now or as per standard.
            merchant = merchant
        )
        insertAccountTransaction(newTransaction)
        
        val newBalance = account.current_balance - finalAmount
        var newAvailableCredit = account.available_credit
        if (account.type == AccountType.CARD && newAvailableCredit != null) {
            newAvailableCredit -= finalAmount
        }

        updateAccount(account.copy(current_balance = newBalance, available_credit = newAvailableCredit))
    }

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Query("DELETE FROM transactions WHERE goalId = :goalId")
    suspend fun deleteTransactionsForGoal(goalId: String)

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<Transaction>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoalsSync(): List<Goal>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsSync(): List<Account>

    @Query("SELECT * FROM loans")
    suspend fun getAllLoansSync(): List<Loan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<Goal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<Loan>)

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM loans")
    suspend fun clearLoans()

    @Query("SELECT * FROM account_transactions")
    suspend fun getAllAccountTransactionsSync(): List<AccountTransaction>

    @Query("SELECT * FROM pending_transactions")
    suspend fun getAllPendingTransactionsSync(): List<PendingTransaction>

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("DELETE FROM account_transactions")
    suspend fun clearAccountTransactions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountTransactions(transactions: List<AccountTransaction>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTransactions(transactions: List<PendingTransaction>)

    @Query("SELECT * FROM pending_transactions ORDER BY timestamp DESC")
    fun getAllPendingTransactionsFlow(): Flow<List<PendingTransaction>>

    @Query("SELECT * FROM pending_transactions WHERE id = :id")
    suspend fun getPendingTransactionById(id: Long): PendingTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTransaction(transaction: PendingTransaction): Long

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deletePendingTransactionById(id: Long)

    @Query("DELETE FROM pending_transactions")
    suspend fun clearPendingTransactions()

    @androidx.room.Transaction
    suspend fun resolvePendingTransaction(pendingId: Long, accountId: Long) {
        val pending = getPendingTransactionById(pendingId) ?: return
        processSmsTransaction(
            accountId = accountId,
            amount = pending.amount,
            merchant = pending.merchant,
            applyInstaPayFee = getAccountById(accountId)?.insta_pay_fee == true
        )
        deletePendingTransactionById(pendingId)
    }
}
