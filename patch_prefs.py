import re

with open('./app/src/main/java/com/oryno/piggy_ledger/data/UserPreferences.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val HAS_LANGUAGE_SELECTED = booleanPreferencesKey("has_language_selected")',
    'val HAS_LANGUAGE_SELECTED = booleanPreferencesKey("has_language_selected")\n        val HAS_HEARD_ABOUT_US = booleanPreferencesKey("has_heard_about_us")'
)

content = content.replace(
    'val hasLanguageSelected: Flow<Boolean> = context.dataStore.data.map {\n        prefs -> prefs[HAS_LANGUAGE_SELECTED] ?: false\n    }',
    'val hasLanguageSelected: Flow<Boolean> = context.dataStore.data.map { prefs ->\n        prefs[HAS_LANGUAGE_SELECTED] ?: false\n    }\n\n    val hasHeardAboutUs: Flow<Boolean> = context.dataStore.data.map { prefs ->\n        prefs[HAS_HEARD_ABOUT_US] ?: false\n    }'
)

content = re.sub(
    r'(val hasLanguageSelected.*?\}).*?(val isAuthenticated)',
    r'\1\n\n    val hasHeardAboutUs: Flow<Boolean> = context.dataStore.data.map { prefs ->\n        prefs[HAS_HEARD_ABOUT_US] ?: false\n    }\n\n    \2',
    content,
    flags=re.DOTALL
)

content = re.sub(
    r'(suspend fun saveLanguageSelected.*?\}).*?(suspend fun saveAuthentication)',
    r'\1\n\n    suspend fun saveHeardAboutUs(completed: Boolean) {\n        context.dataStore.edit { prefs ->\n            prefs[HAS_HEARD_ABOUT_US] = completed\n        }\n    }\n\n    \2',
    content,
    flags=re.DOTALL
)

with open('./app/src/main/java/com/oryno/piggy_ledger/data/UserPreferences.kt', 'w') as f:
    f.write(content)
