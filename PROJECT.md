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
- Screen-state tracking repository
- Periodic monitoring scheduling
- Widget refreshes when Pro state changes
- Source-set-specific `SentryInit` initialization; debug builds may report to Sentry, release builds are a no-op

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
- History charts use "Instrument Sweep" animation (3-phase: grid fade → oscilloscope sweep → emphasis)
- Live current/power charts use smooth scroll interpolation + glow pulse on new data
- Battery screen also consumes dismissed educational/info cards

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
- Uses the same chart data model, tooltip formatting, and "Instrument Sweep" animation as embedded charts

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
- About
  - version
  - Play Store link
  - privacy policy
  - feedback email intent

Notes:

- Notification permission handling is built into Settings for Android 13+
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
| AccentLime | `#C8E636` | — | Storage documents category |
| AccentYellow | `#F5D03A` | — | Storage downloads category |

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
| numericHeroValueTextStyle | displayLarge | 48sp Bold | Large hero values |
| numericHeroLargeValueTextStyle | displayLarge | 54sp | Battery level display |
| numericHeroLevelTextStyle | displayLarge | 48sp Bold, -2sp tracking | Compact hero values |
| numericHeroUnitTextStyle | headlineLarge | 20sp SemiBold | Units next to hero values |
| numericRingValueTextStyle | displayMedium | 32sp Bold | ProgressRing center value |
| numericSpeedHeroValueTextStyle | displaySmall | 40sp | Speed test hero display |
| chartAxisTextStyle | labelSmall | 10sp | Chart axis labels |
| chartTooltipTextStyle | bodySmall | 11sp | Chart tooltip values |

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
| xs | 4dp | Tight gaps |
| sm | 8dp | Small gaps, inter-row spacing |
| md | 12dp | Between cards, standard gaps |
| base | 16dp | Card padding, section spacing |
| lg | 24dp | Between sections |
| xl | 32dp | Page margins, large separations |

**Dividers:** `outlineVariant.copy(alpha = 0.35f)` — no hardcoded colors.

**Contrast:** Minimum 4.5:1 body text, 3:1 large text (WCAG AA). Minimum touch target 48dp.

### Logo

Health-score arc (~210°) wrapping a phone silhouette, rendered in AccentBlue.
Icon source files in `icons/` directory (SVG masters + 512px PNG exports).

---

## CI/CD Pipeline

GitHub Actions workflows in `.github/workflows/`:

| Workflow | Purpose | Status |
|----------|---------|--------|
| `codeql.yml` | CodeQL security analysis (java-kotlin, manual build) | Active |
| `security.yml` | Semgrep SAST + OWASP Dependency-Check (SARIF → Code Scanning) | Active |
| `qodana.yml` | JetBrains Qodana code quality (Community for Android) | Blocked — AGP 9 not yet supported |

External services:
- **SonarCloud** — continuous code quality (`Insaner1980_runcheck`, org `insaner1980`). CI workflow in runcheck repo configured separately.
- **Qodana Cloud** — org "Finnvek Dev", project "runcheck"

Local tools:
- `scripts/security-check.sh` — runs Semgrep + OWASP dependency-check locally, results in `reports/`
- `lint-check` / `security-check` aliases (`lc` / `sc`)

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

### Next Major Feature: Insights Engine

Cross-category correlation engine that analyzes Room data across all four monitoring categories (battery, thermal, network, storage) and surfaces insights, trends, and anomalies automatically. Differentiator vs AccuBattery, DevCheck, AIDA64 which show categories as separate silos. Examples:
- Correlation between temperature rise and battery drain spike
- Network quality degradation at specific times
- Anomaly detection from normal usage patterns

### Known Tool Limitations

- **Qodana:** AGP 9.1.0 not yet supported (`AndroidArtifact.getPrivacySandboxSdkInfo()`)
- **CodeQL:** Works with Kotlin 2.3.0 (runcheck), does NOT support 2.3.20+

---

## Notes for Maintenance

- `PROJECT.md` should describe the code as it exists now, not the intended roadmap only.
- `CODEX.md` and `AGENTS.md` should stay aligned when repository rules or project snapshot notes are updated.
