with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '            PostHog.capture("language_selection_completed")',
    '            val currentLang = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()\n            PostHog.capture("language_selection_completed", properties = mapOf("$set" to mapOf("language" to currentLang)))'
)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'w') as f:
    f.write(content)
