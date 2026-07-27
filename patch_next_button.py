with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("if (currentPage != 7 && currentPage != 8) {", "if (currentPage != 7 && currentPage != 8 && currentPage != 9 && currentPage != 10 && currentPage != 11) {")

with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'w') as f:
    f.write(content)
