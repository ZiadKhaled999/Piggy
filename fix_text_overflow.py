import re

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'r') as f:
    content = f.read()

# For Weekdays sum
content = content.replace(
    'if (isPrivacyMode) "••••••" else format.format(weekdaysSum),\n                        fontSize = 16.sp,\n                        fontWeight = FontWeight.Bold,\n                        color = TextPrimary',
    'if (isPrivacyMode) "••••••" else format.format(weekdaysSum),\n                        fontSize = 16.sp,\n                        fontWeight = FontWeight.Bold,\n                        color = TextPrimary,\n                        maxLines = 1,\n                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis'
)

# For Weekends sum
content = content.replace(
    'if (isPrivacyMode) "••••••" else format.format(weekendsSum),\n                        fontSize = 16.sp,\n                        fontWeight = FontWeight.Bold,\n                        color = TextPrimary',
    'if (isPrivacyMode) "••••••" else format.format(weekendsSum),\n                        fontSize = 16.sp,\n                        fontWeight = FontWeight.Bold,\n                        color = TextPrimary,\n                        maxLines = 1,\n                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis'
)

# For Average sum
content = content.replace(
    'if (isPrivacyMode) "••••••" else format.format(avgValue).replace(".00", ""),\n                        fontSize = 14.sp,\n                        fontWeight = FontWeight.Bold,\n                        color = TextPrimary',
    'if (isPrivacyMode) "••••••" else format.format(avgValue).replace(".00", ""),\n                        fontSize = 14.sp,\n                        fontWeight = FontWeight.Bold,\n                        color = TextPrimary,\n                        maxLines = 1,\n                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis'
)

# For Highest sum
content = content.replace(
    'if (isPrivacyMode) "••••••" else if (peakPoint != null) format.format(peakPoint.value).replace(".00", "") else "$0",\n                        fontSize = 14.sp,\n                        fontWeight = FontWeight.Bold,\n                        color = TextPrimary',
    'if (isPrivacyMode) "••••••" else if (peakPoint != null) format.format(peakPoint.value).replace(".00", "") else "$0",\n                        fontSize = 14.sp,\n                        fontWeight = FontWeight.Bold,\n                        color = TextPrimary,\n                        maxLines = 1,\n                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis'
)

# For Total sum
content = content.replace(
    'if (isPrivacyMode) "••••••" else format.format(totalRevenue).replace(".00", ""),\n                        fontSize = 14.sp,\n                        fontWeight = FontWeight.Bold,\n                        color = TextPrimary',
    'if (isPrivacyMode) "••••••" else format.format(totalRevenue).replace(".00", ""),\n                        fontSize = 14.sp,\n                        fontWeight = FontWeight.Bold,\n                        color = TextPrimary,\n                        maxLines = 1,\n                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis'
)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/AnalyticsScreen.kt', 'w') as f:
    f.write(content)

