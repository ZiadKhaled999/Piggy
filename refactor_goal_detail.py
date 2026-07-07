import re

def update_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Add import if missing
    if 'import androidx.compose.ui.res.stringResource' not in content:
        content = content.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*\nimport androidx.compose.ui.res.stringResource\nimport com.oryno.piggy_ledger.R')

    replacements = [
        ('text = "BUDGETING",', 'text = stringResource(R.string.budgeting_title),'),
        ('text = "TOTAL BALANCE",', 'text = stringResource(R.string.total_balance_label),'),
        ('"+$$${String.format("%.2f", savedAmount - goal.targetAmount)} EXTRA"', 'stringResource(R.string.extra_amount, String.format("%.2f", savedAmount - goal.targetAmount))'),
        ('" (Open Savings)"', ' " (" + stringResource(R.string.open_savings) + ")"'),
        ('Text("YOUR BUDGET",', 'Text(stringResource(R.string.your_budget_title),'),
        ('text = if (isCompleted) "COMPLETED" else if (goal.targetAmount <= 0.0) "OPEN SAVINGS" else "IN PROGRESS",', 'text = if (isCompleted) stringResource(R.string.completed_status) else if (goal.targetAmount <= 0.0) stringResource(R.string.open_savings).uppercase() else stringResource(R.string.in_progress_status),'),
        ('saved (Open Savings)"', 'saved (" + stringResource(R.string.open_savings) + ")"'),
        ('"Goal reached"', 'stringResource(R.string.goal_reached_status)'),
        ('"$$${String.format("%.2f", -remaining)} extra"', '"$$" + String.format("%.2f", -remaining) + " extra"'), # Fixed later if needed
        ('"$$${String.format("%.2f", remaining)} left"', 'stringResource(R.string.remaining_left, String.format("%.2f", remaining))'),
        ('Text("Add Deposit",', 'Text(stringResource(R.string.add_deposit),'),
        ('Text("Goal Completed",', 'Text(stringResource(R.string.goal_completed_msg),'),
        ('TabButton(text = "Overview",', 'TabButton(text = stringResource(R.string.overview_tab),'),
        ('TabButton(text = "History",', 'TabButton(text = stringResource(R.string.history_tab),'),
        ('Text("Add Deposit",', 'Text(stringResource(R.string.add_deposit),'),
        ('Text(\n                    "Add Deposit",', 'Text(\n                    stringResource(R.string.add_deposit),'),
        ('Text("GROWTH YOUR SAVINGS BALANCE",', 'Text(stringResource(R.string.growth_savings_subtitle),'),
        ('label = { Text("DEPOSIT AMOUNT",', 'label = { Text(stringResource(R.string.deposit_amount_label),'),
        ('placeholder = { Text("$$ 0.00") }', 'placeholder = { Text(stringResource(R.string.zero_amount_placeholder)) }'),
        ('label = { Text("NOTE (REQUIRED)",', 'label = { Text(stringResource(R.string.note_required_label),'),
        ('placeholder = { Text("e.g. Monthly contribution") }', 'placeholder = { Text(stringResource(R.string.monthly_contribution_placeholder)) }'),
        ('Text("Confirm Deposit",', 'Text(stringResource(R.string.confirm_deposit_btn),'),
        ('MetadataCard(label = "ESTABLISHED DATE",', 'MetadataCard(label = stringResource(R.string.established_date_label),'),
        ('MetadataCard(label = "DAYS SINCE START",', 'MetadataCard(label = stringResource(R.string.days_since_start_label),'),
        ('MetadataCard(label = "AVG. DAILY SAVING",', 'MetadataCard(label = stringResource(R.string.avg_daily_saving_label),'),
        ('MetadataCard(\n                label = "EST. COMPLETION DATE",', 'MetadataCard(\n                label = stringResource(R.string.est_completion_date_label),'),
        ('"Goal Reached!"', 'stringResource(R.string.goal_reached_success)'),
        ('Text("Savings Challenge",', 'Text(stringResource(R.string.savings_challenge_title),'),
        ('Text("Milestones",', 'Text(stringResource(R.string.milestones_title),'),
        ('"You\'ve made ${transactions.size} contributions so far. Keep it up!"', 'stringResource(R.string.contributions_count_msg, transactions.size)'),
        ('"Deposit"', 'stringResource(R.string.deposit_tx_note)'),
        ('value = "$daysRunning days",', 'value = stringResource(R.string.days_count, daysRunning),')
    ]

    for old, new in replacements:
        content = content.replace(old, new)

    with open(filepath, 'w') as f:
        f.write(content)

update_file('app/src/main/java/com/oryno/piggy_ledger/ui/GoalDetailScreen.kt')
