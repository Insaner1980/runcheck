# runcheck - Project Status

**Date:** 2026-06-23
**Version:** 1.0.0 (versionCode 1)
**Build configuration:** Gradle wrapper 9.4.0, AGP 9.1.1, Kotlin plugin 2.3.0, Kotlin runtime constraints 2.3.20, Compose BOM 2026.03.00
**Android SDK:** Android 17 (API 37) compile/target, minSdk 26
**Database:** Room schema version 10 with exported schemas for versions 6-10
**Test surface:** 74 JVM unit-test files and 1 instrumented migration-test file in the live checkout

---

## Implemented Features

### Core Monitoring

**Battery**
- Real-time level, temperature, voltage, current (mA), power (W), charge status, plug type, and health indicators
- Battery current reliability validation with confidence labels surfaced to users
- Battery health percentage, cycle-count support where available, design/estimated capacity paths, and manufacturer-specific handling
- Screen on/off drain tracking, deep sleep / held-awake analysis, long-term statistics, and charging-session summaries
- History charts for supported periods plus live current/power charting during charging
- Pro-only remaining charge time estimates when the current session data is meaningful

**Network**
- Wi-Fi/cellular type detection, signal strength, signal bars, SSID/IP/DNS details where Android permits access
- On-device connection detail collection plus explicit latency measurement
- User-initiated M-Lab NDT7 speed tests with download/upload, ping/jitter, cellular warning, and history
- Network history with Pro-aware retention/period behavior

**Thermal**
- PowerManager thermal status and headroom paths with battery/CPU temperature display where available
- Temperature history, min/max tracking, and thermal status indicators
- Pro-gated throttling log backed by `throttling_events`
- Throttling events can store the foreground app through the app-usage foreground provider

**Storage**
- Used/available storage, media breakdown, fill-rate projection, SD-card detection, and storage technical details
- Pro-gated cleanup tools for large files, old downloads, APK files, and trash where supported by the Android version
- Cleanup flow uses category-grouped file lists, thumbnails, delete requests, and before/after projection

### Home And Navigation

- Home dashboard with unified health score, metric cards, quick tools, trial/Pro state surfaces, stale-monitoring state, and pull-to-refresh
- Dedicated screens for Insights, Battery, Network, Thermal, Storage, Cleanup, Charger Comparison, App Usage, Learn, Settings, Pro Upgrade, and fullscreen charts
- Route-backed and process-death-sensitive state is handled through `SavedStateHandle` where needed

### Insights

- Room-backed Insights Engine with persisted `insights` rows and `InsightGenerationWorker` scheduled through `MonitorScheduler`
- Home shows a curated subset of up to three visible insights; the dedicated Insights screen shows the full visible active list
- Implemented rules:
  - `BatteryDegradationTrendRule`
  - `BaselineAnomalyRule`
  - `AppBatteryImpactRule`
  - `ChargerPerformanceRule`
  - `StoragePressureProjectionRule`
  - `RecurringThermalThrottlingRule`
  - `HeavyAppUsageRule`
  - `NetworkSignalPatternRule`
  - `NetworkDrivenBatteryDrainRule`
  - `HeatAcceleratedBatteryWearRule`
  - `StoragePressureImpactRule`
  - `ThermalPatternDetectionRule`
- Insights are generally available, but insights targeting Pro-only destinations are filtered for free users:
  - `APP_USAGE`
  - `CHARGER`
- Debug builds expose source-set-specific Insight seeding/regeneration tools; release builds bind a no-op `ReleaseSafeInsightDebugActions`

### Pro And Monetization

- One-time Google Play Billing product ID path for `runcheck_pro`
- Effective Pro access is trial-aware: active trial and purchased Pro both count as Pro
- Debug billing path can auto-enable Pro for local development
- Current `ProFeature` enum entries:
  - Extended History
  - Charger Comparison
  - Per-App Battery
  - Widgets
  - CSV Export
  - Thermal Logs
  - Remaining Charge Time
  - Storage Cleanup

### Background Work, Notifications, And Widgets

- `HealthMonitorWorker` collects snapshots and evaluates alerts
- `HealthMaintenanceWorker` refreshes app-usage data, performs cleanup, and refreshes widgets
- `InsightGenerationWorker` regenerates persisted insights on the monitoring scheduler lifecycle
- `RealTimeMonitorService` is an opt-in foreground service controlled from Settings
- Alerts cover low battery, high temperature, low storage, and charge complete with settings-controlled thresholds/toggles
- Glance widgets are present and treated as a Pro feature

### Settings And Data

- Settings include monitoring interval, notification toggles, live notification metric toggles, temperature unit, data retention, CSV export, data clearing, Pro state, device profile, and debug Insights availability where applicable
- DataStore stores preferences and feature state
- Room stores battery, network, thermal, throttling, storage, charger, speed-test, app-usage, device, and insight data
- `PACKAGE_USAGE_STATS` is declared in the manifest for app-usage based features

### Code Quality And Security

- Local wrapper tooling exists for lint, security, dependency, duplicate, Compose stability, Google Android lint, Sonar, and related checks
- GitHub workflows exist for CodeQL, Qodana, security/dependency scanning, and SonarCloud
- Release builds use R8/ProGuard configuration
- Release path must remain telemetry-free; debug-only Sentry wiring is source-set separated
- No analytics/tracking path is part of the current product contract

---

## Not Yet Implemented Or Still External

### Insights Follow-Ups

- The current `InsightEngine` does not enforce a global "maximum one new insight per day across all rules" cap. It evaluates every bound rule, replaces results per rule, and leaves visible Home curation to `InsightHomeRankingPolicy`.

### Localization

- The app is intentionally English-only in the current build.
- No multi-language resource set is active.

### Play Store And Release Assets

- Feature graphic and screenshot assets were not found in the repository.
- Play Store listing text exists in `docs/play-store-listing.md`, but repository files cannot prove whether it has been submitted.
- Privacy policy exists in `docs/privacy-policy.md`, but repository files cannot prove whether it is hosted at a public URL.
- `docs/release-checklist.md` still tracks open Play Console, signing, privacy policy, asset, and upload steps.
- No release upload workflow was found; existing CI workflows focus on analysis/security/quality gates rather than automated release publishing.

### Testing Gaps

- Unit coverage is broader than the old status file stated, but this is still not a complete end-to-end matrix.
- No dedicated Compose UI test suite was found.
- No full device compatibility matrix was found.
- The Room migration test is instrumented and requires a connected emulator/device.

---

## Project Structure Snapshot

```text
Main module: app
Package root: com.runcheck
Source sets: main, debug, release
Room schema version: 10
Room entity files: 11
Domain use-case files: 37
ViewModel files: 12
JVM unit-test files: 74
Instrumented test files: 1
```

---

## Key Documentation

| File | Content |
|------|---------|
| `PROJECT.md` | Main live-state project snapshot and review reference |
| `AGENTS.md` / `CODEX.md` | Agent instructions, architecture rules, tooling, and review priorities |
| `UI-SPEC.md` | Visual system and UI behavior reference |
| `INSIGHTS_REMAINING.md` | Current Insights rollout follow-up tracker |
| `insights-engine-phase1-spec.md` | Historical Phase 1 Insights implementation spec |
| `insights-engine-phase2-spec.md` | Historical/future Phase 2 Insights rule spec; not all claims are current runtime behavior |
| `docs/release-checklist.md` | Release build and Play Store checklist |
| `docs/play-store-listing.md` | Draft Play Store listing copy |
| `docs/privacy-policy.md` | Draft privacy policy |
| `docs/CHANGELOG.md` | Generated changelog from git history |
