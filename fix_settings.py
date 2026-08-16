import sys
import re

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Replace SettingsMode.BACKUP -> { ... } with SettingsMode.BACKUP -> { BackupSettingsView(...) }
# We need to find the block. It starts at `SettingsMode.BACKUP -> {` and ends at `SettingsMode.SECURITY -> {`

pattern = r'SettingsMode\.BACKUP -> \{.*?\}(?=\s*SettingsMode\.SECURITY -> \{)'
replacement = 'SettingsMode.BACKUP -> {\n                BackupSettingsView(viewModel = viewModel, isPremium = isPremium, createJsonLauncher = createDocumentLauncher)\n            }'

new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'w') as f:
    f.write(new_content)
