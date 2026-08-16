import sys

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

import re

# Remove the openDocumentLauncher definition
content = re.sub(r'    val openDocumentLauncher = rememberLauncherForActivityResult\(\s*contract = ActivityResultContracts\.OpenDocument\(\)\s*\) \{ uri ->.*?\}\n\s*\}\n', '', content, flags=re.DOTALL)

# Remove the parameter passed to DetailSettingsView
content = re.sub(r',\s*openDocumentLauncher\s*=\s*openDocumentLauncher', '', content)

# Remove the parameter from DetailSettingsView signature
content = re.sub(r',\s*openDocumentLauncher:\s*androidx\.activity\.result\.ActivityResultLauncher<Array<String>>', '', content)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)
