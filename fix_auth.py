with open("app/src/main/java/com/oryno/piggy_ledger/ui/AuthScreen.kt", "r") as f:
    content = f.read()

content = content.replace('package com.oryno.piggy_ledger.uiimport androidx.compose.ui.res.stringResource', 'package com.oryno.piggy_ledger.ui\n\nimport androidx.compose.ui.res.stringResource\n')

with open("app/src/main/java/com/oryno/piggy_ledger/ui/AuthScreen.kt", "w") as f:
    f.write(content)
