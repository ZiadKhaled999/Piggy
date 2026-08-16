import sys

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'r') as f:
    content = f.read()

target = """@Composable
fun SettingsMainContent(
    onModeChange: (SettingsMode) -> Unit,
    onNavigateToPendingTransactions: () -> Unit,
    onSignOutClick: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {"""

replacement = """@Composable
fun SettingsMainContent(
    onModeChange: (SettingsMode) -> Unit,
    onNavigateToPendingTransactions: () -> Unit,
    onSignOutClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/SettingsScreen.kt', 'w') as f:
    f.write(content)
