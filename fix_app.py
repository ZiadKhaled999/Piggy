with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerApp.kt', 'r') as f:
    content = f.read()

# I need to find `val hasLanguageSelected by viewModel.hasLanguageSelected.collectAsState()` and append `hasHeardAboutUs`
# Let's just fix it properly.
import re
content = re.sub(r'val hasLanguageSelected by viewModel.hasLanguageSelected.collectAsState\(\)', 'val hasLanguageSelected by viewModel.hasLanguageSelected.collectAsState()\n    val hasHeardAboutUs by viewModel.hasHeardAboutUs.collectAsState()', content)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerApp.kt', 'w') as f:
    f.write(content)
