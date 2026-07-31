import sys

def fix_file(filename):
    with open(filename, 'r') as f:
        content = f.read()
    
    first_newline = content.find('\n')
    if first_newline != -1:
        first_line = content[:first_newline]
        rest = content[first_newline:]
    else:
        first_line = content
        rest = ""

    # Add newlines before "import " and "package "
    first_line = first_line.replace('import ', '\nimport ')
    first_line = first_line.replace('package ', '\npackage ')
    
    with open(filename, 'w') as f:
        f.write(first_line + rest)

fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/LoansScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt')
