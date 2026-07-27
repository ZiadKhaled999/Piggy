with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("We will use the Debt Avalanche method to save you maximum interest.", "We will use the Debt Snowball method to build psychological momentum and quick wins.")
content = content.replace("Debt Avalanche", "Debt Snowball")

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w') as f:
    f.write(content)
