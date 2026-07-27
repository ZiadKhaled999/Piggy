with open('./app/src/main/java/com/oryno/piggy_ledger/ui/HearAboutUsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'import androidx.compose.foundation.background\n',
    'import androidx.compose.foundation.background\nimport androidx.compose.foundation.border\n'
)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/HearAboutUsScreen.kt', 'w') as f:
    f.write(content)
