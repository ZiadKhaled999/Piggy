# Fix 01 — DataStore crash loop (`user_prefs.preferences_pb`)

## Diagnosis
- `Context.dataStore` was created with `preferencesDataStore(name = "user_prefs")` and **no
  `corruptionHandler`**.
- A torn write (process kill during first save, or a cloud-backup restore of a half-written
  `preferences_pb`) makes **every** `data.first()` / collector throw `CorruptionException` /
  `IOException`.
- `MainActivity` reads it in the launch gate (`lifecycleScope.launch { getInitialDestination() }`)
  with no try/catch, so the app crashed on every cold start (splash never dismissed) until the
  user wiped storage, which deletes the corrupt file. Same for the lock-status and auth observers.

## Fix (no behavior change on healthy devices)
- `data/UserPreferences.kt`
  - Added `ReplaceFileCorruptionHandler { emptyPreferences() }` to the delegate: a corrupt file is
    atomically replaced with defaults instead of throwing forever.
  - Added a private `safeData` stream (`.catch { emit(emptyPreferences()) }`) and routed **all**
    preference flows + `first()` reads through it.
  - `getInitialDestination()` now catches everything and falls back to `Screen.LanguageSelection`
    instead of crashing the splash gate.
  - `triggerSync()` WorkManager enqueue wrapped in try/catch so a broken WorkManager DB can't crash
    a DataStore save.

## Why this doesn't break anything
- On a healthy file the handler/catch never trigger; values and navigation order are unchanged.
- On a corrupt file the old behavior was an infinite crash loop; the new behavior is a clean
  first-run onboarding state.
