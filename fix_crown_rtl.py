import sys

with open('app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Add import
if 'import androidx.compose.ui.platform.LocalLayoutDirection' not in content:
    content = content.replace(
        'import androidx.compose.ui.platform.LocalContext',
        'import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.platform.LocalLayoutDirection\nimport androidx.compose.ui.unit.LayoutDirection'
    )

old_crown = """        if (isPro) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .rotate(22f)
                    .size((size * 0.65f).coerceAtLeast(26.dp))
            ) {"""

new_crown = """        if (isPro) {
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .rotate(if (isRtl) -22f else 22f)
                    .size((size * 0.65f).coerceAtLeast(26.dp))
            ) {"""

if old_crown in content:
    content = content.replace(old_crown, new_crown)
    print("Crown RTL logic added successfully.")
else:
    print("Old crown not found. Let's try regex or manual replace.")

with open('app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt', 'w') as f:
    f.write(content)

