import sys

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'ToastUtil.show(context, stringResource(R.string.pro_toast_desc), Toast.LENGTH_LONG)',
    'ToastUtil.show(context, context.getString(R.string.pro_toast_desc), Toast.LENGTH_LONG)'
)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)

