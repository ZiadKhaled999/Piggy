with open("app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt", "r") as f:
    content = f.read()

content = content.replace('context.getString(R.string.csv_export_failed, e.message ?: "")), Toast.LENGTH_LONG).show()', 'context.getString(R.string.csv_export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()')
content = content.replace('context.getString(R.string.excel_export_failed, e.message ?: "")), Toast.LENGTH_LONG).show()', 'context.getString(R.string.excel_export_failed, e.message ?: ""), Toast.LENGTH_LONG).show()')
content = content.replace('context.getString(R.string.csv_restore_failed, error)), Toast.LENGTH_LONG).show()', 'context.getString(R.string.csv_restore_failed, error), Toast.LENGTH_LONG).show()')
content = content.replace('context.getString(R.string.csv_restore_failed, e.message ?: "")), Toast.LENGTH_LONG).show()', 'context.getString(R.string.csv_restore_failed, e.message ?: ""), Toast.LENGTH_LONG).show()')

with open("app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt", "w") as f:
    f.write(content)
