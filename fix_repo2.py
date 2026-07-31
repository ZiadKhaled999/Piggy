import re

file_path = "app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt"
with open(file_path, "r") as f:
    content = f.read()

# We will just append `triggerSync()` after each viewModelScope.launch where a write happens.
# Actually, an easier way is to just grep for `repository.insert`, `repository.delete`, `repository.update`, `repository.resolve`, `repository.markLoanAsPaid` and add `triggerSync()` in the next line.

lines = content.split('\n')
new_lines = []
for line in lines:
    new_lines.append(line)
    if "repository.insert" in line or "repository.update" in line or "repository.delete" in line or "repository.resolve" in line or "repository.markLoanAsPaid" in line:
        if "}" not in line and "{" not in line and "return" not in line and "val " not in line:
            indent = line[:len(line) - len(line.lstrip())]
            new_lines.append(indent + "triggerSync()")
        elif "val " in line:
            indent = line[:len(line) - len(line.lstrip())]
            new_lines.append(indent + "triggerSync()")

with open(file_path, "w") as f:
    f.write('\n'.join(new_lines))
