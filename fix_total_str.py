import re

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'r') as f:
    content = f.read()

# Replace Total Balance text
target = 'Text(totalStr, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)'
replacement = 'Text(totalStr, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)'

content = content.replace(target, replacement)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'w') as f:
    f.write(content)

