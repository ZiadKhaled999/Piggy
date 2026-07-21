with open("app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

content = content.replace('context.getString(R.string.csv_export_failed, e.message ?: ""))),', 'context.getString(R.string.csv_export_failed, e.message ?: "")),')
content = content.replace('context.getString(R.string.excel_export_failed, e.message ?: ""))),', 'context.getString(R.string.excel_export_failed, e.message ?: "")),')
content = content.replace('context.getString(R.string.csv_restore_failed, error))),', 'context.getString(R.string.csv_restore_failed, error)),')
content = content.replace('context.getString(R.string.csv_restore_failed, e.message ?: ""))),', 'context.getString(R.string.csv_restore_failed, e.message ?: "")),')

with open("app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/oryno/piggy_ledger/ui/AuthScreen.kt", "r") as f:
    auth_content = f.read()

auth_content = auth_content.replace('import androidx.compose.ui.res.stringResource\npackage com.oryno.piggy_ledger.ui', 'package com.oryno.piggy_ledger.ui\nimport androidx.compose.ui.res.stringResource')

with open("app/src/main/java/com/oryno/piggy_ledger/ui/AuthScreen.kt", "w") as f:
    f.write(auth_content)
