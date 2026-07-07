import re
import os

def update_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w') as f:
        f.write(content)

dashboard_replacements = [
    ('Text(\n                text = "Welcome to Your Circle",', 'Text(\n                text = stringResource(R.string.welcome_to_circle),'),
    ('Text(\n                text = "Choose how you\'d like to start your saving journey.",', 'Text(\n                text = stringResource(R.string.choose_saving_journey),'),
    ('DashboardCard(\n                title = "Start New Goal",\n                subtitle = "Set a target.",', 'DashboardCard(\n                title = stringResource(R.string.start_new_goal),\n                subtitle = stringResource(R.string.set_target),'),
    ('DashboardCard(\n                title = "Payoffs & Loans",\n                subtitle = "Manage who you lent money to or who you owe.",', 'DashboardCard(\n                title = stringResource(R.string.payoffs_loans),\n                subtitle = stringResource(R.string.manage_loans),'),
    ('Text(\n                                "Community Feedback",', 'Text(\n                                stringResource(R.string.community_feedback),'),
    ('Text(\n                                    text = "Join our Community Board",', 'Text(\n                                    text = stringResource(R.string.join_community_board),'),
    ('Text(\n                                    text = "Help us improve Piggy Ledger! Click below to visit our feature request board. There, you can suggest new features, report bugs, and upvote existing suggestions from other users.",', 'Text(\n                                    text = stringResource(R.string.help_improve),'),
    ('Text(\n                                "Rate the App",', 'Text(\n                                stringResource(R.string.rate_app_title),'),
    ('Text(\n                            text = "Enjoying Piggy Ledger? Tap the stars to rate your experience. Your support keeps us going!",', 'Text(\n                            text = stringResource(R.string.enjoying_piggy_ledger),'),
    ('Text(\n                                "Backup Data",', 'Text(\n                                stringResource(R.string.backup_data_title),'),
    ('Text(\n                                    "Secure Local Export",', 'Text(\n                                    stringResource(R.string.secure_local_export),'),
    ('Text(\n                                    "Save your goals, logs, and ledger stats to a backup JSON file.",', 'Text(\n                                    stringResource(R.string.save_goals_desc),'),
    ('Text(\n                                "Restore Data",', 'Text(\n                                stringResource(R.string.restore_data_title),'),
    ('Text(\n                                    "Import JSON Backup",', 'Text(\n                                    stringResource(R.string.import_json_backup),'),
    ('Text(\n                                    "Restoring data will replace your current local goals and logs.",', 'Text(\n                                    stringResource(R.string.restoring_data_replace),')
]

my_goals_replacements = [
    ('Text(\n                        "No goals yet",', 'Text(\n                        stringResource(R.string.no_goals_yet),'),
    ('Text(\n                        "Start your first goal today!",', 'Text(\n                        stringResource(R.string.start_first_goal),'),
    ('Text(\n                        text = if (isOpenSavings) "Open Savings (General)" else "Target: $$${String.format(\\"%.0f\\", goal.targetAmount)}",', 'Text(\n                        text = if (isOpenSavings) stringResource(R.string.open_savings) else "${stringResource(R.string.target)} $$${String.format(\\"%.0f\\", goal.targetAmount)}",')
]

update_file('app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt', dashboard_replacements)
update_file('app/src/main/java/com/oryno/piggy_ledger/ui/MyGoalsScreen.kt', my_goals_replacements)

# Add strings to strings.xml
en_strings = """
    <string name="welcome_to_circle">Welcome to Your Circle</string>
    <string name="choose_saving_journey">Choose how you\'d like to start your saving journey.</string>
    <string name="start_new_goal">Start New Goal</string>
    <string name="set_target">Set a target.</string>
    <string name="payoffs_loans">Payoffs &amp; Loans</string>
    <string name="manage_loans">Manage who you lent money to or who you owe.</string>
    <string name="community_feedback">Community Feedback</string>
    <string name="join_community_board">Join our Community Board</string>
    <string name="help_improve">Help us improve Piggy Ledger! Click below to visit our feature request board. There, you can suggest new features, report bugs, and upvote existing suggestions from other users.</string>
    <string name="rate_app_title">Rate the App</string>
    <string name="enjoying_piggy_ledger">Enjoying Piggy Ledger? Tap the stars to rate your experience. Your support keeps us going!</string>
    <string name="backup_data_title">Backup Data</string>
    <string name="secure_local_export">Secure Local Export</string>
    <string name="save_goals_desc">Save your goals, logs, and ledger stats to a backup JSON file.</string>
    <string name="restore_data_title">Restore Data</string>
    <string name="import_json_backup">Import JSON Backup</string>
    <string name="restoring_data_replace">Restoring data will replace your current local goals and logs.</string>
    <string name="start_first_goal">Start your first goal today!</string>
    <string name="open_savings">Open Savings (General)</string>
    <string name="target">Target:</string>
"""

ar_strings = """
    <string name="welcome_to_circle">مرحباً بك في دائرتك</string>
    <string name="choose_saving_journey">اختر كيف تريد أن تبدأ رحلة التوفير.</string>
    <string name="start_new_goal">بدء هدف جديد</string>
    <string name="set_target">حدد هدفاً.</string>
    <string name="payoffs_loans">المدفوعات والقروض</string>
    <string name="manage_loans">أدر أموالك المقترضة أو المدين بها.</string>
    <string name="community_feedback">ملاحظات المجتمع</string>
    <string name="join_community_board">انضم إلى لوحة المجتمع</string>
    <string name="help_improve">ساعدنا في تحسين دفتر الحصالة! انقر أدناه لزيارة لوحة طلب الميزات.</string>
    <string name="rate_app_title">تقييم التطبيق</string>
    <string name="enjoying_piggy_ledger">هل تستمتع بدفتر الحصالة؟ اضغط على النجوم لتقييم تجربتك.</string>
    <string name="backup_data_title">نسخ احتياطي للبيانات</string>
    <string name="secure_local_export">تصدير محلي آمن</string>
    <string name="save_goals_desc">احفظ أهدافك وسجلاتك وإحصائياتك في ملف JSON.</string>
    <string name="restore_data_title">استعادة البيانات</string>
    <string name="import_json_backup">استيراد نسخة JSON الاحتياطية</string>
    <string name="restoring_data_replace">استعادة البيانات ستستبدل الأهداف والسجلات الحالية.</string>
    <string name="start_first_goal">ابدأ هدفك الأول اليوم!</string>
    <string name="open_savings">مدخرات مفتوحة (عامة)</string>
    <string name="target">الهدف:</string>
"""

def append_strings(filepath, content):
    with open(filepath, 'r') as f:
        file_content = f.read()
    file_content = file_content.replace('</resources>', content + '\n</resources>')
    with open(filepath, 'w') as f:
        f.write(file_content)

append_strings('app/src/main/res/values/strings.xml', en_strings)
append_strings('app/src/main/res/values-ar/strings.xml', ar_strings)
