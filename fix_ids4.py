import os
import re

def fix_file(file_path):
    if not os.path.exists(file_path):
        return
    with open(file_path, "r") as f:
        content = f.read()

    # AccountsScreen.kt
    if "AccountsScreen.kt" in file_path:
        content = content.replace("onNavigateToEditAccount: (Long) -> Unit", "onNavigateToEditAccount: (String) -> Unit")

    # AddTransactionScreen.kt
    if "AddTransactionScreen.kt" in file_path:
        content = content.replace("val accountId: Long?", "val accountId: String?")

    # DashboardScreen.kt
    if "DashboardScreen.kt" in file_path:
        content = content.replace("onNavigateToEditAccount: (Long) -> Unit", "onNavigateToEditAccount: (String) -> Unit")

    # PiggyLedgerApp.kt
    if "PiggyLedgerApp.kt" in file_path:
        content = content.replace("onNavigateToEditAccount = { accountId: Long ->", "onNavigateToEditAccount = { accountId: String ->")

    # PiggyLedgerViewModel.kt
    if "PiggyLedgerViewModel.kt" in file_path:
        content = content.replace("MutableStateFlow<Long?>", "MutableStateFlow<String?>")
        content = content.replace("StateFlow<Long?>", "StateFlow<String?>")

    with open(file_path, "w") as f:
        f.write(content)

for root, _, files in os.walk("app/src/main/java"):
    for file in files:
        if file.endswith(".kt"):
            fix_file(os.path.join(root, file))

