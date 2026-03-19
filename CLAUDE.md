# CLAUDE.md — runcheck Project Instructions

## Project Overview

runcheck is a native Android app (Kotlin + Jetpack Compose) that monitors device health across four categories: battery, network, thermal, and storage. It provides real-time diagnostics, a unified health score, long-term trend tracking, and storage cleanup tools.

## Tech Stack

- **Language:** Kotlin (via AGP 9.1.0 built-in Kotlin)
- **UI:** Jetpack Compose with Material 3 / Material You (BOM 2026.02.01)
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 36
- **Architecture:** MVVM with Clean Architecture layers (data → domain → ui)
- **Database:** Room 2.8.4 for local historical data
- **Async:** Kotlin Coroutines 1.10.2 + Flow
- **DI:** Hilt 2.59.2
- **Charts:** Custom Compose components (TrendChart, AreaChart, HeatStrip, SegmentedBar)
- **Build:** Gradle 9.4.0 with Kotlin DSL, AGP 9.1.0, KSP 2.3.1
- **Localization:** English (default) + Finnish (fi)

## Project Structure

```
app/src/main/java/com/runcheck/
├── data/
│   ├── appusage/      # App battery usage data source
│   ├── battery/       # BatteryManager wrappers, BatteryDataSourceFactory,
│   │                  #   GenericBatterySource + manufacturer-specific sources
│   │                  #   (Android14, Samsung, OnePlus), BatteryCapacityReader
│   ├── billing/       # BillingManager (lifecycle-aware billing service),
│   │                  #   ProStatusCache
│   ├── charger/       # Charger comparison data
│   ├── crash/         # Firebase Crashlytics integration
│   ├── device/        # Device detection, DeviceProfile, DeviceProfileProvider,
│   │                  #   DeviceCapabilityManager
│   ├── db/            # Room database, DAOs, entities, migrations
│   ├── export/        # Data export functionality
│   ├── network/       # ConnectivityManager, TelephonyManager, speed test
│   ├── preferences/   # DataStore preferences
│   ├── storage/       # StorageStatsManager, MediaStoreScanner, ThumbnailLoader,
│   │                  #   StorageCleanupHelper (createDeleteRequest)
│   └── thermal/       # ThermalManager, CPU temp sysfs readers
├── domain/
│   ├── model/         # Domain models (BatteryState, NetworkState, StorageState, etc.)
│   ├── usecase/       # Business logic (27+ use cases)
│   ├── repository/    # Repository interfaces
│   └── scoring/       # Health score algorithm
├── ui/
│   ├── home/          # Single home screen (hub) + ViewModel
│   ├── battery/       # Battery detail screen + ViewModel + session stats
│   ├── network/       # Network detail screen + ViewModel + speed test
│   ├── thermal/       # Thermal detail screen + ViewModel + session min/max
│   ├── storage/       # Storage detail screen + ViewModel + cleanup/
│   │                  #   (CleanupScreen, CleanupViewModel, FileListItem, CategoryGroup)
│   ├── charger/       # Charger comparison screen + ViewModel
│   ├── appusage/      # App battery usage screen + ViewModel
│   ├── settings/      # Settings screen + ViewModel
│   ├── ads/           # Ad banner components
│   ├── pro/           # Pro upgrade screen, trial UI, purchase flow
│   ├── theme/         # Dark theme, color tokens, typography, spacing
│   ├── common/        # UiText, UiFormatters (formatPercent, formatTemp, etc.)
│   ├── components/    # 29 shared composables (see Components below)
│   └── navigation/    # NavGraph + Screen sealed class (push-based from Home)
├── pro/               # Pro/trial state management
├── billing/           # Billing state helpers
├── widget/            # Home screen widget data provider
├── worker/            # WorkManager workers (trial notifications)
├── service/
│   └── monitor/       # HealthMonitorWorker, RealTimeMonitorService,
│                      #   ScreenStateTracker, NotificationHelper,
│                      #   MonitorScheduler, BootReceiver
├── di/                # Hilt modules
└── util/              # Logging, timestamp sanitization
```

## Architecture Conventions

- **Domain layer must be pure Kotlin** — no `android.*` or `androidx.*` imports in `domain/`. Use `String` instead of `android.net.Uri`, map at data/UI boundaries.
- **UI layer must not import data layer** — ViewModels inject use cases or domain repository interfaces, never data sources or data-layer classes directly.
- **Data layer must not import UI framework** — no Compose types (`ImageBitmap`, etc.) in `data/`. Return platform primitives (`Bitmap`), convert in UI layer.
- **Inject interfaces, not concrete classes** — repositories and data-layer services use interfaces (`DeviceProfileProvider`, `ForegroundAppProvider`). Bind via `@Binds` in Hilt modules.
- **ViewModels must not hold Context** — use `UiText` sealed interface (`UiText.Resource(@StringRes)` / `UiText.Dynamic(value)`) for error messages and status text. Composables resolve with `.resolve()`.
- **Business logic belongs in use cases** — repositories handle data access only. Calculations (e.g., `CalculateFillRateUseCase`), state machines (e.g., `TrackThrottlingEventsUseCase`), and formatting logic go in `domain/usecase/`.
- **`BillingManager` is a lifecycle-aware service, not a repository** — initialized/destroyed by `RuncheckApp`. Widget updates triggered from Application via Flow collection, not from the billing layer.

## Code Style & Conventions

- Write idiomatic Kotlin — use data classes, sealed classes, extension functions
- All UI in Jetpack Compose — no XML layouts, no Fragments
- Use `StateFlow` for ViewModel → UI state
- Use `sealed interface` for UI state (Loading / Success / Error)
- Name ViewModels as `[Screen]ViewModel` (e.g., `HomeViewModel`, `BatteryViewModel`)
- Name UseCases as verb phrases (e.g., `CalculateHealthScoreUseCase`)
- Name composables as nouns (e.g., `ProgressRing`, `GridCard`)
- Keep composables small and focused — extract when > ~50 lines
- All hardcoded strings must go into `strings.xml` for localization (EN + FI)
- Comments in English
- No `!!` operator — use safe calls, `requireNotNull`, or sealed error types

## Design System

- **Single dark theme** — no light mode, no AMOLED toggle, no dynamic colors
- **Dark palette:**
  - BgPage = `#0B1E24`, BgCard = `#133040`, BgIconCircle = `#1A3A4D`
  - Accent Teal `#4DD0B8`, Accent Blue `#5BA8F5`, Accent Orange `#F5A05B`, Accent Red `#F55B5B`
  - Accent Lime `#A8F55B`, Accent Yellow `#F5D45B`
  - TextPrimary `#E0E0E0`, TextSecondary `#ABABAB`, TextMuted `#707070`
- **Typography:** Manrope (custom, body/headers) + JetBrains Mono (numeric displays) via `MaterialTheme.typography` and `MaterialTheme.numericFontFamily`
- **Navigation:** Push-based from single Home screen (no bottom nav bar)
- **Cards:** Flat `BgCard` background, no borders, no shadows, no elevation, 16dp rounded corners
- **Core components** (32+ in `ui/components/`):
  - Layout: GridCard, ListRow, MetricPill, MetricRow, MetricTile, ActionCard
  - Indicators: ProgressRing, MiniBar, StatusDot, ConfidenceBadge, SignalBars
  - Charts: TrendChart, AreaChart, HeatStrip, SegmentedBar, SegmentedBarLegend
  - Navigation: PrimaryTopBar, DetailTopBar
  - Typography: AnimatedNumber, SectionHeader, CardSectionTitle, IconCircle
  - Pro: ProBadgePill, ProFeatureCalloutCard, ProFeatureLockedState
  - Interactive: PullToRefreshWrapper, PrimaryButton, SecondaryButton
- **Section titles:**
  - `SectionHeader` (outline / TextMuted) — page-level sections, above cards
  - `CardSectionTitle` (onSurfaceVariant / TextSecondary) — subsection titles inside cards
- **Corner radii:** Cards = 16dp, small elements (badges, chips) = 8dp
- **Dividers:** `outlineVariant.copy(alpha = 0.35f)` everywhere — no hardcoded colors
- **Value colors:** Data values default to `onSurface`, status labels use `statusColors`. In GridCard, use `statusLabel`/`statusColor` params to separate data from status.
- **Status colors** via `MaterialTheme.statusColors` extension (healthy/fair/poor/critical) — always paired with icons or text labels for accessibility
- **Animations:**
  - ProgressRing: 1200ms ease-out (`FastOutSlowInEasing`) from 0 to target
  - MiniBar: 800ms ease-out from 0 to target
  - Both respect `MaterialTheme.reducedMotion` (instant when true)
  - No card entrance animations
- Contrast ratio minimum: **4.5:1** body text, **3:1** large text (WCAG AA)
- Minimum touch target: 48dp
- Spacing based on 4dp grid: 4/8/12/16/24/32dp tokens via `MaterialTheme.spacing`

## Battery Features

The battery detail screen has extensive monitoring capabilities:

- **Hero card:** ProgressRing with level %, status text, optional mAh remaining (`BATTERY_PROPERTY_CHARGE_COUNTER`)
- **W + mV display:** Power in watts and voltage in millivolts shown under the mA current reading
- **Current stats:** In-memory avg/min/max tracking that resets when charging status changes
- **Battery capacity:** Design capacity via PowerProfile reflection, estimated capacity from `designCapacity × healthPercent / 100`
- **Screen On/Off tracking:** `ScreenStateTracker` with BroadcastReceiver for `ACTION_SCREEN_ON/OFF/POWER_DISCONNECTED`, tracks drain rate per screen state
- **Deep Sleep / Held Awake:** Tracks `PowerManager.isDeviceIdleMode` during screen-off periods
- **Long-term statistics:** `GetBatteryStatisticsUseCase` queries Room for charged/discharged totals, session counts, average drain rates over configurable period
- **History:** TrendChart with `SINCE_UNPLUG`, Day, Week, Month, All periods. SINCE_UNPLUG queries last charging timestamp from Room.
- **Session graph:** Current and power charts with 15m/30m/All windows during charging

## Storage Features

The storage detail screen provides monitoring and cleanup tools:

- **Hero card:** ProgressRing with usage %, used/total, fill rate, cache total, free space MetricPills
- **Media breakdown:** `SegmentedBar` (Canvas, 12dp, animated) with 6 categories: Images (Teal), Videos (Blue), Audio (Orange), Documents (Lime), Downloads (Yellow), Other (Muted). `SegmentedBarLegend` with StatusDots.
- **Cleanup tools:** `ActionCard` components (outlined border) linking to reusable `CleanupScreen`:
  - Large Files (threshold: 10/50/100/500 MB)
  - Old Downloads (age: 30/60/90/365 days)
  - APK Files (no filter, pre-selected)
  - Trash (API 30+, inline empty via `MediaStore.createDeleteRequest`)
- **Cleanup screen:** Shared `cleanup/{type}` route, category-grouped file list with thumbnails (`ThumbnailLoader` LRU-50), `MiniBar` per file, `CleanupBottomBar` with before/after projection, `CleanupSuccessOverlay` fade animation
- **Delete mechanism:** API 30+ `createDeleteRequest` → system dialog via `ActivityResultLauncher`; API 29 `ContentResolver.delete` fallback
- **Fill rate:** `CalculateFillRateUseCase` — linear regression on Room history (7-day lookback)
- **Quick actions:** System intent links (Storage Settings, Free Up Space, Usage Access)
- **SD card:** Shown if detected via `getExternalFilesDirs`

## Settings Features

Settings screen uses grouped card layout with these sections:

- **Monitoring:** Interval selection (15/30/60 min)
- **Notifications:** Master toggle + per-alert toggles (Low Battery, High Temp, Low Storage, Charge Complete). Master off dims and disables sub-toggles.
- **Alert thresholds:** Sliders for battery (5–50%, default 20), temperature (35–50°C, default 42), storage (70–99%, default 90). Value displayed in numericFontFamily with primary color.
- **Display:** Temperature unit (°C/°F) — stored in DataStore, affects all temperature displays
- **Data:** Retention (Pro), export (CSV), clear speed tests, clear all data (error-color button + AlertDialog confirmation)
- **Pro:** Status display, purchase button, restore purchase
- **Privacy:** Crash reporting toggle (Firebase Crashlytics)
- **Device:** Read-only MetricPill grid (model, API level, current reliability, cycle count, thermal zones)
- **About:** Version, Rate on Play Store, Privacy Policy, Send Feedback

All preferences stored in `DataStore<Preferences>` via `UserPreferencesRepository`.

## Device Detection System

The `DeviceCapabilityManager` determines what data is reliably available on the current device. This is critical — the app must NEVER show inaccurate data without warning.

Rules:
- Always validate `BATTERY_PROPERTY_CURRENT_NOW` at startup — check for non-zero, changing, plausible values
- Store results in `DeviceProfile` and use it throughout the app
- Show confidence badges: green "Accurate", yellow "Estimated", gray "Unavailable"
- If a metric is unavailable, hide it or show "Not supported on this device" — never show 0 or garbage values

## Manufacturer-Specific Handling

Use `BatteryDataSourceFactory` to select the best data source based on device:
- API 34+ devices: use new `BATTERY_PROPERTY_CHARGING_CYCLE_COUNT` and `STATE_OF_HEALTH`
- Samsung: handle potential max-theoretical-current-only readings
- OnePlus: handle SUPERVOOC sign conventions
- Google Pixel: typically most reliable, use as baseline
- Fallback: `GenericBatterySource` with confidence warnings

## Background Monitoring

- Use WorkManager for periodic background readings (not foreground service for periodic work)
- Default interval: 30 minutes (user-configurable: 15 / 30 / 60 min)
- Foreground service only when user is actively viewing real-time data
- `ScreenStateTracker` runs as `@Singleton`, started/stopped by BatteryViewModel lifecycle
- Respect battery optimization — don't fight the OS
- Detect and handle data gaps gracefully in trend charts

## Database

- Room with auto-migrations
- Tables: `battery_readings`, `storage_readings`, `network_readings`, `thermal_readings`, `throttling_events`, `charger_sessions`, `speed_test_results`
- Free tier: retain only 24 hours of readings (delete older on each write)
- Pro tier: configurable retention (3mo / 6mo / 1yr / forever)
- Indices on timestamp columns for efficient range queries

## Testing

- Write unit tests for all UseCases and scoring logic
- Write unit tests for DeviceProfile validation logic
- UI tests with Compose testing framework for critical flows
- Use fakes/mocks for system APIs (BatteryManager etc.) in tests

## Monetization

- Free version: core monitoring with locked Pro feature entry points where applicable
- Pro version: one-time in-app purchase (€3.49), unlocks extended history, widgets, charger comparison, export, advanced insights, and storage cleanup tools
- Trial system with expiration modal and notification worker
- Use Google Play Billing Library
- Gate pro features with `BillingManager` (implements `ProStatusProvider` + `ProPurchaseManager`)
- Ad banners on detail screens (free tier only)

## Build & Release

- Use a single `app` module (no multi-module until necessary)
- **Static analysis:** ktlint (formatting) + detekt 2.0.0-alpha.2 (code quality, `ignoreFailures = true` during adoption)
- ProGuard/R8 minification enabled for release builds
- Generate signed APK/AAB for Play Store
- Version code: auto-increment
- Version name: semver (1.0.0)

## Build Notes

- AGP 9.1.0 built-in Kotlin handles Kotlin compilation; `kotlin.compose` plugin applied separately for Compose compiler support
- `android.disallowKotlinSourceSets=false` is needed for KSP generated sources with AGP 9
- `BatteryManager.BATTERY_PROPERTY_CHARGING_CYCLE_COUNT` and `STATE_OF_HEALTH` are not in the public SDK — use raw integer constants (8 and 12)
- Pull-to-refresh uses `PullToRefreshBox` (not the deprecated `PullToRefreshContainer`)

## Important Notes

- This is an Android-only app — no iOS, no cross-platform
- Privacy-first: no analytics, no tracking, no account system, no network calls except optional latency ping
- Measurement and history data stay on device unless the user explicitly enables crash reporting, which sends crash diagnostics to Firebase Crashlytics
- The roadmap and next steps are documented in `docs/plans/2026-03-10-phase1-completion-and-roadmap.md`

## Feature Specs

- `docs/battery-enhancements-spec.md` — Battery & thermal enhancements (mAh remaining, W+mV, current stats, temp min/max, screen on/off, sleep analysis, statistics, since-unplug history)
- `docs/storage-enhancements-spec.md` — Storage feature expansion (media breakdown, top apps, cleanup tools, trash management, large file scanner, history chart)
- `docs/storage-ui-design.md` — Storage UI design spec (SegmentedBar, ActionCard, FileListItem, visual patterns)
- `docs/storage-cleanup-spec.md` — Storage cleanup implementation (CleanupScreen, delete flow, thumbnails, category groups, success overlay)
- `docs/settings-enhancements-spec.md` — Settings enhancements (per-alert notifications, alert thresholds, temperature unit, data management, grouped layout)
- `docs/info-tooltips-spec.md` — Info tooltip system
- `docs/ui-consistency-audit.md` — UI consistency findings and fixes
