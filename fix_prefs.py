with open('./app/src/main/java/com/oryno/piggy_ledger/data/UserPreferences.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '    suspend fun saveLanguageSelected(selected: Boolean) {\n        context.dataStore.edit { prefs ->\n            prefs[HAS_LANGUAGE_SELECTED] = selected\n        }\n\n    suspend fun saveHeardAboutUs(completed: Boolean) {',
    '    suspend fun saveLanguageSelected(selected: Boolean) {\n        context.dataStore.edit { prefs ->\n            prefs[HAS_LANGUAGE_SELECTED] = selected\n        }\n    }\n\n    suspend fun saveHeardAboutUs(completed: Boolean) {'
)

with open('./app/src/main/java/com/oryno/piggy_ledger/data/UserPreferences.kt', 'w') as f:
    f.write(content)
