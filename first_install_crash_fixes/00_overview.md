# First-install crash loop (Play Store) — diagnosis & fixes

Symptom: fresh Play Store download crashes on every launch until the user clears storage/cache.

## Root cause (crash loop, not a one-off bug)
Something written to disk poisoned every subsequent cold start; wiping deleted the poisoned file.
Three startup reads had no protection, and empty backup rules planted torn files even on "fresh"
installs. Each fix below is independent and documented in its own file:

1. `01_datastore_corruption_handler.md` — DataStore `preferences_pb` had no corruption handler;
   every read threw -> launch-gate crash loop.
2. `02_backup_rules.md` — empty backup/extraction rules restored WorkManager DB, SQLite WAL
   sidecars and download caches onto fresh installs.
3. `03_room_v21_and_corrupt_db.md` — v21 shipped with no `MIGRATION_20_21` (upgrade = silent data
   wipe) and Room opens lazily, so a bad DB file crashed a random later caller.
4. `04_startup_guards_and_sdk_keys.md` — scheduler/widgets/Play-update/SDK init unguarded at
   startup; placeholder API keys configured billing/analytics with invalid keys.
5. `05_onboarding_shown_again.md` — onboarding completion was fire-and-forget (navigate before
   the DataStore commit); saves are now durable-before-navigate, racing shortcut merged,
   dead `resetAppFlow()` landmine removed.

## Still needed outside code (release pipeline)
- Inject the real `google-services.json` + real `POSTHOG_API_KEY` / `REVENUECAT_API_KEY` /
  `CLERK_PUBLISHABLE_KEY` via CI secrets (never commit). The app now fails safe without them.
- After release, confirm in Play Console → Android vitals that the `CorruptionException` /
  `SQLiteException` / `WorkManager` crash clusters drop to zero.
- Suggested manual matrix before rollout: release bundle install → corrupt `preferences_pb` →
  relaunch; restore a v20 backup onto v21 → relaunch; device without Play services → relaunch.
