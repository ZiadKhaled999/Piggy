import re

def fix_file(filename):
    with open(filename, 'r') as f:
        content = f.read()
    
    # Insert newline before any 'import' that is preceded by an alphabetical character or '}' or '>' etc
    content = re.sub(r'([a-zA-Z0-9_\>\}])(import )', r'\1\n\2', content)
    content = re.sub(r'([a-zA-Z0-9_\>\}])(package )', r'\1\n\2', content)
    
    with open(filename, 'w') as f:
        f.write(content)

fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/LoansScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt')
