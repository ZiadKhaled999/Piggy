# Fix 05 — Onboarding shown again after it was completed (+ signed in)

## Diagnosis
Traced every writer/reader of the gate flags (`has_onboarded`, `is_authenticated`,
`has_language_selected`, `has_heard_about_us`):

- `getInitialDestination()` (`data/UserPreferences.kt`) shows Onboarding only when
  authenticated=false, language+heard=true, onboarded=false. Sign-out preserves the
  onboarding flags (`clearForLogout`), so sign-out correctly lands on Auth — exonerated.
- Cloud sync (`SyncManager` → `applyFromEntity`) only ever flips flags false→true, never
  true→false — exonerated.
- `StreakManager.clear()` touches only `piggy_streak_prefs` — exonerated.
- **Found (the bug): completion was fire-and-forget.** `completeOnboarding()` /
  `completeLanguageSelection()` / `completeHearAboutUs()` launched a `viewModelScope`
  coroutine and the UI **navigated away in the same click handler without waiting**.
  If the process died or the app crashed in that window (common during the crash-loop era),
  the DataStore edit never committed: next launch reads onboarded=false with
  language+heard=true → Onboarding again, even though the user "completed" it.
- **Found (racing shortcut):** "Already have an account" fired three independent concurrent
  coroutines (each doing its own DataStore edit + read-modify-write cloud snapshot) and
  navigated immediately — same loss window plus interleaved cloud snapshots.
- **Found (landmine):** dead `resetAppFlow()` had no callers but wrote
  `saveOnboarding(false)` + `saveLanguageSelected(false)` — the exact reported symptom if
  anyone ever wired it to a button. Removed.
- Expected (not a bug): after Clear storage / uninstall-reinstall without restore / new
  device, all flags are gone and onboarding correctly reappears.

## Fix
- `ui/PiggyLedgerViewModel.kt`: the three `complete*` functions take `onSaved: () -> Unit = {}`
  (default keeps every other caller source-compatible), invoked after the DataStore commit;
  analytics/DB side-effects wrapped so they can't sink the flag save. New single-coroutine
  `completeAlreadyHaveAccount()` replaces the three racing launches. Deleted dead `resetAppFlow()`.
- `ui/PiggyLedgerApp.kt`: Language/HearAboutUs/Onboarding/Already-have-account navigate
  **inside `onSaved`**, i.e. only after the flag is durable. The existing auto-advance
  `LaunchedEffect` (onboarded && !authenticated → Auth) is kept as a backstop.

## Why this doesn't break anything
- Default `onSaved = {}` preserves old call behavior for any other caller; happy-path order is
  identical, just durable (extra latency is one DataStore commit, milliseconds).
- If the ViewModel is cleared before the save finishes (user exits), `onSaved` never runs —
  correct, the screen is gone.
- No routing logic changed: first-run, skip, sign-out, and reinstall flows resolve exactly as before.
