import re

file_path = "app/src/main/java/com/oryno/piggy_ledger/ui/PiggyLedgerViewModel.kt"
with open(file_path, "r") as f:
    lines = f.read().split('\n')

new_lines = []
skip = False
for line in lines:
    if "private fun triggerSync() {" in line:
        skip = True
        continue
    if skip:
        if "}" in line and "androidx.work.ExistingWorkPolicy.REPLACE" not in line and "workRequest" not in line:
            # this is the end of triggerSync
            skip = False
        continue

    if "triggerSync()" in line:
        continue
    
    new_lines.append(line)

with open(file_path, "w") as f:
    f.write('\n'.join(new_lines))
