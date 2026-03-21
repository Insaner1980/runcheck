# AGENTS.md — runcheck

This file and `CODEX.md` should stay aligned. If one is updated, update the other in the same change.

Android device health diagnostics app. Kotlin + Jetpack Compose. Single dark theme.

---

## Architecture

Clean Architecture with three layers:

- `data/` — Android framework APIs, Room database, BatteryManager, TelephonyManager, StorageStatsManager, PowerManager
- `domain/` — Business logic, use cases, domain models. No Android imports.
- `ui/` — Jetpack Compose screens and components. No direct data layer access.

Dependency injection: Hilt. Database: Room. UI: Jetpack Compose + Material 3.

---

## Current Project Snapshot

- Package root: `com.runcheck`
- Main module: single `app` module
- Architecture: Clean Architecture with `data/`, `domain/`, and `ui/`
- Dependency injection: Hilt
- Database: Room
- Preferences: DataStore
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

Current navigation snapshot:

```text
Home
├── Battery Detail
│   ├── Charger Comparison [PRO]
│   └── Fullscreen Chart
├── Network Detail
│   ├── Speed Test
│   └── Fullscreen Chart
├── Thermal Detail
├── Storage Detail
│   └── Cleanup/{type}
├── App Usage [PRO]
├── Learn
│   └── Learn Article
├── Settings
└── Pro Upgrade
```

Current runtime systems:

- `RuncheckApp` initializes billing, Pro state, notification channels, crash reporting, screen-state tracking, periodic monitoring, and widget refresh hooks
- WorkManager runs `HealthMonitorWorker` for snapshot collection + alert evaluation
- WorkManager runs `HealthMaintenanceWorker` for app-usage refresh, cleanup, and widget refresh
- Widgets are backed by Room snapshots and treated as a Pro feature
- Trial state currently counts as Pro access through `ProState.isPro`

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
- Outbound network calls are allowed only for user-initiated speed tests and Crashlytics when the user has enabled crash reporting.
- Reading current connection details (WiFi, 5G, SSID, signal, IP, DNS) must stay on-device via Android APIs and must not trigger socket, HTTP, or ping-style probes.

### 6. Animations
- All animations must check `LocalReducedMotion.current` (or `MaterialTheme.reducedMotion`) and skip/shorten if true.
- Standard durations: ProgressRing 1200ms, MiniBar 800ms, SegmentedBar 800ms, Thermometer 1200ms, Battery wave 2000ms loop.

### 7. UI consistency
- Background colors: BgPage `#0B1E24`, BgCard `#133040`, BgIconCircle `#1A3A48`.
- Primary accent: Blue `#4A9EDE`.
- Secondary/status accent: Teal `#5DE4C7`.
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
- Any outbound network call outside the speed test flow or toggle-controlled Crashlytics

---

## What Not to Change

- App name is `runcheck` (lowercase). Never change to RunCheck, Runcheck, or any other casing.
- Single dark theme only. No light mode, no AMOLED toggle.
- One-time Pro purchase only. No subscription, no ads.
- NDT7 backend for speed tests. No alternatives.
- Minimum SDK: 26. Do not lower.

---

## Low-CPU Verification

- This repository is often worked on with limited CPU headroom. Avoid heavy local verification by default.
- Do not run full Gradle builds or full test suites unless explicitly requested or required to unblock the task.
- Prefer static analysis, targeted file review, and minimal commands first.
- If verification is needed, use the smallest scoped check possible: one compile task, one module task, or one narrowly filtered test class.
- Avoid running multiple coding agents or tools that may build the same repo in parallel.
- When verification is intentionally skipped or minimized, say so clearly in the final response.
