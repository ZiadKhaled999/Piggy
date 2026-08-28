import re

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'r') as f:
    content = f.read()

# 1. Remove insightText calculation
insight_pattern = r"val insightText = if \(weekendsSum > weekdaysSum\).*?\}\n\n"
content = re.sub(insight_pattern, "", content, flags=re.DOTALL)

# 2. Remove insight Row
row_pattern = r"Spacer\(Modifier\.height\(24\.dp\)\)\n\n\s*Row\(\n\s*modifier = Modifier\n\s*\.fillMaxWidth\(\)\n\s*\.background\(Color\(0xFFFFF9E6\), RoundedCornerShape\(12\.dp\)\)\n\s*\.padding\(12\.dp\),\n\s*verticalAlignment = Alignment\.CenterVertically\n\s*\) \{\n\s*Icon\(\n\s*Icons\.Default\.Lightbulb,\n\s*contentDescription = \"Insight\",\n\s*tint = Color\(0xFFD4A017\),\n\s*modifier = Modifier\.size\(20\.dp\)\n\s*\)\n\s*Spacer\(Modifier\.width\(8\.dp\)\)\n\s*Text\(\n\s*text = insightText,\n\s*fontSize = 14\.sp,\n\s*fontWeight = FontWeight\.Medium,\n\s*color = Color\(0xFF8B6914\)\n\s*\)\n\s*\}\n\n\s*Spacer\(Modifier\.height\(16\.dp\)\)"

content = re.sub(row_pattern, "Spacer(Modifier.height(32.dp))", content)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'w') as f:
    f.write(content)
