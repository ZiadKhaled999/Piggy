import os
import re

def fix_file(file_path):
    if not os.path.exists(file_path):
        return
    with open(file_path, "r") as f:
        content = f.read()

    # Generic replacements
    content = content.replace("selectedAccountId: Long?", "selectedAccountId: String?")
    content = content.replace("accountId: Long?", "accountId: String?")
    content = content.replace("accountId: Long", "accountId: String")
    content = content.replace("pendingId: Long", "pendingId: String")
    content = content.replace("id = 0L", "id = \"\"")
    content = content.replace("?: 0L", "?: \"\"")
    content = content.replace("account_id = accountId ?: 0L", "account_id = accountId ?: \"\"")
    
    # Specifics for PiggyLedgerViewModel
    if "PiggyLedgerViewModel.kt" in file_path:
        content = content.replace("private var _selectedAccountId = MutableStateFlow<Long?>(null)", "private var _selectedAccountId = MutableStateFlow<String?>(null)")
        content = content.replace("val selectedAccountId: StateFlow<Long?>", "val selectedAccountId: StateFlow<String?>")
        content = content.replace("fun selectAccount(id: Long?)", "fun selectAccount(id: String?)")

    # Specifics for Screens.kt
    if "Screens.kt" in file_path:
        content = re.sub(r'val\s+accountId:\s+Long', 'val accountId: String', content)
        content = re.sub(r'val\s+accountId:\s+Long\?', 'val accountId: String?', content)

    # Specifics for PiggyLedgerApp.kt
    if "PiggyLedgerApp.kt" in file_path:
        content = content.replace("it.arguments?.getLong(\"accountId\") ?: 0L", "it.arguments?.getString(\"accountId\") ?: \"\"")
        content = content.replace("it.arguments?.getLong(\"accountId\")", "it.arguments?.getString(\"accountId\")")
        content = content.replace("type = NavType.LongType", "type = NavType.StringType")
        
    with open(file_path, "w") as f:
        f.write(content)

for root, _, files in os.walk("app/src/main/java"):
    for file in files:
        if file.endswith(".kt"):
            fix_file(os.path.join(root, file))

