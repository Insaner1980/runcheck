runcheck Codex Instructions

## Scope

These instructions are for agents working in this repository. Keep this file aligned with `AGENTS.md`. If they ever conflict, resolve the mismatch instead of following two different rule sets.

runcheck is an Android device health diagnostics app built with Kotlin and Jetpack Compose. Product direction is fixed:

- App name is `runcheck` in lowercase
- Single dark theme only
- One-time Pro purchase only
- No subscriptions
- No ads as a product direction
- NDT7 is the speed test backend
- Minimum SDK stays 26

Legacy billing or ad-related code may still exist in the repo. Do not expand that surface unless the task is explicitly about cleanup or migration.

## Current Project Snapshot

- Package root: `com.runcheck`
- Main module: single `app` module
- Architecture: Clean Architecture with `data/`, `domain/`, and `ui/`
- Dependency injection: Hilt
- Database: Room
- UI: Jetpack Compose + Material 3
- Background work: WorkManager
- Widgets: Glance
- Speed test: M-Lab NDT7 (`ndt7-client-android`)
- Build: Gradle Kotlin DSL
- Compile SDK: 36
- Target SDK: 35
- Min SDK: 26

High-level package layout:

```text
app/src/main/java/com/runcheck/
├── data/
├── domain/
├── ui/
├── billing/
├── pro/
├── di/
├── service/
├── worker/
├── widget/
└── util/
```

## Architecture Rules

- `data/` owns Android framework access, persistence, device APIs, and external SDK integration
- `domain/` contains business logic, use cases, repository contracts, and domain models
- `domain/` must not import `android.*` or concrete `data/` implementations
- `ui/` contains Compose screens, components, navigation, and ViewModels
- `ui/` must not bypass use cases or repository contracts to talk directly to data sources
- ViewModels are the bridge between `ui/` and `domain/`

Prefer targeted changes that preserve the existing structure instead of cross-layer rewrites.

## Review Priorities

When reviewing or modifying code, check these first and in this order.

### 1. Layer boundaries

- Flag any `domain/` dependency on Android or `data/`
- Flag any `ui/` dependency that bypasses the ViewModel/use-case path
- Keep business logic out of composables

### 2. Measurement reliability

- Every sensor-facing value must use `MeasuredValue<T>`
- Confidence must be `ACCURATE`, `ESTIMATED`, or `UNAVAILABLE`
- Raw values must not be shown without a confidence indicator such as `ConfidenceBadge`
- Validate `BATTERY_PROPERTY_CURRENT_NOW` with repeated reads, non-zero checks, plausible range `-10000..+10000 mA`, and charge-state sign sanity
- Thermal data must use `PowerManager.getCurrentThermalStatus()` on API 29+ and `getThermalHeadroom()` on API 30+
- Do not add sysfs-based thermal reads

### 3. API level guards

- Guard API 29+ calls with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q`
- Guard API 30+ calls with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.R`
- Guard API 34+ calls with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE`
- Min SDK is 26, so anything above that needs an explicit guard or safe fallback

### 4. Pro gating

Pro features are:

- Charger Comparison
- App Usage
- Extended History
- Thermal Logs
- CSV Export
- Widgets
- Remaining Charge Time

Rules:

- Check `ProManager.isPro()` or the injected `ProStatusProvider` equivalent before exposing the feature
- Use `ProFeatureLockedState` for locked UI, not a custom replacement
- Preserve the one-time purchase model

### 5. Speed test

- Use M-Lab NDT7 only
- Do not hardcode a server; NDT7 chooses the nearest server
- Show the cellular warning before the test starts when the active network is not Wi-Fi

### 6. Motion and animation

- All animations must respect `LocalReducedMotion.current` or `MaterialTheme.reducedMotion`
- Skip or shorten motion when reduced motion is enabled
- Standard durations:
  - `ProgressRing`: 1200ms
  - `MiniBar`: 800ms
  - `SegmentedBar`: 800ms
  - `Thermometer`: 1200ms
  - Battery wave: 2000ms loop

### 7. UI consistency

- Background colors:
  - `BgPage` `#0B1E24`
  - `BgCard` `#133040`
  - `BgIconCircle` `#1A3A4D`
- Primary accent: teal `#5DE4C7`
- Gauge arcs stay neutral white/gray; accent color is only for the indicator
- Status colors are for small badges and dots, not large fills
- Typography:
  - Manrope for body text
  - JetBrains Mono for hero numbers and gauge values
- Card radius: 16dp
- Small-element radius: 8dp
- No shadows, no elevation, no borders, except `ActionCards` with `1dp outlineVariant` at `35%` alpha

### 8. Accessibility

- Minimum touch target: 48dp
- Charts, rings, bars, and similar visuals need content descriptions
- Status must never rely on color alone; pair it with text or icon

## What To Flag

Raise a review comment or fix request for any of these:

- Layer boundary violations
- Missing API guards
- Sensor data shown without `MeasuredValue` or `ConfidenceBadge`
- Pro content exposed without `isPro()` gating
- Animation ignoring reduced motion
- Hardcoded colors that do not match the palette
- Touch targets smaller than 48dp
- Sysfs-based thermal reads
- NDT7 speed tests pinned to a fixed server

## Working Conventions

- Prefer explicit imports
- Avoid wildcard imports
- Keep comments in English
- Avoid `!!`
- Put user-facing strings in resources
- Keep composables small and focused
- Keep ViewModel state explicit and testable
- Prefer minimal, targeted edits over broad rewrites

## Practical Build Notes

- Kotlin version comes from `gradle/libs.versions.toml`
- Compose uses the BOM defined in the version catalog
- Hilt, Room, KSP, ktlint, and detekt are already wired into the build
- Crash reporting code may exist, but do not add analytics or tracking behavior

Useful local commands:

- `./gradlew test`
- `./gradlew ktlintCheck detekt`
- `./gradlew assembleDebug`
