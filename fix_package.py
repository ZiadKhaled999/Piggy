import re

def fix_file(filename):
    with open(filename, 'r') as f:
        content = f.read()
    
    # Extract package line
    package_match = re.search(r'package [\w\.]+', content)
    if package_match:
        pkg = package_match.group(0)
        # Remove package from its current place
        content = content.replace(pkg, '')
        # Remove empty lines at start
        content = content.lstrip()
        # Put package at start
        content = pkg + '\n\n' + content
        
    with open(filename, 'w') as f:
        f.write(content)

fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/LoansScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt')
