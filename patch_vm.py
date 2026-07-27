import re

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val hasLanguageSelected = userPreferences.hasLanguageSelected',
    'val hasLanguageSelected = userPreferences.hasLanguageSelected\n    val hasHeardAboutUs = userPreferences.hasHeardAboutUs'
)

content = content.replace(
    '    fun completeLanguageSelection() {\n        viewModelScope.launch {\n            userPreferences.saveLanguageSelected(true)\n            PostHog.capture("language_selection_completed")\n        }\n    }',
    '    fun completeLanguageSelection() {\n        viewModelScope.launch {\n            userPreferences.saveLanguageSelected(true)\n            PostHog.capture("language_selection_completed")\n        }\n    }\n\n    fun completeHearAboutUs(source: String) {\n        viewModelScope.launch {\n            userPreferences.saveHeardAboutUs(true)\n            PostHog.capture("hear_about_us_answered", properties = mapOf("source" to source))\n            PostHog.set("hear_about_us_source", source)\n        }\n    }'
)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'w') as f:
    f.write(content)
