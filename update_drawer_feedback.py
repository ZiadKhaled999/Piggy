import sys
import xml.etree.ElementTree as ET

# 1. Update translations
strings = {
    'piggy_ledger_pro': {'ar': 'بيجي ليدجر برو', 'eg': 'بيجي ليدجر برو'}
}

files = {
    'ar': 'app/src/main/res/values-ar/strings.xml',
    'eg': 'app/src/main/res/values-ar-rEG/strings.xml'
}

for lang, path in files.items():
    tree = ET.parse(path)
    root = tree.getroot()
    for key, vals in strings.items():
        exists = False
        for child in root:
            if child.get('name') == key:
                exists = True
                child.text = vals[lang]
                break
        if not exists:
            elem = ET.Element('string', name=key)
            elem.text = vals[lang]
            root.append(elem)
    tree.write(path, encoding='utf-8', xml_declaration=True)

# 2. Update PiggyLedgerApp.kt feedback link
with open('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerApp.kt', 'r') as f:
    app_content = f.read()

feedback_drawer_old = """                DrawerMenuItem(
                    title = stringResource(R.string.give_feedback),
                    onClick = {
                        onClose()
                        appNavController.navigate(Screen.Settings(SettingsMode.FEEDBACK.name))
                    }
                )"""

feedback_drawer_new = """                DrawerMenuItem(
                    title = stringResource(R.string.give_feedback),
                    onClick = {
                        onClose()
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://piggy-ledger.featureos.app"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.browser_error), android.widget.Toast.LENGTH_SHORT)
                        }
                    }
                )"""

if feedback_drawer_old in app_content:
    app_content = app_content.replace(feedback_drawer_old, feedback_drawer_new)
    print("Updated PiggyLedgerApp.kt")
else:
    print("Could not find old drawer item in PiggyLedgerApp.kt")

with open('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerApp.kt', 'w') as f:
    f.write(app_content)

# 3. Update SettingsScreen.kt feedback link
with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'r') as f:
    set_content = f.read()

settings_feedback_old = """        SettingsItem(
            title = stringResource(R.string.give_feedback),
            iconRes = R.drawable.img_settings_feedback,
            onClick = { onModeChange(SettingsMode.FEEDBACK) }
        )"""

settings_feedback_new = """        SettingsItem(
            title = stringResource(R.string.give_feedback),
            iconRes = R.drawable.img_settings_feedback,
            onClick = {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://piggy-ledger.featureos.app"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    com.oryno.piggy_ledger.ui.ToastUtil.show(context, context.getString(R.string.browser_error), android.widget.Toast.LENGTH_SHORT)
                }
            }
        )"""

if settings_feedback_old in set_content:
    set_content = set_content.replace(settings_feedback_old, settings_feedback_new)
    print("Updated SettingsScreen.kt")
else:
    print("Could not find old settings item in SettingsScreen.kt")
    
with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'w') as f:
    f.write(set_content)

