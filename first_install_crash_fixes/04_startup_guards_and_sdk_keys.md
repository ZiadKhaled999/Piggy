# Fix 04 — Startup guards: scheduler, widgets, Play updates, SDK keys

## Diagnosis
- `MainActivity.onCreate` called `NotificationScheduler.scheduleAll()`, three widget
  `triggerUpdate()`s, and Play-Core `registerListener`/`checkForAppUpdate()` with **no guards**.
  Any one of them throwing (corrupt WorkManager DB, widget provider hitting a bad DB, Play Core
  on a device without Play services) crashed the launch before first frame.
- `NotificationScheduler.scheduleWorkManager()` itself called `WorkManager.getInstance()` raw;
  the `Application` path was try-caught but this one was not.
- `PiggyLedgerRepository.triggerSync()` had the same raw `WorkManager.getInstance()` — a broken
  scheduler DB could crash the DB write that requested the sync.
- SDK init used `isNotBlank()` only, so `.env.example` placeholders (`goog_placeholder`,
  `phc_placeholder`) configured RevenueCat/PostHog with invalid keys on Play builds; Clerk ran
  with `enableDebugMode = true` hardcoded in production; Crashlytics was touched without
  checking `FirebaseApp` init (repo ships no `google-services.json`; CI must inject it).

## Fix
- `service/NotificationScheduler.kt`: `scheduleWorkManager()` wrapped in try/catch (alarms still
  schedule even if WorkManager is down); uses `applicationContext`.
- `data/PiggyLedgerRepository.kt` + `data/UserPreferences.kt::triggerSync`: sync enqueue is
  best-effort with try/catch + log.
- `MainActivity.kt`: `scheduleAll`, each widget update, `registerListener`/`checkForAppUpdate`,
  resume-check, `onDestroy` unregister, and the update Snackbar `completeUpdate()` all guarded;
  `checkForAppUpdate()` also gets `addOnFailureListener`; launch-gate assignment double-guarded.
- `PiggyLedgerApplication.kt`: Crashlytics only touched when `FirebaseApp.getApps()` is non-empty;
  PostHog/RevenueCap skipped for blank **or placeholder** keys (with a log); Clerk debug mode now
  follows `BuildConfig.DEBUG` and placeholder Clerk keys are skipped.

## Why this doesn't break anything
- Every change only converts a startup **crash** into a logged warning + degraded path (alarms
  without WorkManager, launch without widget refresh, analytics/billing disabled until real keys).
  Happy-path behavior is identical.
- Release action still needed (outside code): provide the real `google-services.json` and real
  `POSTHOG_API_KEY` / `REVENUECAT_API_KEY` / `CLERK_PUBLISHABLE_KEY` via CI secrets — the code now
  fails safe when they are missing instead of crash-looping.
