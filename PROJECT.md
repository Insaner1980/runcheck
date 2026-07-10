# runcheck — Project Overview

Android device health diagnostics app built with Kotlin and Jetpack Compose. Single dark theme, one-time Pro purchase, no subscriptions.

---

## Technical Snapshot

- Package root: `com.runcheck`
- Application ID: `com.runcheck`
- Current app version: `versionName = "1.0.0"`, `versionCode = 1`
- Main module: single `app` module
- Architecture: Clean Architecture with `data/`, `domain/`, and `ui/`
- Dependency injection: Hilt
- Database: Room (`RuncheckDatabase` schema version 10, exported schema enabled)
- Preferences: DataStore
- Background work: WorkManager
- Widgets: Glance app widgets
- Speed test backend: M-Lab NDT7
- Build: Gradle Kotlin DSL
- Build tooling: Gradle wrapper 9.4.0, AGP 9.1.1, Kotlin Gradle/Compose plugin 2.3.0, Kotlin runtime constraints 2.3.20, KSP 2.3.9, Compose BOM 2026.03.00
- Compile SDK: Android 17 (API 37)
- Target SDK: Android 17 (API 37)
- Min SDK: 26
- Java target: 17
- Localization: English-only (`localeFilters = ["en"]`)
- Build variants: `app/src/debug` and `app/src/release` source sets are active
- Release signing: optional until a release artifact is requested, then `RUNCHECK_KEYSTORE_PATH`, `RUNCHECK_KEYSTORE_PASSWORD`, `RUNCHECK_KEY_ALIAS`, and `RUNCHECK_KEY_PASSWORD` are required; signed release artifact tasks must run with `--no-configuration-cache` and must provide the latest published versionCode through `--project-prop=runcheck.releaseVersionCodeFloor=<code>` or `RUNCHECK_RELEASE_VERSION_CODE_FLOOR`

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

Debug/release-specific insight tooling also lives outside the shared main source tree:

- `app/src/debug/java/com/runcheck/debug/insights/` for debug implementations
- `app/src/debug/java/com/runcheck/di/InsightDebugModule.kt` for debug Hilt bindings
- `app/src/release/java/com/runcheck/di/InsightDebugModule.kt` for release Hilt bindings
- `app/src/main/java/com/runcheck/debug/insights/` for release-safe public stubs used by shared code
- `app/src/release/java/com/runcheck/SentryInit.kt` for release no-op Sentry initialization

### Build and dependency source of truth

- `gradle/libs.versions.toml` is the source of truth for dependency and plugin versions.
- `gradle/wrapper/gradle-wrapper.properties` pins Gradle to `9.4.0`.
- `settings.gradle.kts` enforces centralized repositories with `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
- Approved repositories are `google()`, `mavenCentral()`, and JitPack only for `com.github.m-lab`.
- The app intentionally uses `org.jetbrains.kotlin.plugin.compose`; do not reintroduce `kotlin-android` unless the AGP/Kotlin integration model changes and is verified.
- Detekt uses the Detekt 2 plugin id `dev.detekt`; do not apply old Detekt 1 plugin assumptions without checking `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- `debug.credentials.properties` is ignored local-only input for debug Sentry DSN. It is read through Gradle Providers API and must not be committed.
- `localeFilters = ["en"]` and `app/src/main/res/xml/locales_config.xml` both encode the English-only product state.

Current version catalog highlights:

| Area | Current value |
|------|---------------|
| Gradle wrapper | `9.4.0` |
| Android Gradle Plugin | `9.1.1` |
| Kotlin Gradle / Compose plugin | `2.3.0` |
| Kotlin runtime constraints | `2.3.20` |
| KSP | `2.3.9` |
| Hilt | `2.59.2` |
| Hilt AndroidX / Hilt Work | `1.3.0` |
| Room | `2.8.4` |
| Compose BOM | `2026.03.00` |
| Navigation Compose | `2.9.7` |
| Lifecycle | `2.10.0` |
| Activity Compose | `1.12.3` |
| Core KTX | `1.18.0` |
| WorkManager | `2.11.2` |
| DataStore | `1.2.1` |
| Paging | `3.3.6` |
| Play Billing | `8.3.0` |
| Glance | `1.1.1` |
| M-Lab NDT7 client | `e0cb663613eb252a7793216ad28cf54a35677b8f` |
| OkHttp | `4.12.0` |
| Gson | `2.11.0` |
| kotlinx.serialization JSON | `1.8.1` |
| Sentry debug-only core | `8.43.1` |
| Dependency Analysis Gradle plugin | `3.15.0` |
| ktlint rule engine | `1.8.0` |
| ktlint Gradle plugin | `14.2.0` |
| Detekt | `2.0.0-alpha.3` |
| compose-rules for ktlint | `0.5.9` |
| compose-rules for Detekt | `0.5.9` |
| OWASP Dependency-Check Gradle plugin | `12.2.2` |
| SonarQube Gradle plugin | `7.3.1.8318` |
| Compose Stability Analyzer | `0.7.4` |
| Google Android Security Lints | `1.0.4` |
| JaCoCo | `0.8.14` |

### External version review snapshot

Checked on 2026-07-02 against official Android, Kotlin, Compose, and Gradle documentation:

- The repo is not on the newest available Android/Gradle ecosystem versions in every area; that is expected and should be reviewed deliberately.
- AGP 9.1.x official notes list API 37 support and Gradle 9.3.1 minimum compatibility; this repo's Gradle 9.4.0 wrapper satisfies that baseline.
- AGP 9.2.0 lists API 37.0 support and Gradle 9.4.1 as its minimum/default Gradle line; AGP 9.3.0 lists API 37 support and Gradle 9.5.0 as its minimum/default Gradle line. This repo currently uses AGP 9.1.1 with Gradle 9.4.0, so AGP upgrades should be evaluated with Android 17 stable SDK behavior, Qodana, CodeQL, Gradle wrapper, dependency verification, and Kotlin plugin behavior together.
- Kotlin 2.3.20 is available upstream and the repo already constrains Kotlin stdlib adapter/runtime artifacts to 2.3.20, but the Gradle/Compose plugin remains 2.3.0. Do not treat this as a typo without checking AGP/Qodana/CodeQL runner behavior.
- Kotlin 2.4.0 is newer than this repo's Kotlin plugin line, and Kotlin's official Gradle compatibility table lists support through Gradle 9.5.0 for current 2.4.0-era features. Treat it as a deliberate migration, not a routine patch bump.
- Compose BOM controls Compose library versions, but the Compose compiler is managed through the Kotlin plugin in Kotlin 2.0+ projects. Any BOM bump should include Compose UI regression review, compose-rules compatibility, and dependency verification metadata.
- Gradle 9.6.1 is available upstream; this repo currently uses 9.4.0. Wrapper bumps should be checked against AGP and all local wrappers.
- Android 17 uses stable API level 37 in this checkout. Android 17 SDK setup expects the Android 17 platform and Android SDK Build-Tools 37.x line to be installed locally.

External references used for this snapshot:

- Android 17 overview: <https://developer.android.com/about/versions/17>
- Android 17 SDK setup: <https://developer.android.com/about/versions/17/setup-sdk>
- AGP 9.1 release notes: <https://developer.android.com/build/releases/agp-9-1-0-release-notes>
- AGP 9.2 release notes: <https://developer.android.com/build/releases/agp-9-2-0-release-notes>
- AGP 9.3 release notes: <https://developer.android.com/build/releases/agp-9-3-0-release-notes>
- Kotlin 2.3.0 release notes: <https://kotlinlang.org/docs/whatsnew23.html>
- Kotlin 2.3.20 release notes: <https://kotlinlang.org/docs/whatsnew2320.html>
- Kotlin 2.4.0 release notes: <https://kotlinlang.org/docs/whatsnew24.html>
- Compose BOM mapping: <https://developer.android.com/develop/ui/compose/bom/bom-mapping>
- Compose BOM guidance: <https://developer.android.com/develop/ui/compose/bom>
- Gradle current release notes: <https://docs.gradle.org/current/release-notes.html>

### Current authoritative files

When auditing this project, treat these as stronger than older prose docs:

- Build and dependency truth: `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`
- Runtime entry points: `RuncheckApp`, `MainActivity`, `RuncheckNavHost`, `MonitorScheduler`
- Persistence truth: `RuncheckDatabase`, `DatabaseModule`, Room entities/DAOs, app schema exports
- Product gates: `ProState`, `ProManager`, `BillingManager`, `IsProUserUseCase`
- Visual system: `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `Shapes.kt`, `Spacing.kt`, `MotionTokens.kt`, `UiTokens.kt`, `StatusColors.kt`
- Shared UI/runtime helpers: `LifecycleStartStopEffect`, `HistoryLoadErrorMessage`, `HistoryPeriodFilterChipRow`, `ChartStatsRow`, `RuncheckPermissionPolicy`
- Security surface: `AndroidManifest.xml`, `network_security_config.xml`, `data_extraction_rules.xml`, `backup_rules.xml`, `file_export_paths.xml`, `ReleaseSafeLog`, Semgrep config, Sentry source sets

---

## Architecture and Data Flow

runcheck is a single-module Android app using a layered Clean Architecture style. The UI observes domain-facing use cases and repository interfaces; data implementations own Android framework APIs and persistence.

```text
Android framework / Play Billing / MediaStore / M-Lab NDT7
        ↓
data/ sources + repository implementations
        ↓
domain/ models, repository contracts, use cases, scoring, insights
        ↓
ui/ Hilt ViewModels
        ↓
Compose screens and reusable components
```

Layer expectations:

- `domain/` owns business models, repository interfaces, use cases, health scoring, and insight rules.
- `data/` owns Android APIs, Room, MediaStore, BatteryManager, ConnectivityManager, PowerManager, StorageStatsManager, DevicePolicyManager, PackageManager launcher visibility queries, Play Billing, and NDT7 integration.
- `ui/` owns Compose screens, components, ViewModels, navigation, saved UI state, and visual formatting.
- ViewModels bridge UI and domain only; UI should not call data implementations directly.
- `androidx.paging.PagingData` is an allowed documented boundary exception in domain cleanup/app-usage flows.

Dependency injection:

- `RepositoryModule` binds data implementations to domain repository contracts.
- `DatabaseModule` builds Room with migrations 1 through 10 and provides DAOs.
- `SystemBindingsModule` binds Pro, billing, device profile, monitoring scheduler, screen-state tracking, foreground-app provider, and transaction-runner abstractions.
- `InsightsModule` multibinds each `InsightRule` into `Set<InsightRule>`.
- `DataModule` currently provides shared `Gson`.
- Debug and release `InsightDebugModule` source sets keep debug insight tooling out of release builds.

Important runtime data flows:

- Home combines live battery, network, thermal, storage, insight, Pro, preference, and monitoring-freshness flows, throttled to 333ms display updates.
- Battery/network/thermal/storage detail ViewModels sample live updates at 333ms to avoid high-frequency UI churn.
- Periodic monitoring persists snapshots to Room, updates charger sessions, evaluates alerts, records heartbeat, and retries only when core collection/maintenance fails.
- Insight generation is rule-driven and persisted; Home shows ranked persisted rows, not ad hoc generated UI-only messages.
- Fullscreen chart route passes selected source/metric/period back to the parent back stack entry through `SavedStateHandle`.
- Screen observation start/stop is centralized through `LifecycleStartStopEffect` on Home and detail/tool screens so active collectors are started at `ON_START` and stopped at `ON_STOP`.

---

## Navigation

Push-based navigation from a single Home screen. No bottom nav, no tabs.

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

Defined routes in code:

- `home`
- `insights`
- `battery`
- `charger`
- `network`
- `speed_test`
- `thermal`
- `storage`
- `cleanup/{type}`
- `app_usage`
- `learn`
- `learn/{articleId}`
- `fullscreen_chart/{source}/{metric}/{period}`
- `settings`
- `pro_upgrade`

State restoration details:

- `rememberSaveable` is used for screen-local UI state such as sheet visibility and metric chip selections.
- `SavedStateHandle` is used for route-backed or deep state that must survive recreation, including battery/network history period, cleanup filter selection, and fullscreen chart metric/period.
- Free-tier entry into `charger` and `app_usage` routes redirects to `pro_upgrade`.
- Direct notification/deep-link routes are limited to argument-free destinations in `Screen.directRoutes`.
- Learn article cross-links are validated against direct routes at catalog initialization time.
- Fullscreen chart args are route-backed, but selection changes are returned to Battery/Network through `FullscreenChartResult` keys on the previous back stack entry.

---

## Runtime Systems

### App Startup

`RuncheckApp` initializes and coordinates:

- Billing and Pro state
- Notification channels
- Screen-state tracking repository
- Periodic monitoring scheduling
- Widget refreshes when Pro state changes
- Source-set-specific `SentryInit` initialization; debug builds may report to Sentry through `sentry-android-core` only when `RUNCHECK_SENTRY_DSN`, `SENTRY_DSN`, or ignored `debug.credentials.properties` provides `sentry.dsn`; release builds are a no-op and do not include Sentry on the release classpath
- Debug-only StrictMode policies for thread and VM issue logging
- Hilt WorkManager factory through `Configuration.Provider`

### Background Monitoring

Three periodic WorkManager jobs are scheduled through `MonitorScheduler`:

- `HealthMonitorWorker`
  - unique work name: `health_monitor`
  - interval: current `MonitoringInterval` preference (`15`, `30`, or `60` minutes; default `30`)
  - collects battery, network, thermal, and storage snapshots
  - persists readings into Room
  - updates charger sessions through `ChargerSessionTracker`
  - evaluates alert conditions
  - persists last successful worker heartbeat
  - posts notifications for low battery, high temperature, low storage, and charge complete
- `HealthMaintenanceWorker`
  - unique work name: `health_maintenance`
  - interval: current `MonitoringInterval` preference
  - constrained with `requiresBatteryNotLow`
  - collects per-app usage snapshots
  - cleans up old readings
  - refreshes widgets best-effort; widget refresh failure does not force a retry
- `InsightGenerationWorker`
  - unique work name: `insight_generation`
  - evaluates persisted Room history through the Insights Engine
  - refreshes the Home insights surface every 6 hours
  - stays battery-conscious with `requiresBatteryNotLow` constraints
  - retries only on `SQLException`; cancellation is rethrown

Additional WorkManager-backed trial behavior:

- `TrialNotificationWorker`
  - scheduled by `TrialManager` as unique one-time day-5 and day-7 trial notification work
  - initializes billing before notifying and skips notifications when Pro is already active
  - canceled by `ProManager` once a permanent purchase is active

Supporting monitor components include:

- `BootReceiver`
- `ScreenStateTracker`
- `MonitoringAlertStateStore`
- `NotificationHelper`

### Live Notification

`RealTimeMonitorService` provides an opt-in persistent notification with real-time battery stats:

- **Opt-in** — disabled by default, toggled in Settings → Live Notification
- Runs as a foreground service (`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`)
- Updates every 5 seconds with live battery data from `BatteryRepository`
- Uses `BigTextStyle` — collapsed shows level/status/temp, expanded shows additional lines
- Configurable per-metric toggles: Current (mA/W), Charging status, Temperature, Screen stats, Remaining time
- Stops immediately when user disables the toggle
- Tapping the notification opens the app

### Widgets

Two Glance widgets are present:

- Battery widget
- Health score widget

Widget data is read from the latest Room snapshots. Widget access is treated as a Pro feature.

---

## Measurement Reliability and Health Score

### Measured values and confidence

- Sensor values that can be unreliable use `MeasuredValue<T>` with `Confidence`.
- Internal confidence enum values are `HIGH`, `LOW`, and `UNAVAILABLE`.
- The UI maps those internal values to user-facing badge labels: `HIGH` → Accurate, `LOW` → Estimated, `UNAVAILABLE` → Unavailable.
- `ConfidenceBadge` is the shared UI component and uses theme status tokens for backgrounds/text.

Battery current reliability:

- `DeviceCapabilityManager.validateCurrentNow()` reads `BATTERY_PROPERTY_CURRENT_NOW` three times with 300ms spacing.
- A current source is considered reliable only when readings are non-zero, changing, and plausible.
- Runtime current normalization distinguishes microamps from milliamps with `MICROAMP_THRESHOLD = 25_000`.
- Plausible normalized current must be in `0..10000` mA during capability validation.
- Current sign is aligned with charging state so charging values are positive and discharging values are negative.
- Remaining battery capacity uses `BATTERY_PROPERTY_CHARGE_COUNTER` only when Android returns a positive value.
- Estimated full battery capacity is calculated by `estimateFullCapacityMah(remainingMah, levelPercent)` and is emitted only when the battery level is 1..100 and the estimate is in the plausible 500..20,000 mAh range.
- Battery design capacity is not queried or displayed in production; `designCapacityMah` remains `null` because this codebase does not use private `PowerProfile` or other private design-capacity APIs.
- Device profile stores manufacturer, model, API level, current unit, sign convention, cycle-count availability, thermal-zone list, and storage-health availability.
- Vendor-specific battery sources exist for Samsung and OnePlus, with API 34+ variants using Android 14+ capabilities when available.

Thermal reliability:

- Battery temperature comes from `ACTION_BATTERY_CHANGED`.
- Thermal status uses `PowerManager.currentThermalStatus` and `OnThermalStatusChangedListener` on API 29+.
- Thermal headroom uses `PowerManager.getThermalHeadroom(10)` on API 30+ and polls every 3 seconds.
- CPU temperature currently emits `null`; no sysfs thermal reads are used.

Network reliability:

- Current connection details are read through Android network APIs.
- Latency uses five TCP-connect samples against `BuildConfig.LATENCY_HOST` / `BuildConfig.LATENCY_PORT`, with a 1.5s per-sample timeout and 6s total timeout.
- Jitter is computed with an RFC 3550-style moving jitter formula when at least four samples are available.
- Network detail and speed test may display signal, latency, Wi-Fi standard, cellular subtype, DNS/IP/MTU, and VPN state when Android exposes them.

Storage reliability:

- Aggregate app/data/cache bytes use `StorageStatsManager.queryStatsForUser(...)` and are unavailable when the service, access, or platform call is unavailable.
- App count is a distinct count of launchable packages visible through `PackageManager.queryIntentActivities(Intent.ACTION_MAIN + Intent.CATEGORY_LAUNCHER)`; it is not a full installed-app inventory.
- Storage encryption status uses `DevicePolicyManager.storageEncryptionStatus` and maps public platform states to FBE, Encrypted, Inactive, Unsupported, or unavailable.
- No `SystemProperties` reflection or `PackageManager.getInstalledApplications(...)` package inventory is used for these storage values.

### Health score calculation

`HealthScoreCalculator` combines four subsystem scores:

| Subsystem | Weight |
|-----------|--------|
| Battery | 40% |
| Network | 25% |
| Thermal | 25% |
| Storage | 10% |

Status thresholds:

| Score | Status |
|-------|--------|
| 75-100 | Healthy |
| 50-74 | Fair |
| 25-49 | Poor |
| 0-24 | Critical |

Scoring details:

- Battery score penalizes health state, battery temperature, voltage, and optional health percentage.
- Network score is `0` when disconnected.
- Without a recent speed test, network score is based on signal quality and latency.
- With a speed test less than 1 hour old, network score weighs signal 40%, latency/ping 30%, download speed 20%, and jitter/stability 10%.
- Thermal score penalizes battery temperature, missing/known CPU temperature, and Android thermal status.
- Storage score penalizes usage percent, with sharp penalties at high utilization.

---

## Home Screen

Home is the single entry point and aggregates the latest device state from battery, network, thermal, and storage.

Main sections:

- Health score hero with animated `ProgressRing` (1200ms arc fill)
- Battery hero card
- 2×2 quick status grid
  - Network
  - Thermal
  - Charger comparison
  - Storage
- Rule-driven Insights summary backed by persisted Room insight rows, with Home showing a curated subset of up to three items and a dedicated full Insights screen
- Home Insights are selected by `InsightHomeRankingPolicy`, which avoids showing multiple items from the same target bucket before filling remaining slots
- Quick tools card
  - Speed Test
  - App Usage
  - Learn
- Trial / expired-trial / Pro state cards

Trial and Pro UI handled on Home:

- Welcome sheet for trial onboarding
- Day-5 trial snackbar/banner
- Trial-expiration modal
- Post-expiration upgrade card
- Purchased Pro status card
- Top-level Insights summary available to all users, with the full list available from the dedicated Insights screen
- Insight targets for Pro-only destinations such as Charger Comparison and App Usage are hidden for free users and visible for trial/Pro users
- Monitoring stale state is derived from the last worker heartbeat and becomes stale after more than 3x the configured monitoring interval.
- Home marks currently displayed unseen insight rows as seen through `InsightRepository.markAllSeen()`.

---

## Battery Detail

Battery detail is the richest diagnostics screen and mixes live state, recent history, current-session insights, and Pro-only long-range history.

Key sections:

- Hero ring with battery level
- Health + charging summary
- Optional mAh remaining estimate
- Current / charging details with `ConfidenceBadge`
- Session-level current statistics
- Screen-on / screen-off drain analysis
- Sleep analysis while discharging
- Charger comparison CTA
- Charging session graph
- Pro-only remaining charge estimates
- Pro-only history chart
- Pro-only battery statistics panel

Battery-specific supporting behavior:

- Current readings use `MeasuredValue<Int>`
- Current confidence is internally `HIGH`, `LOW`, or `UNAVAILABLE`; badge copy presents those as Accurate, Estimated, or Unavailable.
- Remaining mAh comes from the public BatteryManager charge-counter value when the platform provides one.
- Estimated full capacity is shown as an estimate only when it can be derived from remaining mAh and current battery level inside the repository's plausible range.
- Design capacity is intentionally absent from the UI because no stable public design-capacity source is used.
- Current stats are tracked in-memory and reset on status change
- Session and history charts can open a fullscreen landscape chart route
- History charts use "Instrument Sweep" animation (3-phase: grid fade → oscilloscope sweep → emphasis)
- Live current/power charts use smooth scroll interpolation + glow pulse on new data
- Battery screen also consumes dismissed educational/info cards
- Charger session tracking runs both from Home live observation and from `HealthMonitorWorker` so charge sessions can be updated in foreground and background.

---

## Network Detail

Network detail focuses on current connection state plus historical signal/latency data.

Main sections:

- Signal hero with `SignalBars`
- Wi-Fi name permission help card when SSID is unavailable
- Latency, link speed, frequency metrics
- Connection details card
- IP / DNS / MTU section
- Pro-only signal history chart
- Speed test summary card

Latency measurement:

- `GetMeasuredNetworkStateUseCase` combines the network state flow with periodic TCP latency measurements (30-second interval via `LatencyMeasurer`)
- Latency resets to null when connection is lost and re-measures immediately when connection type changes
- Approved outbound latency surface is TCP connect only to `BuildConfig.LATENCY_HOST` / `BuildConfig.LATENCY_PORT` (`locate.measurementlab.net:443` by default).

Historical chart behavior:

- Metrics: signal strength or latency
- Period selection is stored in ViewModel saved state
- Signal chart uses status gradient line (quality zone colors on the data line)
- Fullscreen chart route is available from the chart section

---

## Speed Test

Speed tests use M-Lab NDT7 only.

Flow:

1. Ready state with current connection context
2. Cellular warning dialog before test start when the active network is cellular
3. Ping phase
4. Download phase
5. Upload phase
6. Completed or failed state
7. Result history list

Stored result fields include:

- Download Mbps
- Upload Mbps
- Ping
- Jitter
- Server name/location
- Connection type and subtype
- Optional signal strength

Connection type (WiFi/Cellular) and network subtype are shown in both the latest result card and the history list with icons and labels (e.g., "WiFi · WiFi 6", "Cellular · 5G"). Signal strength (dBm) is displayed in the latest result.

Free tier stores a limited history. Pro keeps a larger history.

Implementation constraints:

- `SpeedTestService` uses `net.measurementlab.ndt7.android.NdtTest`.
- The service lets NDT7 auto-select the server; it does not hardcode a fixed test server.
- A validated internet connection is required before starting.
- Cellular starts are blocked with `CellularConfirmationRequired` until the user confirms.
- The active default network is locked at test start; network loss or connection identity changes fail the test instead of mixing measurements.
- Download and upload phases each use roughly 10 seconds of NDT7 progress.
- Server metadata is extracted from NDT7 `ClientResponse.origin` / `ClientResponse.test` when present.
- `FinalizeSpeedTestUseCase` trims stored history to the free-tier limit unless Pro is active.

---

## Thermal Detail

Thermal detail surfaces both live thermal state and throttling history.

Main sections:

- Animated thermometer hero
- Heat strip
- Metrics grid
- Pro-only throttling log
- Educational/info cards

Important implementation constraints:

- Thermal state comes from Android PowerManager APIs
- No sysfs-based thermal reads
- Session min/max values are tracked while the screen is active

---

## Storage Detail

Storage detail combines usage diagnostics, media breakdown, cleanup entry points, and storage health guidance.

Main sections:

- Usage hero ring
- Media permission card when needed
- Media breakdown segmented bar
- Cleanup tools section
- Storage history chart with quality zones (0–70% healthy, 70–90% fair, 90–100% critical)
- Storage detail metrics
- Optional SD card section
- Educational/info cards

Permission behavior:

- Storage asks through `RuncheckPermissionPolicy.mediaPermissionsForApi()`.
- Android 14+ distinguishes full media access from selected visual media access through `MediaAccessState`.
- Media breakdown and cleanup affordances are shown only when the relevant media access exists.

Storage-specific data behavior:

- Aggregate app/data/cache bytes use `StorageStatsManager.queryStatsForUser(...)` and may be null without usage access or when Android denies the call.
- App count means distinct launchable packages visible to this app through `ACTION_MAIN` + `CATEGORY_LAUNCHER`, not all installed packages on the device.
- Encryption status comes from `DevicePolicyManager.storageEncryptionStatus`.
- File-system type is read from `/proc/mounts` for `/data`; storage volume count uses `StorageManager.storageVolumes`.

Cleanup tool entry points:

- Large Files
- Old Downloads
- APK Files
- Trash is not a `cleanup/{type}` route; Storage shows trash info and empties trash through a separate API 30+ MediaStore delete request path when trashed media exists.

Free tier behavior:

- Storage screen itself is available
- Cleanup tools are gated behind a Pro callout

---

## Cleanup Screen

Cleanup is a shared route driven by `cleanup/{type}` and `CleanupType`.

Supported cleanup types:

- `LARGE_FILES`
- `OLD_DOWNLOADS`
- `APK_FILES`

Filter behavior:

- Large files: 10 / 50 / 100 / 500 MB
- Old downloads: 30 / 60 / 90 days / 1 year
- APK files: no chip filters

UI and data behavior:

- Scanning, empty, results, deleting, and success states
- Grouped file results by media category
- Expand/collapse per category
- Selection by file or whole group
- Paging-backed item loading per group
- Paging page size: 40
- API 30+ delete flow uses system delete request / intent sender
- Android 10 and below legacy delete path uses `StorageCleanupHelper.deleteLegacy`
- Old Downloads and APK cleanup are version-restricted to API 30+ in `CleanupViewModel`
- APK cleanup preselects all groups by default
- Selected filter is stored through `SavedStateHandle`
- Route is Pro-gated in the ViewModel; non-Pro users receive a locked error state before scanning
- Storage, thermal, network, and battery trend sections share `HistoryPeriodFilterChipRow`, `HistoryLoadErrorMessage`, and `ChartStatsRow` for period chips, history-load failures, and min/avg/max chart stats.

---

## App Usage

App Usage is Pro-gated and backed by paging.

Behavior:

- Locked route redirects to Pro Upgrade for free tier
- Uses usage-access permission instead of normal runtime permission
- Shows permission education card when access is missing
- Refreshes usage snapshot when the user returns from system settings
- Displays total foreground time summary and per-app list items

Background support:

- Usage snapshots are collected periodically in maintenance work only while trial/Pro access is active
- Data is persisted and then paged back into the UI

---

## Learn

Learn is a lightweight educational content flow built entirely in-app.

Screens:

- Learn topic list screen
- Learn article detail screen

Behavior:

- Articles are grouped by topic
- Current catalog size: 15 articles
- Current topics: Battery, Temperature, Network, Storage, General
- Article detail renders structured body text from catalog resources
- Some articles expose cross-link buttons into app routes
- Cross-link routes are validated at startup, and legacy article IDs can alias to canonical IDs.
- Read/unread state is not persisted.

---

## Fullscreen Chart

Fullscreen charts are launched from battery and network trend sections.

Behavior:

- Landscape-only while the route is visible
- Supports battery history, battery session, and network history sources through `FullscreenChartSource`
- Metric and period chip selections are stored in `SavedStateHandle`
- Uses the same chart data model, tooltip formatting, and "Instrument Sweep" animation as embedded charts
- The route itself is `fullscreen_chart/{source}/{metric}/{period}`.
- Pro lock is applied for `BATTERY_HISTORY` and `NETWORK_HISTORY`; `BATTERY_SESSION` remains available as the current-session fullscreen source.
- Selection changes are sent back to the previous route with `FullscreenChartResult.KEY_SOURCE`, `KEY_METRIC`, and `KEY_PERIOD`.

---

## Settings

Settings is broader than a simple preferences page and covers monitoring, privacy, export, purchase flow, and device capability info.

Sections:

- Monitoring interval
- Live Notification
  - master toggle (opt-in, disabled by default)
  - per-metric toggles: current, drain rate, temperature, screen stats, remaining time
  - starts/stops `RealTimeMonitorService` foreground service
- Notifications
  - master notifications toggle
  - low battery
  - high temperature
  - low storage
  - charge complete
- Alert thresholds
  - low battery threshold
  - temperature threshold
  - low storage threshold
- Display
  - Celsius / Fahrenheit
  - Show/hide info cards toggle
- Data
  - data retention
  - CSV export
  - reset info tips
  - clear speed test results
  - clear all data
  - all destructive actions require confirmation dialog
- Pro
  - current Pro status
  - upgrade CTA
  - restore purchase
- Device
  - device model
  - API level
  - current-now reliability
  - cycle-count availability
  - thermal zone count
- Debug Insights
  - visible only when the injected `InsightDebugActions` implementation reports availability
  - debug builds can seed deterministic demo insight data, regenerate insights from local data, and clear insight rows
  - release builds bind `ReleaseSafeInsightDebugActions`, return unavailable/no-op behavior, and ship empty non-translatable release strings for this section
- About
  - version
  - Play Store link
  - privacy policy
  - feedback email intent

Notes:

- Notification permission handling is built into Settings for Android 13+ through `RuncheckPermissionPolicy`
- Export shares one or more CSV files through `FileProvider`
- All destructive actions (clear speed tests, clear all data, reset tips, reset thresholds) show a confirmation AlertDialog with primary blue confirm button

---

## Persistence

Primary persisted data:

- Battery readings
- Network readings
- Thermal readings
- Storage readings
- Throttling events
- Speed test results
- Charger profiles / sessions
- App usage snapshots
- Device profile info
- Insight rows with rule id, dedupe key, priority, confidence, seen/dismissed state, and expiry window
- User preferences and dismissed info cards
- Trial state and upgrade-card dismissal pacing

Persistence technologies:

- Room database name: `runcheck.db`
- Room schema version: 10
- Room for telemetry/history, charger, app-usage, speed-test, and insight entities
- Room schema export is enabled and androidTest assets include `app/schemas`; exported schema assets currently cover versions 6-10
- Room migrations are explicitly registered from 1→2 through 9→10
- A destructive migration callback logs debug-only and records `destructive_migration_occurred` in `runcheck_db_events`
- DataStore `settings` for user preferences, dismissed info cards, selected charger, and app-usage collection timestamp
- DataStore `trial_state` for trial start, last-known timestamp, welcome/day-5 prompt state, and upgrade-card dismissal pacing
- DataStore `monitoring_status` for the last successful periodic worker heartbeat
- DataStore `monitoring_alert_state` for the previous alert snapshot and charge-complete debounce state
- SharedPreferences `pro_status_cache` for synchronous cached purchase status during release cold start

Room tables/entities:

| Table | Entity | Purpose |
|-------|--------|---------|
| `battery_readings` | `BatteryReadingEntity` | Battery history |
| `network_readings` | `NetworkReadingEntity` | Network signal/latency history |
| `thermal_readings` | `ThermalReadingEntity` | Thermal history |
| `storage_readings` | `StorageReadingEntity` | Storage usage history |
| `throttling_events` | `ThrottlingEventEntity` | Thermal throttling log |
| `charger_profiles` | `ChargerProfileEntity` | User charger labels |
| `charging_sessions` | `ChargingSessionEntity` | Charging session measurements |
| `app_battery_usage` | `AppBatteryUsageEntity` | Per-app foreground/estimated usage snapshots |
| `speed_test_results` | `SpeedTestResultEntity` | NDT7 speed test history |
| `devices` | `DeviceEntity` | Current device profile JSON |
| `insights` | `InsightEntity` | Persisted rule-driven insight rows |

Migration history:

| Migration | Main change |
|-----------|-------------|
| 1→2 | Adds `throttling_events` |
| 2→3 | Adds charger profiles and charging sessions |
| 3→4 | Adds app battery usage |
| 4→5 | Recreates network readings with nullable `signal_dbm` |
| 5→6 | Adds speed test results |
| 6→7 | Adds battery status/timestamp and charging session end-time indexes |
| 7→8 | Adds app-usage package/timestamp composite index |
| 8→9 | Drops redundant package-name-only app-usage index |
| 9→10 | Adds `insights` table and indexes |

Default preference values:

| Setting | Default |
|---------|---------|
| Monitoring interval | 30 minutes |
| Notifications master toggle | Enabled |
| Data retention | 3 months |
| Low battery alert | Enabled, threshold 20% |
| High temperature alert | Enabled, threshold 42°C |
| Low storage alert | Enabled, threshold 90% |
| Charge-complete alert | Disabled |
| Temperature unit | Celsius |
| Live notification | Disabled |
| Live current/drain/temp lines | Enabled |
| Live screen stats/remaining time lines | Disabled |
| Info cards | Enabled |

---

## Monetization Model

Pro state is handled through `BillingManager`, `ProManager`, and `ProState`.

Current product behavior:

- Trial duration is 7 days and trial state is treated as Pro access
- Expired trial loses gated features
- Purchased Pro unlocks all Pro features permanently
- No subscriptions
- Debug builds force `BillingManager` Pro state to active for development
- Release builds use Google Play Billing one-time `INAPP` product id `runcheck_pro`
- Debug builds can override the product id with `RUNCHECK_PRO_PRODUCT_ID`; release builds keep the checked-in product id so release artifacts are reproducible.
- Pending purchases are tracked separately and do not unlock Pro until purchased
- Purchased but unacknowledged purchases are acknowledged with up to 3 retries
- Cached Pro state is restored synchronously in release builds to avoid a free-tier flash while Billing queries run

Pro-gated areas currently include:

- Charger Comparison
- App Usage
- Extended history
- Thermal logs
- CSV export
- Widgets
- Remaining charge time
- Storage cleanup route

`ProFeature` enum values:

- `EXTENDED_HISTORY`
- `CHARGER_COMPARISON`
- `PER_APP_BATTERY`
- `WIDGETS`
- `CSV_EXPORT`
- `THERMAL_LOGS`
- `REMAINING_CHARGE_TIME`
- `STORAGE_CLEANUP`

All current features share the single `ProState.isPro` decision. The per-feature enum remains in place so a future per-feature gate can be added without changing all UI call sites.

---

## Security and Privacy Surface

Manifest and network posture:

- `android:allowBackup="false"`
- `android:dataExtractionRules="@xml/data_extraction_rules"` and `android:fullBackupContent="@xml/backup_rules"` exclude root, files, databases, shared preferences, and external app data from cloud backup and device transfer rules.
- `android:usesCleartextTraffic="false"`
- `network_security_config.xml` permits system trust anchors only and disallows cleartext traffic.
- Manifest package visibility is limited to an `ACTION_MAIN` + `CATEGORY_LAUNCHER` `<queries>` intent for launcher-app visibility; the app does not request `QUERY_ALL_PACKAGES`.
- `androidx.startup.InitializationProvider` remains merged for non-WorkManager App Startup components; `androidx.work.WorkManagerInitializer` is removed so WorkManager uses `RuncheckApp`'s explicit `Configuration.Provider`.
- Main launcher activity is exported by design.
- App widget receivers are not exported and are protected with `android.permission.BIND_APPWIDGET`.
- `RealTimeMonitorService` is not exported and uses `foregroundServiceType="specialUse"` with a declared special-use subtype.
- FileProvider is not exported and exposes only cache path `exports/` as `csv_exports`.

Declared permission surface:

| Permission | Purpose |
|------------|---------|
| `ACCESS_NETWORK_STATE` | Network type/capability detection |
| `ACCESS_WIFI_STATE` | Wi-Fi connection details |
| `INTERNET` | User-initiated NDT7 speed tests and TCP latency measurement |
| `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` | Optional Wi-Fi SSID/BSSID visibility; requested together so Android can offer precise access |
| `POST_NOTIFICATIONS` | Android 13+ notifications |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | Opt-in live monitoring notification |
| `RECEIVE_BOOT_COMPLETED` | Reschedule monitoring after boot/package replacement/unlock |
| `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED` | Android 13+ media breakdown/cleanup, including Android 14+ selected visual media state |
| `READ_EXTERNAL_STORAGE` maxSdk 32 | Android 12 and below media fallback |
| `WRITE_EXTERNAL_STORAGE` maxSdk 28 | Legacy delete fallback |
| `PACKAGE_USAGE_STATS` | App Usage, per-app battery feature, and aggregate app/cache storage stats through `StorageStatsManager` |
| `READ_BASIC_PHONE_STATE` | Android 13+ cellular network generation fallback |

Runtime permission decisions are centralized in `RuncheckPermissionPolicy` for Wi-Fi detail location permissions, Android-version-specific media permission lists, Android 14+ partial visual media access, and Android 13+ notification permission checks.

Approved outbound network surfaces:

- User-initiated M-Lab NDT7 speed tests
- TCP latency measurement to the configured latency host/port
- Google Play Billing

Telemetry/logging boundary:

- Release Sentry init is a no-op.
- Debug Sentry disables tracing, profiling, auto session tracking, breadcrumbs, activity lifecycle tracing, frames tracking, screenshots, view hierarchy, NDK, performance v2, and auto trace id generation.
- `ReleaseSafeLog` emits Android logs only in debug builds.
- Semgrep has project-specific rules for backup, cleartext, exported components, broad FileProvider paths, sensitive Android logging, outbound network primitives, and release telemetry expansion.

---

## Future Considerations

- **Learn article read/unread tracking:** Currently no persistence of which Learn articles the user has read. With only 15 articles this isn't needed yet, but if the catalog grows significantly (30+), consider adding DataStore-backed read state with visual indicators (e.g., unread dot on `LearnArticleCard`).

---

## Brand & Design System

Single dark theme — no light mode, no AMOLED toggle, no dynamic colors.

### Color Palette

**Backgrounds:**

| Token | Hex | Material3 Role | Usage |
|-------|-----|----------------|-------|
| BgPage | `#0B1E24` | `background`, `surface` | Page background |
| BgCard | `#133040` | `surfaceContainer` | Card backgrounds |
| BgCardDeep | `#0D2530` | — | Deeper card surfaces where explicitly used |
| BgCardAlt | `#0F2A35` | `surfaceContainerHigh` | Info cards, elevated surfaces |
| BgIconCircle | `#1A3A48` | `surfaceContainerHighest`, `surfaceVariant` | Icon circle backgrounds |

**Accents:**

| Token | Hex | Material3 Role | Usage |
|-------|-----|----------------|-------|
| AccentBlue | `#4A9EDE` | `primary` | Primary accent, buttons, links, brand |
| AccentTeal | `#5DE4C7` | `secondary` | Healthy status, positive values |
| AccentAmber | `#E8C44A` | `tertiary` | Fair status, warnings |
| AccentOrange | `#F5963A` | — | Poor status |
| AccentRed | `#F06040` | `error` | Critical status, destructive actions |
| AccentLime | `#C8E636` | — | Storage video category |
| AccentYellow | `#F5D03A` | — | Storage audio category |

**Text:**

| Token | Hex | Material3 Role | Usage |
|-------|-----|----------------|-------|
| TextPrimary | `#E8E8ED` | `onSurface`, `onBackground` | Main text |
| TextSecondary | `#90A8B0` | `onSurfaceVariant` | Labels, descriptions |
| TextMuted | `#7A949E` | `outline`, `outlineVariant` | Hints, dividers, disabled text |
| TextOnLime | `#1A2E0A` | — | Text on lime-colored backgrounds |

### Status Colors

Used via `MaterialTheme.statusColors` extension. Always paired with icons or text labels for accessibility.

| Status | Color | Thresholds |
|--------|-------|------------|
| Healthy | AccentTeal `#5DE4C7` | Battery ≥75%, Temp <35°C, Storage <75%, Signal Excellent/Good |
| Fair | AccentAmber `#E8C44A` | Battery 50–74%, Temp 35–39°C, Storage 75–84%, Signal Fair |
| Poor | AccentOrange `#F5963A` | Battery 25–49%, Temp 40–44°C, Storage 85–94%, Signal Poor |
| Critical | AccentRed `#F06040` | Battery <25%, Temp ≥45°C, Storage ≥95%, No Signal |

**Confidence badges:**

| Badge | Background | Text |
|-------|-----------|------|
| Accurate | AccentBlue `#4A9EDE` | BgPage `#0B1E24` |
| Estimated | AccentAmber `#E8C44A` | BgPage `#0B1E24` |
| Unavailable | TextMuted `#7A949E` | TextPrimary `#E8E8ED` |

### Typography

**Font families:**
- **Manrope** — all body text, headers, labels (`MaterialTheme.typography`)
- **JetBrains Mono** — numeric displays, values, charts (`MaterialTheme.numericFontFamily`)

**Type scale (Manrope):**

| Style | Size | Weight |
|-------|------|--------|
| displayLarge | 48sp | Bold, -0.04em tracking |
| displayMedium | 36sp | Bold |
| displaySmall | 28sp | SemiBold |
| headlineLarge | 20sp | SemiBold |
| headlineMedium | 16sp | SemiBold |
| headlineSmall | 14sp | SemiBold |
| titleLarge | 20sp | Medium |
| titleMedium | 16sp | Medium |
| titleSmall | 14sp | Medium |
| bodyLarge | 15sp | Normal |
| bodyMedium | 14sp | Normal |
| bodySmall | 13sp | Normal |
| labelLarge | 12sp | SemiBold, 0.08em tracking |
| labelMedium | 10sp | SemiBold |
| labelSmall | 10sp | Medium |

**Numeric text styles (JetBrains Mono):**

| Style | Base | Size | Usage |
|-------|------|------|-------|
| numericHeroDisplayTextStyle | displayLarge | 64sp Bold, -3sp tracking | Primary large hero displays |
| numericHeroDisplayUnitTextStyle | headlineLarge | 28sp SemiBold | Units next to primary large hero displays |
| numericHeroValueTextStyle | displayLarge | 48sp Bold | Large hero values |
| numericHeroLargeValueTextStyle | displayLarge | 54sp | Battery level display |
| numericHeroLevelTextStyle | displayLarge | 48sp Bold, -2sp tracking | Compact hero values |
| numericHeroUnitTextStyle | headlineLarge | 20sp SemiBold | Units next to hero values |
| numericRingValueTextStyle | displayMedium | 32sp Bold | ProgressRing center value |
| numericSpeedHeroValueTextStyle | displaySmall | 40sp | Speed test hero display |
| numericMetricDisplayTextStyle | displayLarge | 48sp Bold, -3sp tracking | Secondary hero numbers (dBm, latency) |
| chartAxisTextStyle | labelSmall | 12sp | Chart axis labels |
| chartTooltipTextStyle | bodySmall | 13sp | Chart tooltip values |

### Shapes & Spacing

**Corner radii:**

| Shape | Radius | Usage |
|-------|--------|-------|
| large | 16dp | Cards, panels, dialogs |
| medium | 8dp | Badges, chips, small elements |
| small | 8dp | Compact elements |
| extraLarge | 50% | Circles (icons, avatars) |

**Spacing grid (4dp base):**

| Token | Value | Usage |
|-------|-------|-------|
| xxs | 2dp | Micro gaps, baseline alignment |
| xs | 4dp | Tight gaps |
| sm | 8dp | Small gaps, inter-row spacing |
| md | 12dp | Between cards, standard gaps |
| base | 16dp | Card padding, section spacing |
| lg | 24dp | Between sections |
| xl | 32dp | Page margins, large separations |

**Dividers:** `outlineVariant.copy(alpha = 0.35f)` — no hardcoded colors.

Shared UI dimensions such as touch targets, icon sizes, button heights, outline width, and Pro lock/badge alpha values live in `UiTokens`.

**Contrast:** Minimum 4.5:1 body text, 3:1 large text (WCAG AA). Minimum touch target 48dp.

### Logo

Health-score arc (~210°) wrapping a phone silhouette, rendered in AccentBlue.
Icon source files in `icons/` directory (SVG masters + 512px PNG exports).

---

## Testing and Verification

Current test surface:

- Unit tests: 83 Kotlin files under `app/src/test/java/com/runcheck/`
- Instrumented tests: 1 Kotlin file under `app/src/androidTest/java/com/runcheck/`
- Android test assets include exported Room schemas for migration tests; current exported assets cover versions 6-10.
- Shared coroutine main dispatcher rule lives in `ui/MainDispatcherRule.kt`.

High-value unit coverage areas:

- Battery source normalization and device capability detection
- Billing helper behavior and Pro state transitions
- Network VPN detection and network ViewModel behavior
- Thermal helper mapping
- Health score calculation
- Domain use cases for cleanup, alerts, export, battery stats, charger comparison, throttling, and retention
- Insight home ranking policy and every current Insight rule
- Home, Settings, Storage cleanup, App Usage, Charger, Battery, Network, Insights, Fullscreen Chart, and Learn ViewModel/helper behavior
- Chart render/accessibility helpers
- Debug/release-safe insight debug action boundaries
- Worker behavior for monitor scheduler, health monitor, and insight generation

Low-CPU verification policy:

- Do not run full Gradle builds, `lc`, `sc`, Sonar, Dependency-Check, MobSF, DeepSec, or broad verification by default.
- Prefer source-backed review, targeted tests, targeted Gradle tasks, wrapper `-PlanOnly`, and static grep first.
- User-facing aliases `lc` and `sc` are intentionally run by the user when they want full lint/security reports.
- When report artifacts exist, read them first instead of rerunning heavy wrappers.
- `reports/` is ignored and must not be committed.

Useful narrow commands:

```powershell
.\gradlew --no-daemon testDebugUnitTest --tests "com.runcheck.domain.scoring.HealthScoreCalculatorTest"
.\gradlew.bat :app:connectedDebugAndroidTest --project-prop=android.testInstrumentationRunnerArguments.class=com.runcheck.data.db.RuncheckDatabaseMigrationTest --dry-run --no-daemon --no-configuration-cache --console=plain
.\gradlew --no-daemon :app:compileDebugKotlin :app:compileDebugUnitTestKotlin --no-configuration-cache
.\tools\pc.ps1 -PlanOnly
.\tools\sentry.ps1 -PlanOnly
.\tools\dc.ps1 -PlanOnly
.\tools\sc.ps1 -PlanOnly -Full
.\tools\sonar.ps1 -PlanOnly
```

Report-reading convention:

- "lue lint-tulokset" means read `reports/ktlint.txt`, `reports/detekt.txt`, and `reports/lint.txt`.
- "lue security-tulokset" means read `reports/security-summary.txt` first when present, then the reports generated by the chosen security mode: `reports/semgrep-kotlin.txt`, `reports/semgrep-secrets.txt`, `reports/gitleaks.txt`, `reports/trufflehog.txt`, `reports/dependency-verification.txt`, `reports/osv.txt`, and `reports/security-deps.txt` for the full `sc -Full` path.

---

## CI/CD Pipeline

GitHub Actions workflows in `.github/workflows/`:

| Workflow | Purpose | Status |
|----------|---------|--------|
| `codeql.yml` | CodeQL security analysis (`java-kotlin`, manual `assembleDebug`) | Active |
| `security.yml` | Semgrep SAST on PRs/main plus OWASP Dependency-Check on weekly/manual runs | Active; Semgrep is the push/PR code-scanning path. OWASP is kept out of push/PR code scanning because cold NVD updates can stall or return 503s; scheduled/manual runs use cache, bounded retries, a job timeout, a shorter non-blocking OWASP step timeout, and upload the report as an Actions artifact when produced |
| `sonar.yml` | SonarCloud scan through Gradle (`assembleDebug`, `:app:jacocoDebugUnitTestReport`, `sonar`) | Active |
| `qodana.yml` | JetBrains Qodana main-branch scan through `JetBrains/qodana-action` pinned at `v2026.1.3` | Uses the `jetbrains/qodana-jvm-community:2026.1` linter from `qodana.yaml` because the 2026.1 Android linter rejects AGP 9.1.x during IDE import |
| `qodana_code_quality.yml` | JetBrains Qodana action pinned at `v2026.1.3` for `main`, `releases/*`, PRs, and manual dispatch | Uses the `jetbrains/qodana-jvm-community:2026.1` linter from `qodana.yaml` because the 2026.1 Android linter rejects AGP 9.1.x during IDE import |

External services:
- **SonarCloud** — continuous code quality (`Insaner1980_runcheck`, org `insaner1980`). CI path is `.github/workflows/sonar.yml`; local path is `tools/sonar.ps1`.
- **Qodana Cloud** — org "Finnvek Dev", project "runcheck"; current workflow files do not pass `QODANA_TOKEN`, so they should be read as local/action-based Qodana analysis unless a token is added later.

Local PowerShell wrappers:

- `tools/lc.ps1` (`lc`) — ktlint, detekt, Android lint; writes `reports/ktlint.txt`, `reports/detekt.txt`, and `reports/lint.txt`; the shared wrapper appends the Android lint text report and fails high-risk lint policy findings
- `tools/ac.ps1` (`ac`) — Android security surface: project Semgrep, mobsfscan, and DeepSec custom report
- `tools/dc.ps1` (`dc`) — Gradle dependency verification, OSV Scanner, and OWASP Dependency-Check
- `tools/ss.ps1` (`ss`) — gitleaks, TruffleHog, and Semgrep secrets
- `tools/ds.ps1` (`ds`) — DeepSec custom scan/report/revalidate paths
- `tools/ms.ps1` (`ms`) — mobsfscan
- `tools/os.ps1` (`os`) — OSV Scanner
- `tools/ql.ps1` (`ql`) — CodeQL workflow/status check through GitHub tooling
- `tools/db.ps1` (`db`) — Dependabot config and alert check
- `tools/pc.ps1` (`pc`) — PMD CPD duplicate scan; default minimum token threshold is 100 and can be overridden with `PMD_CPD_MINIMUM_TOKENS`
- `tools/cs.ps1` (`cs`) — Compose Stability Analyzer (`:app:stabilityCheck`)
- `tools/cr.ps1` (`cr`) — compose-rules through ktlint and detekt
- `tools/ga.ps1` (`ga`) — Google Android Security Lints through Android lint
- `tools/sc.ps1` (`sc`) — combined security check; `-Full` also runs Android security checks
- `tools/sentry.ps1` (`sentry`) — verifies debug-only Sentry wiring and release classpath exclusion; writes `reports/sentry.txt`
- `tools/sonar.ps1` — SonarCloud local path; requires `SONAR_TOKEN`, runs `assembleDebug`, `:app:jacocoDebugUnitTestReport`, prepares an empty Android Lint import placeholder because `lc` owns real lint findings, and runs `sonar`, then writes `reports/sonar.txt`

When `osv-scanner`, gitleaks, TruffleHog, or PMD are missing from `PATH`, the shared Android-check wrappers may download and cache verified tool binaries under `.gradle/android-check-tools/`; offline first runs can therefore skip or fail before a cached tool exists. The OSV source scan excludes `.deepsec` so Android-check's own DeepSec tooling dependencies do not fail app dependency scans.

Compatibility wrappers and config:

- `scripts/security-check.ps1` forwards to `tools/sc.ps1`
- No Linux shell security wrapper is maintained in this Windows-first repo
- Check configuration lives in `config/semgrep/runcheck-security.yml`, `config/dependency-check/suppressions.xml`, `.mobsf`, `.deepsec/`, `.github/dependabot.yml`, `sonar-project.properties`, and `gradle/osv-scanner.toml`
- `reports/` is ignored and must not be committed

---

## Project Management

- **Linear:** Project "runcheck" in Finnvek team, priority High, status In Progress
- **Linear URL:** https://linear.app/loikka1/project/runcheck-5d6d01d874c1
- **Milestones:**
  - v1.0 — Play Store Release (all features, tested, security audited)
  - Insights Engine (cross-category correlation — the differentiator)
- **GitHub:** https://github.com/Insaner1980/runcheck

---

## Roadmap

### Current Major Differentiator: Insights Engine

runcheck now includes a cross-category correlation engine that analyzes Room data across battery, thermal, network, storage, charger, and app-usage history and surfaces insights automatically on Home. It is one of the clearest product differentiators versus apps that only show siloed metrics. Examples:
- Correlation between temperature rise and battery drain spike
- Network quality degradation at specific times
- Anomaly detection from normal usage patterns

Current Insights rule set:

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

Rules are Hilt multibindings into `Set<InsightRule>`. `InsightEngine` filters generated candidates below 0.6 confidence, replaces results per rule, preserves existing seen/dismissed state for matching dedupe keys, and deletes expired rows before and after generation.

### Known Tool Limitations

- **Qodana:** `qodana.yaml` documents that the Qodana Android linter 2026.1 rejects this repo's AGP 9.1.x during IDE import (`Latest supported version is AGP 9.0.0`). The workflows therefore run `jetbrains/qodana-jvm-community:2026.1` until JetBrains publishes or the project verifies an Android linter compatible with this AGP line. Re-test Qodana on every AGP/Gradle bump.
- **CodeQL:** `.github/workflows/codeql.yml` pins `github/codeql-action/init` and `analyze` to `v4.36.2` and builds with `assembleDebug --no-configuration-cache`. Check the actual CodeQL Action runner and Kotlin extractor support before Kotlin plugin upgrades.
- **Sonar:** AGP 9 support has had scanner-side compatibility churn. Keep `tools/sonar.ps1` and `.github/workflows/sonar.yml` verified when changing AGP, Gradle, or Kotlin.
- **OWASP Dependency-Check:** NVD updates can take a very long time or return transient 503 responses, so PRs and ordinary main pushes run Semgrep/CodeQL/Qodana while Dependency-Check is reserved for weekly scheduled or manual runs with cache, bounded retries, a job timeout, and a shorter non-blocking OWASP step timeout. Dependency-Check reports are uploaded as Actions artifacts instead of GitHub Code scanning SARIF so stale dependency analyses do not keep fixed Dependabot issues open.

---

## Notes for Maintenance

- `PROJECT.md` should describe the code as it exists now, not the intended roadmap only.
- `CODEX.md` and `AGENTS.md` should stay aligned when repository rules or project snapshot notes are updated.
