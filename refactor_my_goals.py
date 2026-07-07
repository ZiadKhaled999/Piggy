import re

def update_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    replacements = [
        ('text = "OPEN",', 'text = stringResource(R.string.open_badge),'),
        ('"$${String.format("%.2f", savedAmount)} saved"', 'stringResource(R.string.amount_saved, String.format("%.2f", savedAmount))'),
        ('"$${String.format("%.2f", savedAmount)} total ($${String.format("%.2f", -remaining)} extra)"', 'stringResource(R.string.amount_total_extra, String.format("%.2f", savedAmount), String.format("%.2f", -remaining))'),
        ('"$${String.format("%.2f", remaining)} left"', 'stringResource(R.string.remaining_left, String.format("%.2f", remaining))')
    ]

    for old, new in replacements:
        content = content.replace(old, new)

    with open(filepath, 'w') as f:
        f.write(content)

update_file('app/src/main/java/com/oryno/piggy_ledger/ui/MyGoalsScreen.kt')
