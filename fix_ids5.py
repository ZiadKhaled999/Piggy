import os
import re

def fix_file(file_path):
    if not os.path.exists(file_path):
        return
    with open(file_path, "r") as f:
        content = f.read()

    # AddTransactionScreen.kt
    if "AddTransactionScreen.kt" in file_path:
        content = content.replace("account.id != 0L", 'account.id != ""')

    # DashboardScreen.kt
    if "DashboardScreen.kt" in file_path:
        content = content.replace("onAccountTransactionAdd(0L)", 'onAccountTransactionAdd("")')
        content = content.replace("onAccountClick(0L)", 'onAccountClick("")')
        content = content.replace("onNavigateToEditAccount = {}", 'onNavigateToEditAccount = {}') # Need to see line 768 in DashboardScreen
        content = content.replace("onNavigateToEditAccount = { onNavigateToEditAccount(0L) }", 'onNavigateToEditAccount = { onNavigateToEditAccount("") }')

    # PendingTransactionsScreen.kt
    if "PendingTransactionsScreen.kt" in file_path:
        content = content.replace("onResolve(pending.id, 0L)", 'onResolve(pending.id, "")')
        content = content.replace("onDelete: (Long) -> Unit", "onDelete: (String) -> Unit")

    # PiggyLedgerViewModel.kt
    if "PiggyLedgerViewModel.kt" in file_path:
        # line 538: Argument type mismatch: actual type is 'Unit', but 'String' was expected.
        # This was probably due to `val newId = repository.insertAccount(newAccount)` returning Unit, but we try to use newId.
        pass

    with open(file_path, "w") as f:
        f.write(content)

for root, _, files in os.walk("app/src/main/java"):
    for file in files:
        if file.endswith(".kt"):
            fix_file(os.path.join(root, file))

