import re

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'r') as f:
    content = f.read()

# Replace RevenuePeriod enum
old_revenue_period_pattern = r"enum class RevenuePeriod\(val label: String\) \{.*?YEAR_1\(\"1Y\"\)\n\}"
new_revenue_period = """enum class RevenuePeriod(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}"""
content = re.sub(old_revenue_period_pattern, new_revenue_period, content, flags=re.DOTALL)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'w') as f:
    f.write(content)
