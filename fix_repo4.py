import os

file_path = "app/src/main/java/com/oryno/piggy_ledger/data/PiggyLedgerRepository.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("= { dao.resolvePendingTransaction", "{ dao.resolvePendingTransaction")

with open(file_path, "w") as f:
    f.write(content)
