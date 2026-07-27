with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '    val hasLanguageSelected = userPreferences.hasLanguageSelected\n    val hasHeardAboutUs = userPreferences.hasHeardAboutUs.stateIn(',
    '    val hasLanguageSelected = userPreferences.hasLanguageSelected.stateIn(\n        viewModelScope, SharingStarted.WhileSubscribed(5000), null\n    )\n    val hasHeardAboutUs = userPreferences.hasHeardAboutUs.stateIn('
)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'w') as f:
    f.write(content)
