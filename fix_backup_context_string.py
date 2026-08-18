import re

with open('app/src/main/java/com/oryno/piggy_ledger/ui/BackupSettingsView.kt', 'r') as f:
    content = f.read()

replacements = [
    ('ToastUtil.show(context, stringResource(R.string.export_success, "JSON"), android.widget.Toast.LENGTH_SHORT)',
     'ToastUtil.show(context, context.getString(R.string.export_success, "JSON"), android.widget.Toast.LENGTH_SHORT)'),
    ('ToastUtil.show(context, stringResource(R.string.export_failed, e.message.toString()), android.widget.Toast.LENGTH_LONG)',
     'ToastUtil.show(context, context.getString(R.string.export_failed, e.message.toString()), android.widget.Toast.LENGTH_LONG)'),
    ('ToastUtil.show(context, stringResource(R.string.export_success, "CSV"), android.widget.Toast.LENGTH_SHORT)',
     'ToastUtil.show(context, context.getString(R.string.export_success, "CSV"), android.widget.Toast.LENGTH_SHORT)'),
    ('ToastUtil.show(context, stringResource(R.string.export_success, "EXCEL"), android.widget.Toast.LENGTH_SHORT)',
     'ToastUtil.show(context, context.getString(R.string.export_success, "EXCEL"), android.widget.Toast.LENGTH_SHORT)'),
    ('ToastUtil.show(context, stringResource(R.string.export_upgrade_pro), android.widget.Toast.LENGTH_SHORT)',
     'ToastUtil.show(context, context.getString(R.string.export_upgrade_pro), android.widget.Toast.LENGTH_SHORT)'),
    ('Text(stringResource(R.string.export_action),', 'Text(text = stringResource(R.string.export_action),')
]

for old, new in replacements:
    content = content.replace(old, new)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/BackupSettingsView.kt', 'w') as f:
    f.write(content)

print("Fixed context getString in BackupSettingsView")
