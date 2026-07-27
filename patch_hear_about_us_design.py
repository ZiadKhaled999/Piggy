import re

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/HearAboutUsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '        colors = CardDefaults.cardColors(\n            containerColor = if (isSelected) PinkPrimary.copy(alpha = 0.1f) else Color(0xFFF1F5F9)\n        ),\n        border = if (isSelected) BorderStroke(2.dp, PinkPrimary) else null,',
    '        modifier = Modifier\n            .fillMaxWidth()\n            .height(72.dp)\n            .border(\n                width = if (isSelected) 3.dp else 2.dp,\n                color = if (isSelected) PinkPrimary else Color(0xFFCBD5E1),\n                shape = RoundedCornerShape(20.dp)\n            )\n            .clickable(onClick = onClick),\n        shape = RoundedCornerShape(20.dp),\n        colors = CardDefaults.cardColors(\n            containerColor = if (isSelected) PinkPrimary.copy(alpha = 0.05f) else Color.White\n        ),'
)

content = content.replace(
    '    Card(\n        modifier = Modifier\n            .fillMaxWidth()\n            .height(64.dp)\n            .clickable(onClick = onClick),\n        shape = RoundedCornerShape(16.dp),\n        modifier = Modifier',
    '    Card(\n        modifier = Modifier'
)

content = content.replace(
    '            verticalArrangement = Arrangement.spacedBy(16.dp)',
    '            verticalArrangement = Arrangement.spacedBy(12.dp)'
)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/HearAboutUsScreen.kt', 'w') as f:
    f.write(content)
