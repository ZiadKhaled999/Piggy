# Sync issues (ranked, with file:line)

## P0 — data loss / cross-device resurrection

- **P0-1 Deletes never reach cloud.** `SyncManager.kt:589-610` → `DELETE /sync/delete` 404s (live-probed), swallowed,
  callers ignore result. Only local soft-delete happens. Next login elsewhere pulls the still-alive server row.
  Aggravator: `deleteGoal` (`PiggyLedgerRepository.kt:113-119`) and `deleteAccount` (`86-96`) fire `deleteFromCloud`
  fire-and-forget with **no `triggerSync()`**, so even the tombstone sits dirty until an unrelated write.
  (`deleteLoan:111`, `deleteLoanPayment:70`, `deletePending:154` do trigger — inconsistent.)
- **P0-2 Currency sync dead.** `@Transient appCurrency` (`Entities.kt:160-161`) + commented-out apply
  (`UserPreferences.kt:438`). Multi-device currency diverges permanently.
- **P0-3 Logout wipe can be repopulated by stray worker.** Enqueued `SyncWork` / `StreakManager` scope pull after
  `clearAllTables()` (`PiggyLedgerViewModel.kt:508-540`) while still authed → re-downloads wiped rows.

## P1 — sync correctness / robustness

- **P1-1 Onboarding duplicates.** Timestamp ids, no upsert-by-key (`PiggyLedgerRepository.kt:157-185`).
- **P1-2 Streak removal resurrects + stale overwrite.** No `updatedAt` guard (`SyncManager.kt:232-244`), union merge
  (`StreakManager.kt:196-203`), `syncStreakToDb` never tombstones removed dates, prefs merge includes `is_deleted`
  rows.
- **P1-3 Dirty-local pins remote forever.** Pull guard `local.isSynced && remote.updatedAt > local.updatedAt` has no
  fallback; one Boolean collapses 12 tables; no per-row surfacing.
- **P1-4 Worker reliability.** Always `success()`, no `CONNECTED` constraint, no backoff, no periodic, `REPLACE`
  churn on every write.
- **P1-5 Transport hygiene.** `LogLevel.ALL` in release (`ApiClient.kt:20-27`) logs JWT+PII on ~24 calls/sync;
  no Ktor timeouts (can hang); token fetched once, no 401 refresh; duplicate full syncs on login
  (`PiggyLedgerViewModel.kt:67-77` + `AuthScreen.kt` ×3, mutex-safe but 2× traffic).
- **P1-6 `updatedAt` never bumped on `@Update` paths** (`repository.updateAccount:79-85`, `markLoanAsPaid:104-110`
  keeps old timestamp) → LWW comparisons meaningless.

## P2 — accepted / minor (documented, not fixed here)

- `isSynced=true` sent on wire: harmless (server column defaults true), stripping needs DTOs per table — deferred.
- Login-then-jump first paint (async sync, no await): UX polish, needs nav-graph change — deferred.
- RLS `USING(true)` (`neon_schema.sql:235-246`): must be enforced in Vercel API (outside this repo) — flagged, not fixable here.
