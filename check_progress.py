with open('./app/src/main/java/com/oryno/piggy_ledger/ui/OnboardingScreen.kt', 'r') as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if "ProgressNextButton(" in line:
        start = max(0, i-5)
        end = min(len(lines), i+15)
        print("\n".join(lines[start:end]))
        print("-----")
