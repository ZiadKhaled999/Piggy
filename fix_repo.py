import sys

with open('app/src/main/java/com/oryno/piggy_ledger/data/PiggyLedgerRepository.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if line.strip().startswith("suspend fun restoreBackup(data: BackupData) {"):
        skip = True
    
    if skip and line.strip() == "}":
        skip = False
        continue # Skip the closing brace of restoreBackup
        
    if line.strip().startswith("suspend fun restoreFullDatabaseBackup(data: FullBackupData) {"):
        skip = True

    if skip and line.strip().startswith("val allPendingTransactions: Flow<List<PendingTransaction>>"):
        skip = False
        
    if not skip:
        new_lines.append(line)

with open('app/src/main/java/com/oryno/piggy_ledger/data/PiggyLedgerRepository.kt', 'w') as f:
    f.writelines(new_lines)
