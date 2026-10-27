# Sync diagnosis — how sync actually works (verified in code)

Date: 2026-10-09. Scope: Android client only. Backend (`piggy-api-endpoints.vercel.app` → Neon) is external; findings about it are inferred from client code + live probing noted in prior review.

## Path traced

```
Room (isSynced=0 dirty, is_deleted=1 tombstone)
 ├── PiggyLedgerRepository.kt:8-22  triggerSync() -> WorkManager OneTime "SyncWork" REPLACE, no constraints
 ├── UserPreferences.kt:490-510     same trigger on every prefs save
 ├── StreakManager.kt:205-228       direct SyncManager.syncAll() (NOT WorkManager), unstructured CoroutineScope(IO)
 └── PiggyLedgerViewModel.kt:67-93  sessionFlow first-session direct sync
     + AuthScreen.kt:112-126,1177-1178,1232-1233 setAuthUser + triggerCloudSync (3 call sites)
     + logout PiggyLedgerViewModel.kt:448-506 direct syncAll() then wipeLocalData()
        ↓
SyncWorker.kt:12-20  CoroutineWorker, discards syncAll() result, always success()
        ↓
SyncManager.kt:55-94  syncAll() mutex-guarded, 12 tables sequential:
  onboarding → prefs → streak → goals → transactions → loans → loan_payments
  → accounts → account_transactions → pending → ai_conversations → ai_chat_messages
  each: dao.getUnsynced*() → copy(userId, isSynced=true) → POST /sync/push/{table}
        → on 2xx re-insert locally (mark clean) → GET /sync/pull/{table}
        → insert remote iff (local==null || (local.isSynced && remote.updatedAt > local.updatedAt))
        ↓
Ktor ApiClient.kt → Vercel → Neon Postgres (neon_schema.sql, RLS USING(true) on all tables)
```

Auth: Clerk `session.fetchToken(GetTokenOptions())` (no template) once per `syncAll()`, reused for ~24 calls. Null/empty token aborts whole sync (returns false).

## Key mechanism facts (matter for fix choice)

1. **Tombstone push is the only delete path that can work.** Soft-deletes set `is_deleted=1, isSynced=0`
   (`PiggyLedgerDao.kt:106,121,139,181,256,307`), and `getUnsynced*` has no `is_deleted` filter, so tombstones
   ride the normal push. `deleteFromCloud()` (`SyncManager.kt:589-610`) hits `DELETE /sync/delete`, which live-probes
   404 on all variants and is swallowed + ignored by callers. So: hard-delete never lands; tombstone-upsert is the
   real channel. Any fix must make tombstones reliably enqueue + push.
2. **Currency never reaches the wire.** `UserPreferencesEntity.appCurrency` is `@Transient` (`Entities.kt:160-161`),
   and `applyFromEntity` has `prefs[APP_CURRENCY]` commented out (`UserPreferences.kt:438`). Server column exists
   (`neon_schema.sql` user_preferences.appCurrency). Pure client bug, no migration needed (Room column already exists;
   `@Transient` only affects kotlinx.serialization).
3. **Streak is append-only with union merge.** `syncFromCloud` (`StreakManager.kt:196-203`) unions prefs sets, never
   removes; `syncStreakToDb` only inserts current dates, never tombstones removed ones; pull block
   (`SyncManager.kt:232-244`) has no `updatedAt` guard unlike all other tables. Removals resurrect.
4. **Onboarding ids are timestamps** (`"${now}_$key"`, `PiggyLedgerRepository.kt:157-185`), never upserted by key →
   duplicates accumulate both sides; `getOnboardingAnswerByKey` returns an arbitrary one.
5. **Worker always reports success** (`SyncWorker.kt:12-20`), has no `CONNECTED` constraint, no backoff, no periodic
   schedule; `REPLACE` on every keystroke cancels the queued job. Offline failures are forgotten until logout.
6. **Logout can be undone by a stray worker.** `performSyncAndLogout` awaits direct `syncAll()` then
   `clearAllTables()`, but an enqueued `SyncWork` (or `StreakManager`'s unstructured scope) can run pull after the
   wipe while still authenticated and repopulate what was just wiped.
