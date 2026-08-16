import sys

with open('app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt', 'r') as f:
    content = f.read()

old_crown = """        if (isPro) {
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .rotate(if (isRtl) -22f else 22f)
                    .size((size * 0.65f).coerceAtLeast(26.dp))
            ) {"""

new_crown = """        if (isPro) {
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = (-12).dp)
                    .rotate(if (isRtl) -22f else 22f)
                    .size((size * 0.65f).coerceAtLeast(26.dp))
            ) {"""

if old_crown in content:
    content = content.replace(old_crown, new_crown)
    print("Crown updated successfully.")
else:
    print("Old crown not found. Please check.")

with open('app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt', 'w') as f:
    f.write(content)

