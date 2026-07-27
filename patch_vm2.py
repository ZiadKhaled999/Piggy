with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '            PostHog.capture("hear_about_us_answered", properties = mapOf("source" to source))',
    '            PostHog.capture("hear_about_us_answered", properties = mapOf("source" to source, "$set" to mapOf("hear_about_us_source" to source)))'
)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'w') as f:
    f.write(content)
