# Report

## Section 1 — Files changed
| File | Type | Summary |
|---|---|---|
| data/Entities.kt | modified | Added @ColumnInfo defaults on source/needsReview for PendingTransaction & AccountTransaction; import ColumnInfo |
| data/PiggyLedgerDao.kt | modified | processSmsTransaction returns String; added dedup queries/methods |
| data/PiggyLedgerDatabase.kt | modified | version 28; MIGRATION_27_28; exportSchema true |
| data/DedupModels.kt | new | RecentRow projection |
| data/NotificationParserPrefs.kt | new | DataStore prefs |
| service/TransactionIngestor.kt | new/modified | shared ingestion; hoisted dedup; cross-type merge handling |
| service/DuplicateDetector.kt | modified | evaluatePure refactor |
| service/SmsProcessor.kt | modified | thin wrapper |
| service/SmsParser.kt | modified | reference field |
| service/BankNotificationListenerService.kt | new | notification listener |
| service/SmsNotificationListenerService.kt | deleted | removed old listener |
| AndroidManifest.xml | modified | added Bank listener; removed old listener |
| ui/Screens.kt | modified | added NOTIFICATION_PARSING |
| ui/SettingsScreen.kt | modified | item + branch |
| ui/NotificationParserSettingsView.kt | new | settings UI |
| ui/PendingTransactionsScreen.kt | modified | badges + duplicate groups |
| res/values/strings.xml | modified | new keys |
| app/build.gradle.kts | modified | room.schemaLocation |
| app/schemas/.../28.json | new | exported schema |
| app/src/test/.../DuplicateDetectorTest.kt | new | 10 tests |

## Section 2 — DuplicateDetector logic
```
(paste full file)
```

## Section 3 — Migration vs entity cross-check
Columns added by MIGRATION_27_28:
pending_transactions: source (TEXT NOT NULL DEFAULT 'AUTOMATIC_SMS'), notificationPackage (TEXT DEFAULT NULL), sourceKey (TEXT DEFAULT NULL), reference (TEXT DEFAULT NULL), duplicateGroupId (TEXT DEFAULT NULL), needsReview (INTEGER NOT NULL DEFAULT 0)
account_transactions: sourceKey (TEXT DEFAULT NULL), reference (TEXT DEFAULT NULL), duplicateGroupId (TEXT DEFAULT NULL), needsReview (INTEGER NOT NULL DEFAULT 0)

Entity @ColumnInfo defaults: source='AUTOMATIC_SMS', needsReview='0'. notificationPackage/sourceKey/reference/duplicateGroupId nullable no default. Match: Yes for source/needsReview.

28.json exists.

## Section 4 — Test results
compile: BUILD SUCCESSFUL
tests: BUILD SUCCESSFUL
All 10 DuplicateDetector tests pass.

## Section 5 — Cross-type MERGE
MERGE branch: if matchedTable == pending_transactions && matchedAccount != null: processSmsTransaction returns new id; flagPendingDuplicate(matchedId); flagAccountDuplicate(newId). Otherwise enrich.

## Section 6 — Old listener status
Before: file + manifest entry. After: none (deleted).

## Section 7 — Open questions
- PendingTransactionsScreen uses DAO directly in composable; ViewModel not extended.
- exportSchema true generates schemas in repo.
- No UI tests added.
```
But need full DuplicateDetector content in Section 2. Let's append file content. Easier use shell to create report with cat. I'll write report using python including file content.<|end|><|start|><|repo_name|>assistant to=bash<|channel|>analysis <|constrain|>xml<|message|><rfl:parameter name="command">
cd /workspaces/Piggy && python3 - <<'PY'
import pathlib
dd=pathlib.Path("app/src/main/java/com/oryno/piggy_ledger/service/DuplicateDetector.kt").read_text()
report=pathlib.Path("REPORT.md")
report.write_text("""# Report

## Section 1 — Files changed
See git status; 16 files modified/created/deleted including TransactionIngestor, DuplicateDetector, listener, prefs, UI, entities, DAO, DB, manifest, strings, build.gradle, schema, tests.

## Section 2 — DuplicateDetector logic as it stands now
```kotlin
"""+dd+"""
```

## Section 3 — Migration vs. entity cross-check
Columns added by MIGRATION_27_28:
- pending_transactions: source TEXT NOT NULL DEFAULT 'AUTOMATIC_SMS'; notificationPackage TEXT DEFAULT NULL; sourceKey TEXT DEFAULT NULL; reference TEXT DEFAULT NULL; duplicateGroupId TEXT DEFAULT NULL; needsReview INTEGER NOT NULL DEFAULT 0
- account_transactions: sourceKey TEXT DEFAULT NULL; reference TEXT DEFAULT NULL; duplicateGroupId TEXT DEFAULT NULL; needsReview INTEGER NOT NULL DEFAULT 0

Entity @ColumnInfo defaults: source='AUTOMATIC_SMS', needsReview='0'. Match: Yes. app/schemas/.../28.json exists.

## Section 4 — Test results
`./gradlew :app:compileDebugKotlin` -> BUILD SUCCESSFUL
`./gradlew :app:testDebugUnitTest` -> BUILD SUCCESSFUL
All 10 DuplicateDetector tests pass.

## Section 5 — Cross-type MERGE handling
MERGE branch checks matchedTable == "pending_transactions" && matchedAccount != null; calls processSmsTransaction (returns new id), flagPendingDuplicate(matchedId), flagAccountDuplicate(newId). Otherwise enrich existing row.

## Section 6 — Old listener status
Before: file and manifest entry present. After Task 3: deleted file and removed manifest entry; grep returns none.

## Section 7 — Open questions
- Pending screen uses DAO directly in composable rather than ViewModel.
- No UI tests (emulator required).
- Kept fallbackToDestructiveMigration.
""")
print("written")
PY
cat REPORT.md | head -20