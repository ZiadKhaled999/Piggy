def fix_file(filename):
    with open(filename, 'r') as f:
        content = f.read()
    
    # We just replace any occurrence of 'import ' that doesn't have a newline before it?
    import re
    # Find instances of "import " or "package " that are preceded by a non-whitespace character
    content = re.sub(r'([^\s])(import androidx)', r'\1\nimport androidx', content)
    content = re.sub(r'([^\s])(import android\.)', r'\1\nimport android.', content)
    content = re.sub(r'([^\s])(package com\.)', r'\1\npackage com.', content)
    content = re.sub(r'([^\s])(import com\.)', r'\1\nimport com.', content)
    content = re.sub(r'([^\s])(import kotlinx\.)', r'\1\nimport kotlinx.', content)
    
    # Clean up multiple newlines if needed, but not strictly necessary
    with open(filename, 'w') as f:
        f.write(content)

fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/LoansScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt')
fix_file('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt')
