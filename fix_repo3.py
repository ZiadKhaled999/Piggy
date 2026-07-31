import os

file_path = "app/src/main/java/com/oryno/piggy_ledger/data/PiggyLedgerRepository.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("class PiggyLedgerRepository(private val dao: PiggyLedgerDao) {", "class PiggyLedgerRepository(private val dao: PiggyLedgerDao, private val context: android.content.Context) {\n\n    private fun triggerSync() {\n        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.oryno.piggy_ledger.service.SyncWorker>().build()\n        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(\n            \"SyncWork\",\n            androidx.work.ExistingWorkPolicy.REPLACE,\n            workRequest\n        )\n    }\n")

# Now wrap all dao calls with triggerSync
methods_to_wrap = [
    "suspend fun insertGoal(goal: Goal) = dao.insertGoal(goal)",
    "suspend fun insertTransaction(transaction: Transaction) = dao.insertTransaction(transaction)",
    "suspend fun updateGoal(goal: Goal) = dao.updateGoal(goal)",
    "suspend fun deleteGoal(id: String) = dao.deleteGoalById(id)",
    "suspend fun deleteTransaction(id: String) = dao.deleteTransactionById(id)",
    "suspend fun insertLoan(loan: Loan) = dao.insertLoan(loan)",
    "suspend fun updateLoan(loan: Loan) = dao.updateLoan(loan)",
    "suspend fun deleteLoan(id: String) = dao.deleteLoanById(id)",
    "suspend fun insertLoanPayment(payment: LoanPayment) = dao.insertLoanPayment(payment)",
    "suspend fun deleteLoanPayment(id: String) = dao.deleteLoanPaymentById(id)",
    "suspend fun insertAccount(account: Account) = dao.insertAccount(account)",
    "suspend fun updateAccount(account: Account) = dao.updateAccount(account)",
    "suspend fun deleteAccount(id: String) = dao.deleteAccountById(id)",
    "suspend fun insertAccountTransaction(transaction: AccountTransaction) = dao.insertAccountTransaction(transaction)",
    "suspend fun insertPendingTransaction(transaction: PendingTransaction) = dao.insertPendingTransaction(transaction)",
    "suspend fun deletePendingTransaction(id: String) = dao.deletePendingTransactionById(id)"
]

for m in methods_to_wrap:
    new_m = m.split(" = ")[0] + " { " + m.split(" = ")[1] + "; triggerSync() }"
    content = content.replace(m, new_m)

# For methods that are not one-liners like markLoanAsPaid
content = content.replace("dao.updateLoan(loan.copy(isPaidOff = true))\n        }", "dao.updateLoan(loan.copy(isPaidOff = true))\n            triggerSync()\n        }")
content = content.replace("dao.resolvePendingTransaction(pendingId, accountId)", "{ dao.resolvePendingTransaction(pendingId, accountId); triggerSync() }")

with open(file_path, "w") as f:
    f.write(content)

# MainActivity.kt
main_path = "app/src/main/java/com/oryno/piggy_ledger/MainActivity.kt"
with open(main_path, "r") as f:
    content = f.read()

content = content.replace("val repository = PiggyLedgerRepository(database.piggyLedgerDao())", "val repository = PiggyLedgerRepository(database.piggyLedgerDao(), applicationContext)")
with open(main_path, "w") as f:
    f.write(content)

