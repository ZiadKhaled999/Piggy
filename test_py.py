with open('app/src/main/java/com/oryno/piggy_ledger/ui/LoansScreen.kt', 'r') as f:
    content = f.read()
first_newline = content.find('\n')
print(f"First newline index: {first_newline}")
print(f"Total length: {len(content)}")
