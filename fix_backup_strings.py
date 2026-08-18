import re

with open('app/src/main/java/com/oryno/piggy_ledger/ui/BackupSettingsView.kt', 'r') as f:
    content = f.read()

replacements = [
    ('text = "Export data"', 'text = stringResource(R.string.export_data_title)'),
    ('text = "Export your data for external spreadsheets."', 'text = stringResource(R.string.export_data_subtitle)'),
    ('"Include Pending Transactions"', 'stringResource(R.string.export_include_pending)'),
    ('"Include Account Balances"', 'stringResource(R.string.export_include_balances)'),
    ('"Include Goal History"', 'stringResource(R.string.export_include_goals)'),
    ('Text("EXPORT",', 'Text(stringResource(R.string.export_action),'),
    ('text = "FOUND ${transactions.size} TRANSACTIONS"', 'text = stringResource(R.string.export_found_transactions, transactions.size)'),
    ('"Upgrade to Pro to export your data"', 'stringResource(R.string.export_upgrade_pro)'),
    ('"Data exported successfully to CSV"', 'stringResource(R.string.export_success, "CSV")'),
    ('"Data exported successfully to EXCEL"', 'stringResource(R.string.export_success, "EXCEL")'),
    ('"Data exported successfully to JSON"', 'stringResource(R.string.export_success, "JSON")'),
    ('"Export failed: ${e.message}"', 'stringResource(R.string.export_failed, e.message.toString())')
]

for old, new in replacements:
    content = content.replace(old, new)

if "import androidx.compose.ui.res.stringResource" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.res.stringResource\nimport com.oryno.piggy_ledger.R")

with open('app/src/main/java/com/oryno/piggy_ledger/ui/BackupSettingsView.kt', 'w') as f:
    f.write(content)

print("Replaced strings in BackupSettingsView")
