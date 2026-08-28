import re

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'r') as f:
    content = f.read()

# Add weights to stats columns in SpendingView
# Weekdays
content = content.replace(
    'Column(horizontalAlignment = Alignment.CenterHorizontally) {\n                    Text("Weekdays"',
    'Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {\n                    Text("Weekdays"'
)
# Weekends
content = content.replace(
    'Column(horizontalAlignment = Alignment.CenterHorizontally) {\n                    Text("Weekends"',
    'Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {\n                    Text("Weekends"'
)

# Add weights to stats columns in RevenueView
# Average
content = content.replace(
    'Column(horizontalAlignment = Alignment.CenterHorizontally) {\n                    Text("Average"',
    'Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {\n                    Text("Average"'
)
# Highest
content = content.replace(
    'Column(horizontalAlignment = Alignment.CenterHorizontally) {\n                    Text("Highest"',
    'Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {\n                    Text("Highest"'
)
# Total
content = content.replace(
    'Column(horizontalAlignment = Alignment.CenterHorizontally) {\n                    Text("Total"',
    'Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {\n                    Text("Total"'
)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'w') as f:
    f.write(content)

