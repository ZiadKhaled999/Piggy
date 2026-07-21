import os
import re

files_to_update = {
    "app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt": [
        ('Toast.makeText(context, "Data exported successfully"', 'Toast.makeText(context, context.getString(R.string.export_success)'),
        ('Toast.makeText(context, "Export failed: ${e.message}"', 'Toast.makeText(context, context.getString(R.string.export_failed, e.message ?: ""))'),
        ('Toast.makeText(context, "Data restored successfully"', 'Toast.makeText(context, context.getString(R.string.restore_success)'),
        ('Toast.makeText(context, "Restore failed: $error"', 'Toast.makeText(context, context.getString(R.string.restore_failed, error))'),
        ('Toast.makeText(context, "Failed to read file: ${e.message}"', 'Toast.makeText(context, context.getString(R.string.read_file_failed, e.message ?: ""))'),
        ('Toast.makeText(context, "No browser found to open link."', 'Toast.makeText(context, context.getString(R.string.browser_error)'),
        ('Toast.makeText(context, "No email client installed."', 'Toast.makeText(context, context.getString(R.string.email_error)'),
        ('Toast.makeText(context, "CSV exported successfully"', 'Toast.makeText(context, context.getString(R.string.csv_export_success)'),
        ('Toast.makeText(context, "CSV export failed: ${e.message}"', 'Toast.makeText(context, context.getString(R.string.csv_export_failed, e.message ?: ""))'),
        ('Toast.makeText(context, "Excel exported successfully"', 'Toast.makeText(context, context.getString(R.string.excel_export_success)'),
        ('Toast.makeText(context, "Excel export failed: ${e.message}"', 'Toast.makeText(context, context.getString(R.string.excel_export_failed, e.message ?: ""))'),
        ('Text("Export Beautiful Excel"', 'Text(stringResource(R.string.export_excel_title)'),
        ('Text("Export CSV File"', 'Text(stringResource(R.string.export_csv_title)'),
        ('Toast.makeText(context, "Data restored successfully from CSV"', 'Toast.makeText(context, context.getString(R.string.csv_restore_success)'),
        ('Toast.makeText(context, "CSV import failed: $error"', 'Toast.makeText(context, context.getString(R.string.csv_restore_failed, error))'),
        ('Toast.makeText(context, "CSV import failed: ${e.message}"', 'Toast.makeText(context, context.getString(R.string.csv_restore_failed, e.message ?: ""))'),
        ('Text("Select CSV File"', 'Text(stringResource(R.string.select_csv_file)'),
        ('Text("Save")', 'Text(stringResource(R.string.save))')
    ],
    "app/src/main/java/com/oryno/piggy_ledger/ui/AccountsScreen.kt": [
        ('Text("Close", color = Color.White', 'Text(stringResource(R.string.close), color = Color.White')
    ],
    "app/src/main/java/com/oryno/piggy_ledger/ui/components/VoiceRecordButton.kt": [
        ('Toast.makeText(context, "Microphone permission is required to record voice"', 'Toast.makeText(context, context.getString(R.string.mic_permission_required)')
    ],
    "app/src/main/java/com/oryno/piggy_ledger/ui/EditAccountScreen.kt": [
        ('Text("e.g. 1234 5678 9012"', 'Text(stringResource(R.string.card_number_placeholder)')
    ],
    "app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt": [
        ('Text("Spent"', 'Text(stringResource(R.string.spent)'),
        ('Text("Payoffs"', 'Text(stringResource(R.string.payoffs)'),
        ('Text("Savings Goals"', 'Text(stringResource(R.string.savings_goals)'),
        ('Text("See all"', 'Text(stringResource(R.string.see_all)'),
        ('Text("Processing your voice..."', 'Text(stringResource(R.string.processing_voice)'),
        ('Text("Correct Transaction"', 'Text(stringResource(R.string.correct_transaction)'),
        ('Text("Cancel")', 'Text(stringResource(R.string.cancel))'),
        ('Text("Re-Process")', 'Text(stringResource(R.string.re_process))'),
        ('Text("Transaction Details"', 'Text(stringResource(R.string.transaction_details)'),
        ('Text("You said:"', 'Text(stringResource(R.string.you_said)'),
        ('Text("Amount"', 'Text(stringResource(R.string.amount)'),
        ('Text("Target (Account / Goal)"', 'Text(stringResource(R.string.target_account_goal)'),
        ('text = { Text("Account: ${acc.name}") }', 'text = { Text(stringResource(R.string.account_name_format, acc.name)) }'),
        ('text = { Text("Goal: ${goal.name}") }', 'text = { Text(stringResource(R.string.goal_name_format, goal.name)) }'),
        ('Text("Correct")', 'Text(stringResource(R.string.correct))'),
        ('Text("Go Ahead")', 'Text(stringResource(R.string.go_ahead))'),
        ('Text("No goals set yet. Tap to start saving!"', 'Text(stringResource(R.string.no_goals_set)')
    ],
    "app/src/main/java/com/oryno/piggy_ledger/ui/AuthScreen.kt": [
        ('text = "Piggy Ledger"', 'text = stringResource(R.string.auth_welcome_title_main)'),
        ('text = "Secure, simple, and smart."', 'text = stringResource(R.string.auth_welcome_subtitle_main)'),
        ('text = "Track your expenses, set goals, and save money effortlessly."', 'text = stringResource(R.string.auth_welcome_desc)'),
        ('text = "Continue with Google"', 'text = stringResource(R.string.auth_continue_google)')
    ],
    "app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt": [
        ('Toast.makeText(context, "Please select an option to proceed"', 'Toast.makeText(context, context.getString(R.string.please_select_option)')
    ]
}

for file_path, replacements in files_to_update.items():
    if not os.path.exists(file_path):
        print(f"Skipping {file_path}, does not exist.")
        continue
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)

print("Done replacing.")
