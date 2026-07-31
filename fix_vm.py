with open('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'r') as f:
    content = f.read()
with open('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'w') as f:
    f.write("import kotlinx.coroutines.flow.first\n" + content)
