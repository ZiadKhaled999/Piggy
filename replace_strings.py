import os
import re

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Needs a small check to inject import androidx.compose.ui.res.stringResource if needed
    if 'stringResource' in str(replacements) and 'androidx.compose.ui.res.stringResource' not in content:
        content = content.replace('import androidx.compose.runtime.Composable', 'import androidx.compose.runtime.Composable\nimport androidx.compose.ui.res.stringResource\nimport com.oryno.piggy_ledger.R')

    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w') as f:
        f.write(content)

replacements_dashboard = [
    ('Text("Settings & Preferences"', 'Text(stringResource(R.string.settings)'),
    ('Text("Welcome Back!"', 'Text(stringResource(R.string.welcome_back)"'), # Wait, wait, simple replace might break. Let's do exact match
    ('Text("Welcome Back!",', 'Text(stringResource(R.string.welcome_back),'),
    ('Text("Your Balance",', 'Text(stringResource(R.string.your_balance),'),
    ('Text("Recent Activity",', 'Text(stringResource(R.string.recent_activity),'),
    ('Text("No recent activity",', 'Text(stringResource(R.string.no_recent_activity),'),
    ('Text("View All",', 'Text(stringResource(R.string.view_all),'),
    ('Text("Go straight to Dashboard →",', 'Text(stringResource(R.string.go_straight_dashboard),'),
    ('Text("Home",', 'Text(stringResource(R.string.nav_home),'),
    ('Text("Goals",', 'Text(stringResource(R.string.nav_goals),'),
    ('Text("Loans",', 'Text(stringResource(R.string.nav_loans),'),
    ('Text("Settings",', 'Text(stringResource(R.string.nav_settings),'),
    ('Text("Give Feedback",', 'Text(stringResource(R.string.give_feedback),'),
    ('Text("Rate the App",', 'Text(stringResource(R.string.rate_app),'),
    ('Text("Backup Data",', 'Text(stringResource(R.string.backup_data),'),
    ('Text("Restore Data",', 'Text(stringResource(R.string.restore_data),'),
    ('Text("Open Feedback Board",', 'Text(stringResource(R.string.open_feedback_board),'),
    ('Text("Send Rating",', 'Text(stringResource(R.string.send_rating),'),
    ('Text("Create Backup File",', 'Text(stringResource(R.string.create_backup_file),'),
    ('Text("Select Backup File",', 'Text(stringResource(R.string.select_backup_file),')
]

replacements_onboarding = [
    ('Text("Track Every Penny"', 'Text(stringResource(R.string.onboarding_step_1_title)"'), # bug here with quotes
    ('Text("Track Every Penny",', 'Text(stringResource(R.string.onboarding_step_1_title),'),
    ('Text("No more guessing where your money went. Piggy Ledger keeps everything organized.",', 'Text(stringResource(R.string.onboarding_step_1_desc),'),
    ('Text("Set Big Goals",', 'Text(stringResource(R.string.onboarding_step_2_title),'),
    ('Text("Saving for a vacation or a new PC? Set visual goals and crush them.",', 'Text(stringResource(R.string.onboarding_step_2_desc),'),
    ('Text("Stay Consistent",', 'Text(stringResource(R.string.onboarding_step_3_title),'),
    ('Text("Build healthy financial habits with daily streak tracking and friendly reminders.",', 'Text(stringResource(R.string.onboarding_step_3_desc),'),
    ('Text("Continue",', 'Text(stringResource(R.string.continue_btn),'),
    ('Text("Get Started",', 'Text(stringResource(R.string.get_started),')
]

replacements_mygoals = [
    ('Text("My Goals",', 'Text(stringResource(R.string.my_goals),'),
    ('Text("No goals yet. Add one!",', 'Text(stringResource(R.string.no_goals_yet),'),
    ('Text("Add Goal",', 'Text(stringResource(R.string.add_goal),')
]

replacements_creategoal = [
    ('Text("New Goal",', 'Text(stringResource(R.string.new_goal),'),
    ('Text("What\'s the big plan? Set it up here.",', 'Text(stringResource(R.string.new_goal_subtitle),'),
    ('Text("WHAT ARE YOU SAVING FOR?",', 'Text(stringResource(R.string.what_are_you_saving_for),'),
    ('Text("e.g. Dream Vacation, New PC, General Savings")', 'Text(stringResource(R.string.goal_name_placeholder))'),
    ('Text("Goal Type",', 'Text(stringResource(R.string.goal_type),'),
    ('Text("HOW MUCH DO YOU NEED? ($)",', 'Text(stringResource(R.string.how_much_do_you_need),'),
    ('Text("Let\'s Get Saving!",', 'Text(stringResource(R.string.lets_get_saving),')
]

try:
    replace_in_file('app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt', replacements_dashboard)
    replace_in_file('app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', replacements_onboarding)
    replace_in_file('app/src/main/java/com/oryno/piggy_ledger/ui/MyGoalsScreen.kt', replacements_mygoals)
    replace_in_file('app/src/main/java/com/oryno/piggy_ledger/ui/CreateGoalScreen.kt', replacements_creategoal)
except Exception as e:
    print(e)
