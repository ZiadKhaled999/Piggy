with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '            PostHog.set("hear_about_us_source", source)\n',
    ''
)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'w') as f:
    f.write(content)
