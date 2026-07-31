import os
import re

def fix_file(file_path):
    if not os.path.exists(file_path):
        return
    with open(file_path, "r") as f:
        content = f.read()

    # Generic ones
    content = content.replace("account.id == 0L", 'account.id == ""')
    content = content.replace("account.id != 0L", 'account.id != ""')
    content = content.replace("AccountTransaction(id = 0L,", 'AccountTransaction(')
    content = content.replace("Account(id = 0L,", 'Account(')
    content = content.replace("LoanPayment(id = 0L,", 'LoanPayment(')
    content = content.replace("PendingTransaction(id = 0L,", 'PendingTransaction(')

    # PiggyLedgerViewModel.kt
    if "PiggyLedgerViewModel.kt" in file_path:
        content = content.replace("private val _selectedAccountId = MutableStateFlow<Long?>(null)", "private val _selectedAccountId = MutableStateFlow<String?>(null)")
        content = content.replace("private var _selectedAccountId = MutableStateFlow<Long?>(null)", "private var _selectedAccountId = MutableStateFlow<String?>(null)")
        content = content.replace("val selectedAccountId: StateFlow<Long?>", "val selectedAccountId: StateFlow<String?>")
        content = content.replace("fun selectAccount(id: Long?)", "fun selectAccount(id: String?)")
        
    # AccountsScreen.kt
    if "AccountsScreen.kt" in file_path:
        content = content.replace("onTransactionAdd: (Long) -> Unit", "onTransactionAdd: (String) -> Unit")
        content = content.replace("accountId = 0L", 'accountId = ""')

    # AddTransactionScreen.kt
    if "AddTransactionScreen.kt" in file_path:
        content = content.replace("account.id != 0L", 'account.id != ""')

    # DashboardScreen.kt
    if "DashboardScreen.kt" in file_path:
        content = content.replace("onAccountTransactionAdd: (Long) -> Unit", "onAccountTransactionAdd: (String) -> Unit")
        content = content.replace("onDeleteAccount: (Long) -> Unit", "onDeleteAccount: (String) -> Unit")
        content = content.replace("onAccountClick: (Long) -> Unit", "onAccountClick: (String) -> Unit")

    # PendingTransactionsScreen.kt
    if "PendingTransactionsScreen.kt" in file_path:
        content = content.replace("onResolve: (Long, Long) -> Unit", "onResolve: (String, String) -> Unit")
        content = content.replace("onDelete: (Long) -> Unit", "onDelete: (String) -> Unit")

    # PiggyLedgerApp.kt
    if "PiggyLedgerApp.kt" in file_path:
        content = content.replace("it.arguments?.getLong(\"accountId\") ?: 0L", "it.arguments?.getString(\"accountId\") ?: \"\"")
        content = content.replace("type = NavType.LongType", "type = NavType.StringType")

    # UserPreferences.kt
    if "UserPreferences.kt" in file_path:
        content = content.replace("it[LATEST_ACCOUNT_ID] ?: 0L", 'it[LATEST_ACCOUNT_ID] ?: ""')
        content = content.replace("val LATEST_ACCOUNT_ID = longPreferencesKey(\"latest_account_id\")", "val LATEST_ACCOUNT_ID = stringPreferencesKey(\"latest_account_id\")")
        content = content.replace("suspend fun saveLatestAccountId(id: Long)", "suspend fun saveLatestAccountId(id: String)")

    with open(file_path, "w") as f:
        f.write(content)

for root, _, files in os.walk("app/src/main/java"):
    for file in files:
        if file.endswith(".kt"):
            fix_file(os.path.join(root, file))

