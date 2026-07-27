with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r') as f:
    content = f.read()

variables = """
    var relatesToLoans by remember { mutableStateOf<Boolean?>(null) }
    var relatesToAccounts by remember { mutableStateOf<Boolean?>(null) }
    var relatesToEmergency by remember { mutableStateOf<Boolean?>(null) }
"""

content = content.replace('    var selectedIntensity by remember { mutableIntStateOf(-1) }', '    var selectedIntensity by remember { mutableIntStateOf(-1) }\n' + variables)

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w') as f:
    f.write(content)
