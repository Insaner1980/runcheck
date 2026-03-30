# AGENTS.md — runcheck

This file and `CODEX.md` should stay aligned. If one is updated, update the other in the same change.

Android device health diagnostics app. Kotlin + Jetpack Compose. Single dark theme.

When product/runtime facts or visual system rules matter, treat `PROJECT.md` and `UI-SPEC.md` as the authoritative companion docs and keep them aligned with code.

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
- Compile SDK: Android 17 beta (`CinnamonBun`)
- Target SDK: Android 17 beta (`CinnamonBun`)
- Min SDK: 26
- Java target: 17
- Localization: English-only (`localeFilters = ["en"]`)
- Build variants: `app/src/debug` and `app/src/release` source sets are active

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
├── Insights
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

- `RuncheckApp` initializes billing, Pro state, notification channels, screen-state tracking, periodic monitoring, and widget refresh hooks
- `RuncheckApp` also initializes source-set-specific `SentryInit`; debug builds may report to Sentry, release builds are a no-op and must remain telemetry-free
- WorkManager runs `HealthMonitorWorker` for snapshot collection + alert evaluation
- WorkManager runs `HealthMaintenanceWorker` for app-usage refresh, cleanup, and widget refresh
- WorkManager runs `InsightGenerationWorker` on the monitoring scheduler lifecycle to generate persisted Home insights from Room history
- `RealTimeMonitorService` is an opt-in live notification foreground service and must stay user-controlled from Settings
- Widgets are backed by Room snapshots and treated as a Pro feature
- Trial state currently counts as Pro access through `ProState.isPro`
- Home now includes a rule-driven Insights surface backed by Room-persisted insight rows; Home shows a curated subset of up to three items and the full list lives in the dedicated Insights screen
- Debug-only insight seeding and manual regeneration live behind debug source-set wiring and must stay release-inaccessible

State restoration conventions:

- Use `rememberSaveable` for screen-local UI state such as sheet visibility, dialogs, and metric chip selections
- Use `SavedStateHandle` for route-backed or process-death-sensitive state such as selected history period, cleanup type, and fullscreen chart args

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
- Pro features: Charger Comparison, Per-App Battery, Extended History, Thermal Logs, CSV Export, Widgets.
- Each must check `ProManager.isPro()` or the injected `ProStatusProvider` / `IsProUserUseCase` path before showing content.
- Locked state must use `ProFeatureLockedState` component, not custom implementations.
- The top-level Home Insights card is not a Pro feature. It may link into Pro-gated destinations, but the destinations themselves must remain gated.

### 5. Speed test
- Uses M-Lab NDT7 (`ndt7-client-android` Kotlin library). No other speed test backend.
- Never hardcode a fixed server — NDT7 auto-selects nearest global server.
- Cellular warning dialog must appear before test starts if active network is not WiFi.
- Outbound network calls are allowed only for user-initiated speed tests, latency measurement, and Google Play Billing.
- Reading current connection details (WiFi, 5G, SSID, signal, IP, DNS) must stay on-device via Android APIs and must not trigger socket, HTTP, or ping-style probes.

### 6. Animations
- All animations must check `LocalReducedMotion.current` (or `MaterialTheme.reducedMotion`) and skip/shorten if true.
- Standard durations: ProgressRing 1200ms, MiniBar 800ms, SegmentedBar 800ms, Thermometer 1200ms, Battery wave 2000ms loop.

### 7. UI consistency
- Background colors: BgPage `#0B1E24`, BgCard `#133040`, BgIconCircle `#1A3A48`.
- Alternate card background: BgCardAlt `#0F2A35`.
- Primary accent: Blue `#4A9EDE`.
- Secondary/status accent: Teal `#5DE4C7`.
- Gauge arcs must be neutral (white/gray) — not colored. Accent color is for the indicator only.
- Status colors (Teal/Blue/Orange/Red) are for small badges and status dots only, never for large fills.
- Typography: Manrope for body text, JetBrains Mono for hero numbers and gauge values.
- Card corner radius: 16dp. Small elements: 8dp. No shadows, no elevation, no borders (except ActionCards: 1dp outlineVariant at 35% alpha).
- No dynamic colors. If a task changes visual design, follow `UI-SPEC.md` instead of inventing alternate tokens or component variants.
- English-only strings are intentional right now. Do not reintroduce partial localization without updating docs and string coverage together.
- Icons: use `Icons.Outlined` exclusively — no `Icons.Default`, `Icons.Filled`, or `Icons.Rounded`
- All padding/spacing values must be on the 4dp grid (2/4/8/12/16/24/32dp)
- All animation durations must use `MotionTokens` constants, never bare `tween()` without explicit spec
- All ViewModels with live state flows must use `.sample(333L)` to throttle UI updates

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
- Any outbound network call outside the speed test flow, latency measurement, or billing
- Any release-path telemetry, crash reporting, or analytics expansion beyond the current debug-only Sentry setup

---

## What Not to Change

- App name is `runcheck` (lowercase). Never change to RunCheck, Runcheck, or any other casing.
- Single dark theme only. No light mode, no AMOLED toggle.
- No dynamic colors.
- One-time Pro purchase only. No subscription, no ads.
- English-only localization is intentional for now. Do not reintroduce partial Finnish strings ad hoc.
- Debug-only Sentry wiring exists for local/dev diagnostics; do not ship crash reporting, analytics, or tracking in release.
- NDT7 backend for speed tests. No alternatives.
- Minimum SDK: 26. Do not lower.

---

## Preferred Local Skills

If local Codex skills are installed, prefer:

- `runcheck-deep-review` for deep reviews, large change audits, LLM-generated Android code audits, and subtle API/lifecycle regression checks
- `runcheck-security-scan` for manifest, permission, exported-component, logging, secrets, and release-safety audits

---

## Low-CPU Verification

- This repository is often worked on with limited CPU headroom. Avoid heavy local verification by default.
- Do not run full Gradle builds or full test suites unless explicitly requested or required to unblock the task.
- Prefer static analysis, targeted file review, and minimal commands first.
- If verification is needed, use the smallest scoped check possible: one compile task, one module task, or one narrowly filtered test class.
- Avoid running multiple coding agents or tools that may build the same repo in parallel.
- When verification is intentionally skipped or minimized, say so clearly in the final response.
