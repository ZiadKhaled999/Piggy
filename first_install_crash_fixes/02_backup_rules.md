# Fix 02 — Auto Backup restored torn files onto "fresh" Play installs

## Diagnosis
- Manifest has `allowBackup="true"` but both rule files were effectively empty:
  `backup_rules.xml` had an empty `<full-backup-content>`, `data_extraction_rules.xml` an empty
  `<cloud-backup>` block. Empty = restore **everything**.
- So Google silently restored the WorkManager DB, SQLite `-shm`/`-wal` sidecars, and downloaded
  mascot/remote-config caches onto what the user experiences as a "first-time download".
- A backup captured mid-write (or a `-wal` without its matching main DB file) is torn on restore:
  WorkManager/DataStore/Room reads then throw on every cold start until the user wipes storage.

## Fix (user data still restores)
- `res/xml/backup_rules.xml` + `res/xml/data_extraction_rules.xml` (`cloud-backup` and
  `device-transfer`): added `<exclude>` entries only — no previously-backed-up user data was
  removed from backup:
  - `androidx.work.workdb*` + `androidx.work.util.preferences.xml` (device-specific scheduler state)
  - `piggy_ledger_db-shm / -wal / -journal` (WAL sidecars; the main DB file still restores and
    SQLite recovers cleanly without stale sidecars)
  - `files/mascots/` + `piggy_remote_config_cache.json` (refetchable downloads)

## Why this doesn't break anything
- Pure `<exclude>` additions for volatile/refetchable files; onboarding prefs, DataStore, and the
  Room main DB keep their previous backup behavior.
- Combined with Fix 01 (corruption handler) and Fix 03 (safe Room open), even a stale restore that
  slips through can no longer crash-loop.
