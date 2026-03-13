# CLAUDE.md — DevicePulse Project Instructions

## Project Overview

DevicePulse is a native Android app (Kotlin + Jetpack Compose) that monitors device health across four categories: battery, network, thermal, and storage. It provides real-time diagnostics, a unified health score, and long-term trend tracking.

## Tech Stack

- **Language:** Kotlin (via AGP 9.1.0 built-in Kotlin, `android.builtInKotlin` disabled for KSP compatibility)
- **UI:** Jetpack Compose with Material 3 / Material You (BOM 2026.02.01)
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 36 (required by Vico 3.x)
- **Architecture:** MVVM with Clean Architecture layers (data → domain → ui)
- **Database:** Room 2.8.4 for local historical data
- **Async:** Kotlin Coroutines 1.10.2 + Flow
- **DI:** Hilt 2.59.2
- **Charts:** Vico 3.0.3 (Compose-native charting library)
- **Build:** Gradle 9.4.0 with Kotlin DSL, AGP 9.1.0, KSP 2.3.1

## Project Structure

```
app/src/main/java/com/devicepulse/
├── data/
│   ├── battery/        # BatteryManager wrappers, sysfs readers
│   ├── network/        # ConnectivityManager, TelephonyManager
│   ├── thermal/        # ThermalManager, CPU temp sysfs readers
│   ├── storage/        # StorageStatsManager
│   ├── device/         # Device detection, DeviceProfile, capability mapping
│   └── db/             # Room database, DAOs, entities, migrations
├── domain/
│   ├── model/          # Domain models (BatteryState, NetworkState, etc.)
│   ├── usecase/        # Business logic (CalculateHealthScore, GetBatteryTrend, etc.)
│   └── scoring/        # Health score algorithm
├── ui/
│   ├── home/           # Single home screen (hub) + ViewModel
│   ├── battery/        # Battery detail screen + ViewModel
│   ├── network/        # Network detail screen + ViewModel
│   ├── thermal/        # Thermal detail screen + ViewModel
│   ├── storage/        # Storage detail screen + ViewModel
│   ├── charger/        # Charger comparison screen + ViewModel
│   ├── appusage/       # App battery usage screen + ViewModel
│   ├── settings/       # Settings screen + ViewModel
│   ├── theme/          # Dark theme, color tokens, typography, spacing
│   ├── common/         # Shared formatting helpers (formatPercent, formatTemp, etc.)
│   ├── components/     # Shared composables: ProgressRing, MiniBar, GridCard, ListRow,
│   │                   #   SectionHeader, IconCircle, StatusDot, ProBadgePill,
│   │                   #   PrimaryTopBar, DetailTopBar, MetricTile, ConfidenceBadge,
│   │                   #   ProFeatureCalloutCard, ProFeatureLockedState, charts, etc.
│   └── navigation/     # NavGraph + Screen sealed class (push-based from Home)
└── service/
    └── monitor/        # Background WorkManager jobs for periodic readings
```

## Code Style & Conventions

- Write idiomatic Kotlin — use data classes, sealed classes, extension functions
- All UI in Jetpack Compose — no XML layouts, no Fragments
- Use `StateFlow` for ViewModel → UI state
- Use `sealed interface` for UI state (Loading / Success / Error)
- Name ViewModels as `[Screen]ViewModel` (e.g., `HomeViewModel`, `BatteryViewModel`)
- Name UseCases as verb phrases (e.g., `CalculateHealthScoreUseCase`)
- Name composables as nouns (e.g., `ProgressRing`, `GridCard`)
- Keep composables small and focused — extract when > ~50 lines
- All hardcoded strings must go into `strings.xml` for localization
- Comments in English
- No `!!` operator — use safe calls, `requireNotNull`, or sealed error types

## Design System

- **Single dark theme** — no light mode, no AMOLED toggle, no dynamic colors
- **Dark palette:**
  - BgPage = `#0B1E24`, BgCard = `#133040`, BgIconCircle = `#1A3A4D`
  - Accent Teal `#4DD0B8`, Accent Blue `#5BA8F5`, Accent Orange `#F5A05B`, Accent Red `#F55B5B`
  - Accent Lime `#A8F55B`, Accent Yellow `#F5D45B`
  - TextPrimary `#E0E0E0`, TextSecondary `#ABABAB`, TextMuted `#707070`
- **Typography:** System Roboto (no custom fonts) — M3 defaults via `MaterialTheme.typography`
- **Navigation:** Push-based from single Home screen (no bottom nav bar)
- **Cards:** Flat `BgCard` background, no borders, no shadows, no elevation, 16dp rounded corners
- **Core components:** ProgressRing, MiniBar, GridCard, ListRow, SectionHeader, IconCircle, StatusDot, ProBadgePill, PrimaryTopBar, DetailTopBar, MetricTile
- **Status colors** via `MaterialTheme.statusColors` extension (healthy/fair/poor/critical) — always paired with icons or text labels for accessibility
- **Animations:**
  - ProgressRing: 1200ms ease-out (`FastOutSlowInEasing`) from 0 to target
  - MiniBar: 800ms ease-out from 0 to target
  - Both respect `MaterialTheme.reducedMotion` (instant when true)
  - No card entrance animations
- Contrast ratio minimum: **4.5:1** body text, **3:1** large text (WCAG AA)
- Minimum touch target: 48dp
- Spacing based on 4dp grid: 4/8/12/16/24/32dp tokens via `MaterialTheme.spacing`

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
- Respect battery optimization — don't fight the OS
- Detect and handle data gaps gracefully in trend charts

## Database

- Room with auto-migrations
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
- Pro version: one-time in-app purchase (€3.49), unlocks extended history, widgets, charger comparison, export, and advanced insights
- Use Google Play Billing Library
- Gate pro features with a simple `ProStatusRepository` that checks purchase state

## Build & Release

- Use a single `app` module (no multi-module until necessary)
- ProGuard/R8 minification enabled for release builds
- Generate signed APK/AAB for Play Store
- Version code: auto-increment
- Version name: semver (1.0.0)

## Build Notes

- AGP 9.1.0 built-in Kotlin is disabled (`android.builtInKotlin=false`) because KSP 2.3.1 requires the separate Kotlin plugin
- `android.disallowKotlinSourceSets=false` is needed for KSP generated sources with AGP 9
- `kotlin.compose` plugin is applied separately for Compose compiler support
- `BatteryManager.BATTERY_PROPERTY_CHARGING_CYCLE_COUNT` and `STATE_OF_HEALTH` are not in the public SDK — use raw integer constants (8 and 12)
- Pull-to-refresh uses `PullToRefreshBox` (not the deprecated `PullToRefreshContainer`)
- Vico 3.x removed the `core` module (merged into `views`); only `compose` and `compose-m3` are needed

## Important Notes

- This is an Android-only app — no iOS, no cross-platform
- Privacy-first: no analytics, no tracking, no account system, no network calls except optional latency ping
- Measurement and history data stay on device unless the user explicitly enables crash reporting, which sends crash diagnostics to Firebase Crashlytics
- The spec file `device-health-monitor-spec.md` in the repo root contains the full feature specification — refer to it for detailed feature requirements and UI design guidelines
- The roadmap and next steps are documented in `docs/plans/2026-03-10-phase1-completion-and-roadmap.md`
