import sys

with open('app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt', 'r') as f:
    content = f.read()

old_crown = """        if (isPro) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .rotate(22f)
                    .size((size * 0.45f).coerceAtLeast(18.dp))
            ) {"""

new_crown = """        if (isPro) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .rotate(22f)
                    .size((size * 0.65f).coerceAtLeast(26.dp))
            ) {"""

if old_crown in content:
    content = content.replace(old_crown, new_crown)
    print("Crown updated successfully.")
else:
    print("Old crown not found. Let's try regex or manual replace.")

with open('app/src/main/java/com/oryno/piggy_ledger/ui/DashboardScreen.kt', 'w') as f:
    f.write(content)

