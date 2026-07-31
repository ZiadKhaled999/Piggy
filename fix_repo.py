import os

file_path = "app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt"
with open(file_path, "r") as f:
    content = f.read()

# I will add triggerSync() right after insert/update methods
sync_method = """
    private fun triggerSync() {
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.oryno.piggy_ledger.service.SyncWorker>().build()
        androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            "SyncWork",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
"""

content = content.replace("class PiggyLedgerViewModel(application: Application) : AndroidViewModel(application) {", "class PiggyLedgerViewModel(application: Application) : AndroidViewModel(application) {" + sync_method)

methods_to_wrap = [
    "repository.insertGoal(",
    "repository.insertTransaction(",
    "repository.updateGoal(",
    "repository.deleteGoal(",
    "repository.deleteTransaction(",
    "repository.insertLoan(",
    "repository.updateLoan(",
    "repository.deleteLoan(",
    "repository.markLoanAsPaid(",
    "repository.insertLoanPayment(",
    "repository.deleteLoanPayment(",
    "repository.insertAccount(",
    "repository.updateAccount(",
    "repository.deleteAccount(",
    "repository.insertAccountTransaction(",
    "repository.deletePendingTransaction(",
    "repository.resolvePendingTransaction("
]

for m in methods_to_wrap:
    # replace repository.something(...) with { repository.something(...); triggerSync() }
    content = content.replace(m, m.replace("repository.", "repository.").replace("(", "(")) # wait regex is better

with open(file_path, "w") as f:
    f.write(content)
