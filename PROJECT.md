# runcheck — Project Overview

Android device health diagnostics app built with Kotlin and Jetpack Compose. Single dark theme, one-time Pro purchase, no subscriptions.

---

## Technical Snapshot

- Package root: `com.runcheck`
- Main module: single `app` module
- Architecture: Clean Architecture with `data/`, `domain/`, and `ui/`
- Dependency injection: Hilt
- Database: Room
- Preferences: DataStore
- Background work: WorkManager
- Widgets: Glance app widgets
- Speed test backend: M-Lab NDT7
- Build: Gradle Kotlin DSL
- Compile SDK: 36
- Target SDK: 35
- Min SDK: 26
- Java target: 17

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

---

## Navigation

Push-based navigation from a single Home screen. No bottom nav, no tabs.

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

Defined routes in code:

- `home`
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

---

## Runtime Systems

### App Startup

`RuncheckApp` initializes and coordinates:

- Billing and Pro state
- Notification channels
- Crash reporting controller
- Screen-state tracking repository
- Periodic monitoring scheduling
- Widget refreshes when Pro state changes

### Background Monitoring

Two WorkManager jobs are scheduled through `MonitorScheduler`:

- `HealthMonitorWorker`
  - collects battery, network, thermal, and storage snapshots
  - persists readings into Room
  - evaluates alert conditions
  - posts notifications for low battery, high temperature, low storage, and charge complete
- `HealthMaintenanceWorker`
  - collects per-app usage snapshots
  - cleans up old readings
  - refreshes widgets

Supporting monitor components include:

- `BootReceiver`
- `ScreenStateReceiver`
- `ScreenStateTracker`
- `MonitoringAlertStateStore`
- `NotificationHelper`

### Widgets

Two Glance widgets are present:

- Battery widget
- Health score widget

Widget data is read from the latest Room snapshots. Widget access is treated as a Pro feature.

---

## Home Screen

Home is the single entry point and aggregates the latest device state from battery, network, thermal, and storage.

Main sections:

- Health score hero with animated `ProgressRing`
- Battery hero card
- 2×2 quick status grid
  - Network
  - Thermal
  - Charger comparison
  - Storage
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
- Purchased Pro “Insights” card

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
- Current stats are tracked in-memory and reset on status change
- Session and history charts can open a fullscreen landscape chart route
- Battery screen also consumes dismissed educational/info cards

---

## Network Detail

Network detail focuses on current connection state plus historical signal/latency data.

Main sections:

- Signal hero with `SignalBars`
- Wi-Fi name permission help card when SSID is unavailable
- Connection details card
- IP / DNS / MTU section
- Pro-only signal history chart
- Speed test summary card

Historical chart behavior:

- Metrics: signal strength or latency
- Period selection is stored in ViewModel saved state
- Fullscreen chart route is available from the expandable chart container

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

Free tier stores a limited history. Pro keeps a larger history.

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
- Storage detail metrics
- Optional SD card section
- Educational/info cards

Cleanup tool entry points:

- Large Files
- Old Downloads
- APK Files
- Trash (API 30+)

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
- API 30+ delete flow uses system delete request / intent sender
- Selected filter is stored through `SavedStateHandle`

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

- Usage snapshots are collected periodically in maintenance work
- Data is persisted and then paged back into the UI

---

## Learn

Learn is a lightweight educational content flow built entirely in-app.

Screens:

- Learn topic list screen
- Learn article detail screen

Behavior:

- Articles are grouped by topic
- Article detail renders structured body text from catalog resources
- Some articles expose cross-link buttons into app routes

---

## Fullscreen Chart

Fullscreen charts are launched from battery and network trend sections.

Behavior:

- Landscape-only while the route is visible
- Supports battery history, battery session, and network history sources
- Metric and period chip selections are stored in `SavedStateHandle`
- Uses the same chart data model and tooltip formatting as embedded charts

---

## Settings

Settings is broader than a simple preferences page and covers monitoring, privacy, export, purchase flow, and device capability info.

Sections:

- Monitoring interval
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
- Data
  - data retention
  - CSV export
  - clear all data
- Pro
  - current Pro status
  - upgrade CTA
  - restore purchase
- Privacy
  - crash reporting toggle
- Device
  - device model
  - API level
  - current-now reliability
  - cycle-count availability
  - thermal zone count
- About
  - version
  - Play Store link
  - privacy policy
  - feedback email intent

Notes:

- Notification permission handling is built into Settings for Android 13+
- Crash reporting is opt-in
- Export shares one or more CSV files through `FileProvider`

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
- User preferences and dismissed info cards

Persistence technologies:

- Room for telemetry/history entities
- DataStore for user preferences and UI dismissals

---

## Monetization Model

Pro state is handled through `BillingManager`, `ProManager`, and `ProState`.

Current product behavior:

- Trial state is treated as Pro access
- Expired trial loses gated features
- Purchased Pro unlocks all Pro features permanently
- No subscriptions

Pro-gated areas currently include:

- Charger Comparison
- App Usage
- Extended history
- Thermal logs
- CSV export
- Widgets
- Remaining charge time

---

## Future Considerations

- **Learn article read/unread tracking:** Currently no persistence of which Learn articles the user has read. With only 14 articles this isn't needed yet, but if the catalog grows significantly (30+), consider adding DataStore-backed read state with visual indicators (e.g., unread dot on `LearnArticleCard`).

---

## Notes for Maintenance

- `PROJECT.md` should describe the code as it exists now, not the intended roadmap only.
- `CODEX.md` and `AGENTS.md` should stay aligned when repository rules or project snapshot notes are updated.
