package com.example.data

import kotlinx.coroutines.flow.Flow

class PiggyLedgerRepository(private val dao: PiggyLedgerDao) {
    val allGoals: Flow<List<Goal>> = dao.getAllGoals()
    val allLoans: Flow<List<Loan>> = dao.getAllLoans()

    fun getGoalById(id: String) = dao.getGoalById(id)
    fun getTransactionsForGoal(id: String) = dao.getTransactionsForGoal(id)

    suspend fun insertGoal(goal: Goal) = dao.insertGoal(goal)
    suspend fun insertTransaction(transaction: Transaction) = dao.insertTransaction(transaction)
    
    suspend fun insertLoan(loan: Loan) = dao.insertLoan(loan)
    suspend fun markLoanAsPaid(id: String) {
        val loan = dao.getLoanById(id)
        if (loan != null) {
            dao.updateLoan(loan.copy(isPaidOff = true))
        }
    }
    suspend fun deleteLoan(id: String) = dao.deleteLoanById(id)
}
