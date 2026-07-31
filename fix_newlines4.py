import re

def fix_file(filename):
    with open(filename, 'r') as f:
        content = f.read()
    
    # We want to insert a newline before every 'import ' and 'package ' if there isn't one already.
    # We can just replace 'import ' with '\nimport ' and 'package ' with '\npackage '.
    content = content.replace('import ', '\nimport ')
    content = content.replace('package ', '\npackage ')
    
    # Then we can remove all double newlines
    while '\n\n\n' in content:
        content = content.replace('\n\n\n', '\n\n')
        
    with open(filename, 'w') as f:
        f.write(content)

fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/LoansScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt')
