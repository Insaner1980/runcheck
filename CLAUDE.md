# CLAUDE.md — DevicePulse Project Instructions

## Project Overview

DevicePulse is a native Android app (Kotlin + Jetpack Compose) that monitors device health across four categories: battery, network, thermal, and storage. It provides real-time diagnostics, a unified health score, and long-term trend tracking.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3 / Material You
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **Architecture:** MVVM with Clean Architecture layers (data → domain → ui)
- **Database:** Room for local historical data
- **Async:** Kotlin Coroutines + Flow
- **DI:** Hilt
- **Charts:** Vico (Compose-native charting library) or equivalent
- **Build:** Gradle with Kotlin DSL (build.gradle.kts)

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
│   ├── dashboard/      # Main dashboard screen + ViewModel
│   ├── battery/        # Battery detail screen + ViewModel
│   ├── network/        # Network detail screen + ViewModel
│   ├── thermal/        # Thermal detail screen + ViewModel
│   ├── storage/        # Storage detail screen + ViewModel
│   ├── settings/       # Settings screen + ViewModel
│   ├── theme/          # Material You theme, color schemes, typography
│   ├── components/     # Shared composables (gauges, charts, cards, badges)
│   └── navigation/     # Navigation graph, bottom nav setup
└── service/
    └── monitor/        # Background WorkManager jobs for periodic readings
```

## Code Style & Conventions

- Write idiomatic Kotlin — use data classes, sealed classes, extension functions
- All UI in Jetpack Compose — no XML layouts, no Fragments
- Use `StateFlow` for ViewModel → UI state
- Use `sealed interface` for UI state (Loading / Success / Error)
- Name ViewModels as `[Screen]ViewModel` (e.g., `DashboardViewModel`)
- Name UseCases as verb phrases (e.g., `CalculateHealthScoreUseCase`)
- Name composables as nouns (e.g., `HealthGauge`, `BatteryCard`)
- Keep composables small and focused — extract when > ~50 lines
- All hardcoded strings must go into `strings.xml` for localization
- Comments in English
- No `!!` operator — use safe calls, `requireNotNull`, or sealed error types

## Material You / Design Rules

- Use `MaterialTheme.colorScheme` everywhere — never hardcode colors
- Support dynamic colors (`DynamicColors`) on Android 12+
- Provide fallback color scheme (teal/cyan primary) for older devices
- **Three theme modes:** Light, Dark (#121212 surface), AMOLED Black (#000000 surface)
  - Dark is the default dark mode — best readability and no OLED smearing
  - AMOLED Black is opt-in toggle within dark mode settings — maximum battery saving on OLED
- **Never use pure white (#FFFFFF) text on dark backgrounds** — use #E0E0E0 for primary text, #ABABAB for secondary. Pure white causes halation and eye strain.
- **AMOLED Black cards use #0A0A0A**, not pure black, to maintain visual hierarchy
- Use semantic status colors via custom theme extensions:
  - Green for healthy/good
  - Yellow/amber for fair/attention
  - Red for poor/critical
- Status colors must ALWAYS be paired with icons or text labels — never color alone (color blindness accessibility)
- Contrast ratio minimum: **4.5:1** body text, **3:1** large text (WCAG AA)
- All shapes use M3 shape system (no custom hardcoded corner radii)
- Light, Dark, and AMOLED Black themes must all look polished — test all three
- Minimum touch target: 48dp
- Use **Roboto Mono** for real-time numeric values (mA, mV, °C) to prevent layout jitter
- Respect `AccessibilityManager.isReducedMotionEnabled` — disable or simplify animations when set
- Spacing based on 4dp grid: 4/8/12/16/24/32dp tokens

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

- Free version: small banner ad on detail screens only (not dashboard)
- Pro version: one-time in-app purchase (€3.49), unlocks extended history, widgets, charger comparison, export, ad removal
- Use Google Play Billing Library
- Gate pro features with a simple `ProStatusRepository` that checks purchase state

## Build & Release

- Use a single `app` module (no multi-module until necessary)
- ProGuard/R8 minification enabled for release builds
- Generate signed APK/AAB for Play Store
- Version code: auto-increment
- Version name: semver (1.0.0)

## Important Notes

- This is an Android-only app — no iOS, no cross-platform
- Privacy-first: no analytics, no tracking, no account system, no network calls except optional latency ping
- All data stays on device
- The spec file `device-health-monitor-spec.md` in the repo root contains the full feature specification — refer to it for detailed feature requirements and UI design guidelines
