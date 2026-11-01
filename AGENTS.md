# AGENTS.md

## Build & test

- JDK target 17; Gradle 9.3.1 wrapper. Android SDK expected at `/opt/android-sdk` (`local.properties`).
- `./gradlew :app:assembleDebug` — debug build.
- `./gradlew :app:testDebugUnitTest` — unit tests (Robolectric + Room in-memory).
- `./gradlew :app:testDebugUnitTest --tests "com.oryno.piggy_ledger.SmsParsingAndMatchingTest"` — single class.
- `./gradlew :app:lintDebug` — lint.
- `./gradlew bundleRelease` — release bundle (CI uses `gradle bundleRelease`).
- Secrets plugin reads `.env` (default `.env.example`). Builds pass without `.env`; keys fall back to placeholders and the app disables those SDKs.
- No `google-services.json` is committed; the google-services plugin is set to `WARN`. Firebase/Crashlytics init is guarded in `PiggyLedgerApplication`.

## Architecture

- Single module `:app`, package `com.oryno.piggy_ledger`.
- Room DB is at version 26 in `data/PiggyLedgerDatabase.kt`; migrations `MIGRATION_1_2` … `MIGRATION_25_26` are all registered. Add a migration for any schema change — do not rely on `fallbackToDestructiveMigration()`.
- `getInstance()` forces the SQLite open and recovers a corrupt file by deleting and rebuilding it.
- DataStore `user_prefs` uses `ReplaceFileCorruptionHandler` and a `safeData` stream; route reads through it.
- Backup/extraction rules (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`) exclude WorkManager DB, WAL sidecars, and caches. Keep those excludes when editing backup config.
- Cloud sync is tombstone-based: `is_deleted` rows push through normal upserts. The `DELETE /sync/delete` route is dead. `deleteAccount`/`deleteGoal` must call `triggerSync()`; `wipeLocalData()` cancels sync workers before clearing tables.

## Conventions & gotchas

- Kotlin code style is `official`; configuration cache is enabled; Kotlin/KSP incremental compilation is disabled.
- `.env.example` placeholders contain `placeholder`; `PiggyLedgerApplication` treats those keys as missing for PostHog, RevenueCat, and Clerk.
- Clerk debug mode follows `BuildConfig.DEBUG`.
- Instrumented tests (`androidTest`) need a device/emulator; unit tests run under Robolectric.

## Reference docs

- `fixes/` and `first_install_crash_fixes/` contain verified diagnosis and fix notes for sync, startup, backup, Room, and onboarding. Read them before changing those areas.