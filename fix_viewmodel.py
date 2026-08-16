import sys

with open('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if line.strip().startswith("fun importData(jsonString: String, onComplete: () -> Unit, onError: (String) -> Unit) {"):
        skip = True
    
    if skip and line.strip().startswith("val hasOnboarded = userPreferences.hasOnboarded.stateIn("):
        skip = False
    
    if not skip:
        new_lines.append(line)

with open('app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt', 'w') as f:
    f.writelines(new_lines)
