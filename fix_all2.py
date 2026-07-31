import re
def fix_file(filename):
    with open(filename, 'r') as f:
        content = f.read()
    
    # Insert newline before "import " if preceded by alphabetical char
    content = re.sub(r'([a-zA-Z])import ', r'\1\nimport ', content)
    content = re.sub(r'([a-zA-Z])package ', r'\1\npackage ', content)
    
    with open(filename, 'w') as f:
        f.write(content)

fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/LoansScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt')
