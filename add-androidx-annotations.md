# Task: Add AndroidX Annotations to runcheck

## What This Is

AndroidX Annotations (`androidx.annotation.*`) are metadata tags that enable Android's built-in lint checker to catch bugs at compile time — wrong resource types, missing permission checks, API level violations, threading mistakes. These complement Detekt (which checks Kotlin code quality) by catching Android-specific issues.

This task has two parts: retrofit annotations onto existing code, then update CLAUDE.md so all future code uses them automatically.

## Part 1: Add Annotations to Existing Code

Read `PROJECT.md` and `CLAUDE.md` before starting.

### @RequiresApi — API level safety

The app has `minSdk = 26` but uses APIs from 29, 30, 34+. Find every function that calls a higher-API method and add `@RequiresApi` to it.

Key areas to check:
- `PowerManager.getCurrentThermalStatus()` — API 29
- `PowerManager.getThermalHeadroom()` — API 30
- `BatteryManager.BATTERY_PROPERTY_CHARGING_CYCLE_COUNT` — API 34 (verify)
- `BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH` — API 34 (verify)
- `MediaStore.createDeleteRequest()` / `IntentSender` flow — API 30
- `UsageStatsManager` methods that require specific API levels
- Any `Build.VERSION.SDK_INT >= XX` checks — the function being called inside that check likely needs the annotation on its declaration

For each function:
1. Add `@RequiresApi(Build.VERSION_CODES.XX)` to the function declaration
2. Verify that all call sites have an appropriate API level check (`if (Build.VERSION.SDK_INT >= XX)`)
3. If a call site is missing the check, add it

Run `./gradlew lint` after this section to verify no new warnings.

### @RequiresPermission — permission safety

The app uses sensitive permissions. Find functions that directly call permission-protected Android APIs and annotate them.

Key permissions to trace:
- `ACCESS_FINE_LOCATION` — WiFi SSID reading (Android 12+)
- `ACCESS_NETWORK_STATE` — network monitoring
- `PACKAGE_USAGE_STATS` — app usage (special permission)
- `READ_PHONE_STATE` — if used for telephony/signal strength
- `POST_NOTIFICATIONS` — if notification channels are used
- Any other permissions declared in AndroidManifest.xml

For each:
1. Find the function that makes the actual system API call
2. Add `@RequiresPermission(Manifest.permission.XX)` to it
3. If multiple permissions are needed: use `@RequiresPermission(allOf = [...])` or `@RequiresPermission(anyOf = [...])`
4. Verify call sites check permission before calling

### @WorkerThread / @MainThread — threading safety

Find functions that perform blocking or long-running work and annotate them:

- Room DAO methods — already handled by Room, skip these
- Network operations (M-Lab speed test, DNS lookups) — `@WorkerThread`
- File I/O (CSV export, large file scanning, cleanup operations) — `@WorkerThread`
- MediaStore queries — `@WorkerThread`
- `UsageStatsManager` queries — `@WorkerThread`
- Composable functions — inherently `@MainThread`, no annotation needed
- ViewModel functions that launch coroutines — no annotation needed (coroutine handles threading)

Only annotate functions where the threading requirement isn't already enforced by the framework (Room, Compose, etc.).

### Resource type annotations — @StringRes, @DrawableRes, @ColorRes, @ColorInt

Find functions that accept Android resource IDs as Int parameters and annotate them:

- Functions taking string resource IDs → `@StringRes`
- Functions taking drawable resource IDs → `@DrawableRes`  
- Functions taking color resource IDs → `@ColorRes`
- Functions taking raw color Int values (not resource IDs) → `@ColorInt`
- Functions taking dimension values in pixels → `@Px`
- Functions taking any resource ID generically → `@AnyRes`

Focus on:
- Custom composable functions that accept `titleRes: Int` or similar — add `@StringRes`
- Theme/color utility functions that take color parameters — distinguish `@ColorRes` vs `@ColorInt`
- Navigation functions that take route arguments derived from resources

### @IntRange / @FloatRange — value bounds

Add range annotations where the valid range is known:
- Battery percentage: `@IntRange(from = 0, to = 100)`
- Health score: `@IntRange(from = 0, to = 100)` (verify actual range)
- Confidence values: annotate with actual valid range
- Temperature values: `@FloatRange` with plausible bounds if applicable
- Progress values: `@FloatRange(from = 0.0, to = 1.0)` if normalized

### @CheckResult — don't ignore return values

Add `@CheckResult` to functions where ignoring the return value is always a bug:
- Functions that compute and return a value without side effects
- Builder-pattern methods that return a new/modified object
- Functions that return success/failure status

Don't add it to functions that have side effects AND return a value (like Room insert returning a row ID).

## Part 2: Update CLAUDE.md

After completing Part 1, add an `## AndroidX Annotations` section to CLAUDE.md with these rules for all future code:

```markdown
## AndroidX Annotations

All new code must use AndroidX annotations where applicable:

- `@RequiresApi(Build.VERSION_CODES.XX)` on any function calling APIs above minSdk (26)
- `@RequiresPermission` on functions that call permission-protected Android APIs
- `@WorkerThread` on functions performing blocking I/O, network, or heavy computation (except Room DAOs and suspend functions)
- `@MainThread` on functions that must run on the UI thread (except Composables)
- `@StringRes`, `@DrawableRes`, `@ColorRes`, `@ColorInt` on Int parameters that represent resources or colors
- `@IntRange` / `@FloatRange` on parameters with known valid ranges (battery %, health score, confidence)
- `@CheckResult` on pure functions where ignoring the return value is a bug

Do not annotate:
- Room DAO methods (Room handles threading)
- Composable functions (inherently main thread)
- Suspend functions (caller controls threading via coroutine dispatcher)
- Private functions only called from one already-annotated place (avoid redundancy)
```

## Verification

After all changes:

1. `./gradlew assembleDebug` — must compile
2. `./gradlew lint` — check for new warnings introduced by annotations (these are GOOD — they reveal existing problems)
3. `./gradlew detekt` — must still pass (annotations don't affect Detekt)

If `lint` reveals new warnings from the annotations (e.g. "call requires API 30 but minSdk is 26"), those are real bugs that need fixing — the annotation exposed a missing version check. Fix them.

## Rules

- Read PROJECT.md and CLAUDE.md before starting
- Work through one annotation category at a time in the order listed above
- Don't over-annotate — skip cases where the framework already handles it
- If uncertain whether a function needs an annotation, skip it
- Run build verification after each category
