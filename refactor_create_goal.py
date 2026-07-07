import re

def update_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    replacements = [
        ('text = "Target Goal",', 'text = stringResource(R.string.target_goal_title),'),
        ('text = "Save for a specific target amount.",', 'text = stringResource(R.string.target_goal_desc),'),
        ('text = "Open Savings",', 'text = stringResource(R.string.open_savings),'),
        ('text = "Deposit just for saving (No target limit).",', 'text = stringResource(R.string.open_savings_desc),'),
        ('text = "💡 Hint: Opened balance is for general/indefinite savings. You can deposit money here anytime for savings without setting any specific limit.",', 'text = stringResource(R.string.open_savings_hint),')
    ]

    for old, new in replacements:
        content = content.replace(old, new)

    with open(filepath, 'w') as f:
        f.write(content)

update_file('app/src/main/java/com/oryno/piggy_ledger/ui/CreateGoalScreen.kt')
