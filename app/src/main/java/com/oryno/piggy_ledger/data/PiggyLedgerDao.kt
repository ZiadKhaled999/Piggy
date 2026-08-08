package com.oryno.piggy_ledger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PiggyLedgerDao {

    @Query("SELECT * FROM goals WHERE isSynced = 0")
    suspend fun getUnsyncedGoals(): List<Goal>

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<Transaction>

    @Query("SELECT * FROM loans WHERE isSynced = 0")
    suspend fun getUnsyncedLoans(): List<Loan>

    @Query("SELECT * FROM loan_payments WHERE isSynced = 0")
    suspend fun getUnsyncedLoanPayments(): List<LoanPayment>

    @Query("SELECT * FROM accounts WHERE isSynced = 0")
    suspend fun getUnsyncedAccounts(): List<Account>

    @Query("SELECT * FROM account_transactions WHERE isSynced = 0")
    suspend fun getUnsyncedAccountTransactions(): List<AccountTransaction>

    @Query("SELECT * FROM pending_transactions WHERE isSynced = 0")
    suspend fun getUnsyncedPendingTransactions(): List<PendingTransaction>

    @Query("SELECT * FROM user_preferences WHERE isSynced = 0")
    suspend fun getUnsyncedUserPreferences(): List<UserPreferencesEntity>

    @Query("SELECT * FROM streak_dates WHERE isSynced = 0")
    suspend fun getUnsyncedStreakDates(): List<StreakDateEntity>

    @Query("SELECT * FROM ai_conversations WHERE isSynced = 0")
    suspend fun getUnsyncedAiConversations(): List<AiConversation>

    @Query("SELECT * FROM ai_chat_messages WHERE isSynced = 0")
    suspend fun getUnsyncedAiChatMessages(): List<AiChatMessage>

    @Query("SELECT * FROM onboarding_answers WHERE isSynced = 0")
    suspend fun getUnsyncedOnboardingAnswers(): List<OnboardingAnswer>

    suspend fun getTotalUnsyncedCount(): Int {
        return getUnsyncedGoals().size +
               getUnsyncedTransactions().size +
               getUnsyncedLoans().size +
               getUnsyncedLoanPayments().size +
               getUnsyncedAccounts().size +
               getUnsyncedAccountTransactions().size +
               getUnsyncedPendingTransactions().size +
               getUnsyncedUserPreferences().size +
               getUnsyncedStreakDates().size +
               getUnsyncedAiConversations().size +
               getUnsyncedAiChatMessages().size +
               getUnsyncedOnboardingAnswers().size
    }

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

    @Query("SELECT * FROM loan_payments ORDER BY timestamp DESC")
    fun getAllLoanPaymentsFlow(): Flow<List<LoanPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanPayment(payment: LoanPayment)

    @Query("SELECT * FROM loan_payments WHERE id = :id")
    suspend fun getLoanPaymentById(id: String): LoanPayment?

    @Query("DELETE FROM loan_payments WHERE id = :id")
    suspend fun deleteLoanPaymentById(id: String)

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
    suspend fun getAccountById(id: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Update
    suspend fun updateAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: String)

    @Query("DELETE FROM account_transactions WHERE account_id = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: String)

    @Query("SELECT * FROM account_transactions WHERE account_id = :accountId ORDER BY timestamp DESC")
    fun getTransactionsForAccount(accountId: String): Flow<List<AccountTransaction>>

    @Query("SELECT * FROM account_transactions ORDER BY timestamp DESC")
    fun getAllAccountTransactions(): Flow<List<AccountTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountTransaction(transaction: AccountTransaction)

    @androidx.room.Transaction
    suspend fun processSmsTransaction(accountId: String, amount: Double, merchant: String, applyInstaPayFee: Boolean, isIncome: Boolean = false) {
        val account = getAccountById(accountId) ?: return
        
        var finalAmount = amount
        if (!isIncome && applyInstaPayFee) {
            val fee = minOf(amount * 0.001, 20.0)
            finalAmount += fee
        }
        val signedAmount = if (isIncome) finalAmount else -finalAmount
        
        val newTransaction = AccountTransaction(
            account_id = accountId,
            amount = signedAmount,
            merchant = merchant
        )
        insertAccountTransaction(newTransaction)
        
        val newBalance = account.current_balance + signedAmount
        var newAvailableCredit = account.available_credit
        if (account.type == AccountType.CARD && newAvailableCredit != null) {
            newAvailableCredit += signedAmount
        }
        updateAccount(account.copy(current_balance = newBalance, available_credit = newAvailableCredit, isSynced = false))
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
    suspend fun getPendingTransactionById(id: String): PendingTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTransaction(transaction: PendingTransaction)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deletePendingTransactionById(id: String)

    @Query("DELETE FROM pending_transactions")
    suspend fun clearPendingTransactions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnboardingAnswer(answer: OnboardingAnswer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnboardingAnswers(answers: List<OnboardingAnswer>)

    @Query("SELECT * FROM onboarding_answers")
    suspend fun getAllOnboardingAnswers(): List<OnboardingAnswer>

    @Query("SELECT * FROM onboarding_answers")
    suspend fun getAllOnboardingAnswersSync(): List<OnboardingAnswer>

    @Query("SELECT * FROM onboarding_answers WHERE `key` = :key")
    suspend fun getOnboardingAnswerByKey(key: String): OnboardingAnswer?

    @Query("DELETE FROM onboarding_answers WHERE id = :id")
    suspend fun deleteOnboardingAnswerById(id: String)

    @androidx.room.Transaction
    suspend fun resolvePendingTransaction(pendingId: String, accountId: String) {
        val pending = getPendingTransactionById(pendingId) ?: return
        val isIncome = pending.amount > 0
        val absAmount = kotlin.math.abs(pending.amount)
        processSmsTransaction(
            accountId = accountId,
            amount = absAmount,
            merchant = pending.merchant,
            applyInstaPayFee = getAccountById(accountId)?.insta_pay_fee == true,
            isIncome = isIncome
        )
        deletePendingTransactionById(pendingId)
    }

    @Query("SELECT * FROM ai_conversations ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllConversationsFlow(): Flow<List<AiConversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversation)

    @Query("UPDATE ai_conversations SET title = :title, updatedAt = :updatedAt, isSynced = 0 WHERE id = :id")
    suspend fun updateConversationTitle(id: String, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ai_conversations SET isPinned = :isPinned, isSynced = 0 WHERE id = :id")
    suspend fun updateConversationPinned(id: String, isPinned: Boolean)

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)

    @Query("DELETE FROM ai_chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteChatMessagesForConversation(conversationId: String)

    @Query("SELECT * FROM ai_chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getChatMessagesForConversationFlow(conversationId: String): Flow<List<AiChatMessage>>

    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessagesFlow(): Flow<List<AiChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: AiChatMessage)

    @Query("DELETE FROM ai_chat_messages")
    suspend fun clearChatMessages()

    @Query("SELECT * FROM user_preferences")
    suspend fun getAllUserPreferencesSync(): List<UserPreferencesEntity>

    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    suspend fun getUserPreferencesByUserId(userId: String): UserPreferencesEntity?

    @Query("DELETE FROM user_preferences WHERE userId = :userId")
    suspend fun deleteUserPreferencesByUserId(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPreferences(userPreferences: UserPreferencesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPreferencesList(preferencesList: List<UserPreferencesEntity>)

    @Query("SELECT * FROM streak_dates")
    suspend fun getAllStreakDatesSync(): List<StreakDateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreakDate(streakDate: StreakDateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreakDates(streakDates: List<StreakDateEntity>)

    @Query("DELETE FROM streak_dates WHERE id = :id")
    suspend fun deleteStreakDateById(id: String)

    @Query("SELECT * FROM ai_conversations")
    suspend fun getAllAiConversationsSync(): List<AiConversation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiConversations(conversations: List<AiConversation>)

    @Query("SELECT * FROM ai_chat_messages")
    suspend fun getAllAiChatMessagesSync(): List<AiChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiChatMessages(messages: List<AiChatMessage>)
}
