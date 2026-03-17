# AGENTS.md — runcheck

Android device health diagnostics app. Kotlin + Jetpack Compose. Single dark theme.

---

## Architecture

Clean Architecture with three layers:

- `data/` — Android framework APIs, Room database, BatteryManager, TelephonyManager, StorageStatsManager, PowerManager
- `domain/` — Business logic, use cases, domain models. No Android imports.
- `ui/` — Jetpack Compose screens and components. No direct data layer access.

Dependency injection: Hilt. Database: Room. UI: Jetpack Compose + Material 3.

---

## Code Review Priorities

When reviewing a PR or file, check for these in order:

### 1. Layer violations
- Does `domain/` import anything from `android.*` or `data/`?
- Does `ui/` call data sources directly, bypassing use cases?
- Are ViewModels the only bridge between `ui/` and `domain/`?

### 2. Measurement reliability
- Every sensor value must be wrapped in `MeasuredValue<T>` with a confidence level: `ACCURATE`, `ESTIMATED`, or `UNAVAILABLE`.
- Raw values must never be shown to the user without a confidence indicator (ConfidenceBadge component).
- `BATTERY_PROPERTY_CURRENT_NOW` must be validated: multiple reads, non-zero, range -10000..+10000 mA, sign matches charge state.
- Thermal data must use `PowerManager.getCurrentThermalStatus()` (API 29+) and `getThermalHeadroom()` (API 30+). No sysfs reads — SELinux blocks these on modern Android.

### 3. API level guards
- All API 29+ calls guarded with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q`.
- All API 30+ calls guarded with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.R`.
- All API 34+ calls guarded with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE`.
- Minimum SDK is 26. No calls to APIs below 26 without a fallback.

### 4. Pro feature gating
- Pro features: Charger Comparison, App Usage, Extended History, Thermal Logs, CSV Export, Widgets, Remaining Charge Time.
- Each must check `ProManager.isPro()` before showing content.
- Locked state must use `ProFeatureLockedState` component, not custom implementations.

### 5. Speed test
- Uses M-Lab NDT7 (`ndt7-client-android` Kotlin library). No other speed test backend.
- Never hardcode a fixed server — NDT7 auto-selects nearest global server.
- Cellular warning dialog must appear before test starts if active network is not WiFi.

### 6. Animations
- All animations must check `LocalReducedMotion.current` (or `MaterialTheme.reducedMotion`) and skip/shorten if true.
- Standard durations: ProgressRing 1200ms, MiniBar 800ms, SegmentedBar 800ms, Thermometer 1200ms, Battery wave 2000ms loop.

### 7. UI consistency
- Background colors: BgPage `#0B1E24`, BgCard `#133040`, BgIconCircle `#1A3A4D`.
- Primary accent: Teal `#5DE4C7`.
- Gauge arcs must be neutral (white/gray) — not colored. Accent color is for the indicator only.
- Status colors (Teal/Blue/Orange/Red) are for small badges and status dots only, never for large fills.
- Typography: Manrope for body text, JetBrains Mono for hero numbers and gauge values.
- Card corner radius: 16dp. Small elements: 8dp. No shadows, no elevation, no borders (except ActionCards: 1dp outlineVariant at 35% alpha).

### 8. Accessibility
- Minimum touch target: 48dp.
- All visual elements (charts, rings, bars) must have content descriptions.
- Status information must never rely on color alone — always paired with text or icon.

---

## What to Flag

Raise a review comment for any of the following:

- Layer violation (data/domain/ui boundary crossed)
- Missing API level guard on a version-gated API
- Sensor value shown without MeasuredValue wrapper or ConfidenceBadge
- Pro feature accessible without `isPro()` check
- Animation missing reduced motion check
- Hardcoded color hex that doesn't match the palette above
- Touch target smaller than 48dp
- Sysfs read for thermal data (use PowerManager API instead)
- NDT7 speed test using a fixed server URL

---

## What Not to Change

- App name is `runcheck` (lowercase). Never change to RunCheck, Runcheck, or any other casing.
- Single dark theme only. No light mode, no AMOLED toggle.
- One-time Pro purchase only. No subscription, no ads.
- NDT7 backend for speed tests. No alternatives.
- Minimum SDK: 26. Do not lower.
