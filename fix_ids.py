import os
import re

files_to_fix = [
    "app/src/main/java/com/oryno/piggy_ledger/ui/AccountsScreen.kt",
    "app/src/main/java/com/oryno/piggy_ledger/ui/AddTransactionScreen.kt",
    "app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt",
    "app/src/main/java/com/oryno/piggy_ledger/ui/EditAccountScreen.kt",
    "app/src/main/java/com/oryno/piggy_ledger/ui/LoansScreen.kt",
    "app/src/main/java/com/oryno/piggy_ledger/ui/PendingTransactionsScreen.kt",
    "app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt",
    "app/src/main/java/com/oryno/piggy_ledger/ui/Screens.kt"
]

for file in files_to_fix:
    if not os.path.exists(file):
        continue
    with open(file, "r") as f:
        content = f.read()

    # Screens.kt might have Long arguments in routes
    if file.endswith("Screens.kt"):
        content = content.replace("val accountId: Long", "val accountId: String")
        content = content.replace("val accountId: Long?", "val accountId: String?")

    # Viewmodel replacements
    if "PiggyLedgerViewModel.kt" in file:
        content = content.replace("fun deleteAccount(id: Long)", "fun deleteAccount(id: String)")
        content = content.replace("fun getAccountById(id: Long", "fun getAccountById(id: String")
        content = content.replace("fun deleteLoanPayment(id: Long)", "fun deleteLoanPayment(id: String)")
        content = content.replace("fun deletePendingTransaction(id: Long)", "fun deletePendingTransaction(id: String)")
        content = content.replace("fun resolvePendingTransaction(pendingId: Long, accountId: Long)", "fun resolvePendingTransaction(pendingId: String, accountId: String)")
        content = content.replace("accountId: Long", "accountId: String")
        content = content.replace("pendingId: Long", "pendingId: String")
        content = re.sub(r'val newId = repository\.insertAccount\(([^)]+)\)', r'repository.insertAccount(\1)\n            val newId = \1.id', content)
        content = re.sub(r'val newId = repository\.insertPendingTransaction\(([^)]+)\)', r'repository.insertPendingTransaction(\1)\n            val newId = \1.id', content)

    # UI Screens
    content = content.replace("accountId: Long?", "accountId: String?")
    content = content.replace("accountId: Long", "accountId: String")
    content = content.replace("selectedAccountId: Long?", "selectedAccountId: String?")
    content = content.replace("selectedAccountId: Long", "selectedAccountId: String")
    content = content.replace("account_id = accountId ?: 0L", "account_id = accountId ?: \"\"")
    content = content.replace("account_id = accountId", "account_id = accountId")
    content = content.replace("val pendingId: Long", "val pendingId: String")
    content = content.replace("id = 0L", 'id = ""') # if they create new object

    with open(file, "w") as f:
        f.write(content)

