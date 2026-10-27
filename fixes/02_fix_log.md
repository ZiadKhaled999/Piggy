# Fix log (running — updated as work lands)

## Strategy

Backend is external, so all fixes are client-side and tombstone-first: make `is_deleted` push reliable instead of
depending on the dead `DELETE /sync/delete` route. `deleteFromCloud()` calls are kept (harmless, may start working
if backend adds the route) but no longer load-bearing. No Room schema change → no migration needed.

## Applied (2026-10-09)

- [x] F1 Worker reliability (`SyncWorker.kt`, triggers, `PiggyLedgerApplication.kt`)
  - `doWork()` returns `retry()` on `syncAll()==false` (max 3 attempts), `success()` when logged out.
  - New `scheduleOneTime()` (KEEP + `CONNECTED` + exponential backoff) and `schedulePeriodic()` (6h, KEEP).
  - `cancelAll()` for logout wipe race. `Repository.triggerSync()` + `UserPreferences.triggerSync()` delegate to it
    (REPLACE removed). App `onCreate` schedules periodic. Why KEEP: `syncAll()` flushes everything, so coalescing
    rapid writes into one pending job is correct and cheaper; REPLACE cancelled the queued job per keystroke.
- [x] F2 Transport hygiene (`ApiClient.kt`)
  - `HttpTimeout` 30s/15s/30s; `Logging` INFO in debug, NONE in release (was ALL — JWT/PII bodies in logcat).
- [x] F3 Delete reliability (`PiggyLedgerRepository.kt`, `PiggyLedgerViewModel.kt`)
  - `deleteAccount`/`deleteGoal` now `triggerSync()` (tombstone push enqueued); `deleteFromCloud` kept best-effort.
  - `wipeLocalData()` cancels sync workers before `clearAllTables()`.
- [x] F4 Currency (`Entities.kt`, `UserPreferences.kt`)
  - Removed `@Transient` on `appCurrency` (no migration — Room column already exists); restored apply-from-cloud.
- [x] F5 Onboarding dedup (`PiggyLedgerDao.kt`, `PiggyLedgerRepository.kt`)
  - New `getOnboardingAnswersByKey()`; saves reuse newest row id per key and tombstone older duplicates.
- [x] F6 Streak guards (`SyncManager.kt`, `StreakManager.kt`)
  - Pull gained the `updatedAt` guard like all other tables; prefs merge excludes `is_deleted` rows;
    `syncStreakToDb` tombstones dates removed from prefs.
- [x] F7 `updatedAt` bumps (`PiggyLedgerRepository.kt`)
  - `updateAccount` / `markLoanAsPaid` set `updatedAt=now` so LWW compares edit time.

## Verification

- [x] grep: no `ExistingWorkPolicy.REPLACE` / `LogLevel.ALL` left under `app/src/main/java`; all `scheduleOneTime`,
  `cancelAll`, tombstone, currency touchpoints present.
- [x] `git diff` reviewed per file (incl. repair of a mangled whitespace edit in `PiggyLedgerApplication.kt`).
- [ ] Full Gradle build/tests: NOT run — no Android SDK in this container (`NO_SDK`). Needs `./gradlew :app:assembleDebug`
  + unit tests on a dev machine before merge. Risk points to watch at build time: `HttpTimeout` plugin import
  (`io.ktor.client.plugins.HttpTimeout`, ktor 3.0.0 core), `runAttemptCount` availability, `catch (_e: …)` syntax.
