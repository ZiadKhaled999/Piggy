# Fix 03 — Room v21 upgrade wipe + corrupt-DB crash loop

## Diagnosis
- `PiggyLedgerDatabase` is at `version = 21`, but the migration chain stopped at
  `MIGRATION_19_20` (commit `7c3ecf0` bumped 20 → 21 with no migration and no entity change).
  Every existing user updating via Play therefore hit `fallbackToDestructiveMigration()`:
  a **silent total wipe of local data** on upgrade.
- Separately, Room opens the SQLite file **lazily**: `getInstance()` only builds the object.
  A corrupt / half-restored `piggy_ledger_db` file (cloud restore, killed write) then threw at
  whatever later caller touched it first — `MainActivity.onCreate` calls `getInstance()` on the
  main thread with no guard — i.e. a crash on every launch until the user wiped storage.

## Fix (`data/PiggyLedgerDatabase.kt`, same `getInstance()` signature — all callers untouched)
- Added the missing `MIGRATION_20_21` (empty: v21 has no schema change) and registered it, so
  20 → 21 upgrades **migrate instead of wiping**.
- `getInstance()` now forces the actual SQLite open (`openHelper.readableDatabase`) inside the
  existing synchronized block; if the file is unusable it closes, deletes
  (`deleteDatabase()` + file delete), rebuilds once, and returns a working DB instead of
  throwing into the caller's face.

## Why this doesn't break anything
- Public API unchanged (`getInstance(context)`); healthy devices take the identical path plus
  one cheap open check.
- A corrupt file previously meant an infinite crash loop; now it means one clean empty DB
  (server sync via `SyncManager` repopulates cloud-backed rows afterwards).
