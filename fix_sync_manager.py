import re

with open('app/src/main/java/com/oryno/piggy_ledger/service/SyncManager.kt', 'r') as f:
    content = f.read()

# Make syncAll return Boolean
content = re.sub(r'suspend fun syncAll\(\) = withContext\(Dispatchers\.IO\) \{', r'suspend fun syncAll(): Boolean = withContext(Dispatchers.IO) {', content)
content = re.sub(r'return@withContext\n', r'return@withContext false\n', content)

# Find syncAll body and replace the try-catch to return boolean
sync_all_body = """        try {
            var allOk = true
            allOk = syncOnboardingAnswers(nonNullUserId, authHeader) && allOk
            allOk = syncUserPreferences(nonNullUserId, authHeader) && allOk
            allOk = syncStreakDates(nonNullUserId, authHeader) && allOk
            allOk = syncGoals(nonNullUserId, authHeader) && allOk
            allOk = syncTransactions(nonNullUserId, authHeader) && allOk
            allOk = syncLoans(nonNullUserId, authHeader) && allOk
            allOk = syncLoanPayments(nonNullUserId, authHeader) && allOk
            allOk = syncAccounts(nonNullUserId, authHeader) && allOk
            allOk = syncAccountTransactions(nonNullUserId, authHeader) && allOk
            allOk = syncPendingTransactions(nonNullUserId, authHeader) && allOk
            allOk = syncAiConversations(nonNullUserId, authHeader) && allOk
            allOk = syncAiChatMessages(nonNullUserId, authHeader) && allOk

            Log.i("SyncManager", "Sync completed successfully. Success: $allOk")
            allOk
        } catch (e: Exception) {
            Log.w("SyncManager", "Sync skipped/failed: ${e.message}")
            false
        }"""
content = re.sub(r'        try \{.*?catch \(e: Exception\) \{\s*Log\.w\("SyncManager", "Sync skipped/failed: \$\{e\.message\}"\)\s*\}', sync_all_body, content, flags=re.DOTALL)

def fix_sync_func(func_text):
    func_text = re.sub(r'private suspend fun sync(\w+)\((.*?)\) \{', r'private suspend fun sync\1(\2): Boolean {', func_text)
    
    # Replace pushRemote logic
    func_text = re.sub(r'if \(pushRemote\((.*?),\s*(.*?),\s*(.*?)\)\) \{', r'val pushOk = pushRemote(\1, \2, \3)\n        if (pushOk) {', func_text)
    
    # Replace pullRemote logic
    func_text = re.sub(r'val remote:\s*List<(.*?)>\?\s*=\s*pullRemote\((.*?),\s*(.*?)\)', r'val remote: List<\1>? = pullRemote(\2, \3)\n        val pullOk = remote != null', func_text)
    
    # Add return statement. We know these functions end with a '}' on a new line.
    # To be safe, just substitute the last '}' with the return statement.
    if func_text.rstrip().endswith('}'):
        func_text = func_text.rstrip()[:-1] + '    return pushOk && pullOk\n    }\n'
    
    return func_text

funcs = re.split(r'(?=    private suspend fun sync[A-Z])', content)

new_content = funcs[0]
for func in funcs[1:]:
    if 'private suspend fun sync' in func:
        if 'private suspend fun sync' in func.split('\n')[0]:
            new_func = fix_sync_func(func)
            new_content += new_func
        else:
            new_content += func
    else:
        new_content += func

with open('app/src/main/java/com/oryno/piggy_ledger/service/SyncManager.kt', 'w') as f:
    f.write(new_content)

