DevicePulse Project Instructions

## Project Overview

DevicePulse is a native Android app (Kotlin + Jetpack Compose) that monitors device health across four categories: battery, network, thermal, and storage. It provides real-time diagnostics, a unified health score, and long-term trend tracking.

## Tech Stack

- Language: Kotlin (via AGP 9.1.0 built-in Kotlin, `android.builtInKotlin` disabled for KSP compatibility)
- UI: Jetpack Compose with Material 3 / Material You (BOM 2026.02.01)
- Min SDK: 26 (Android 8.0)
- Target SDK: 35 (Android 15)
- Compile SDK: 36 (required by Vico 3.x)
- Architecture: MVVM with Clean Architecture layers (`data -> domain -> ui`)
- Database: Room 2.8.4 for local historical data
- Async: Kotlin Coroutines 1.10.2 + Flow
- DI: Hilt 2.59.2
- Charts: Vico 3.0.3
- Build: Gradle 9.4.0 with Kotlin DSL, AGP 9.1.0, KSP 2.3.1

## Project Structure

```text
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
│   ├── usecase/        # Business logic
│   └── scoring/        # Health score algorithm
├── ui/
│   ├── dashboard/      # Main dashboard screen + ViewModel
│   ├── battery/        # Battery detail screen + ViewModel
│   ├── network/        # Network detail screen + ViewModel
│   ├── thermal/        # Thermal detail screen + ViewModel
│   ├── storage/        # Storage detail screen + ViewModel
│   ├── settings/       # Settings screen + ViewModel
│   ├── theme/          # Material You theme, color schemes, typography
│   ├── components/     # Shared composables
│   └── navigation/     # Navigation graph, bottom nav setup
└── service/
    └── monitor/        # Background WorkManager jobs for periodic readings
```

## Code Style & Conventions

- Write idiomatic Kotlin: use data classes, sealed classes/interfaces, and extension functions where appropriate
- All UI is Jetpack Compose: no XML layouts, no Fragments
- Use `StateFlow` for ViewModel -> UI state
- Use `sealed interface` for UI state (`Loading / Success / Error`)
- Name ViewModels as `[Screen]ViewModel`
- Name UseCases as verb phrases (for example `CalculateHealthScoreUseCase`)
- Name composables as nouns (for example `HealthGauge`, `BatteryCard`)
- Keep composables small and focused; extract when they get too large
- All hardcoded strings go into `strings.xml`
- Comments in English
- No `!!`; use safe calls, `requireNotNull`, or explicit error handling

## Material You / Design Rules

- Use `MaterialTheme.colorScheme` everywhere; never hardcode colors in feature code
- Support dynamic colors on Android 12+
- Provide fallback color scheme (teal/cyan primary) for older devices
- Support Light, Dark, and AMOLED Black themes
- Never use pure white (`#FFFFFF`) text on dark backgrounds; prefer softer text colors
- AMOLED Black cards use `#0A0A0A`, not pure black
- Use semantic status colors through theme extensions:
  - Green for healthy/good
  - Yellow/amber for fair/attention
  - Red for poor/critical
- Status colors must always be paired with icons or text labels
- Contrast ratio minimum: 4.5:1 body text, 3:1 large text
- All shapes use the Material 3 shape system
- Minimum touch target: 48dp
- Use monospaced numeric typography for real-time values that would otherwise jitter
- Respect reduced motion accessibility settings
- Spacing follows a 4dp grid: 4 / 8 / 12 / 16 / 24 / 32dp

## Device Detection System

The `DeviceCapabilityManager` determines what data is reliably available on the current device. The app must never show inaccurate data without warning.

Rules:
- Validate `BATTERY_PROPERTY_CURRENT_NOW` at startup for non-zero, changing, plausible values
- Store results in `DeviceProfile` and use them throughout the app
- Show confidence badges: Accurate / Estimated / Unavailable
- If a metric is unavailable, hide it or show a clear unsupported/unavailable state
- Never show unsupported metrics as zero

## Manufacturer-Specific Handling

Use `BatteryDataSourceFactory` to select the best battery data source:
- API 34+ devices: use `BATTERY_PROPERTY_CHARGING_CYCLE_COUNT` and `STATE_OF_HEALTH`
- Samsung: handle theoretical-current-only readings
- OnePlus: handle SUPERVOOC sign conventions
- Google Pixel: treat as baseline for reliable behavior
- Fallback: `GenericBatterySource` with confidence warnings

## Background Monitoring

- Use WorkManager for periodic background readings
- Default interval: 30 minutes (user-configurable: 15 / 30 / 60 minutes)
- Foreground service only while the user is actively viewing real-time data
- Respect battery optimization
- Handle missing background samples gracefully in charts and trends

## Database

- Use Room with auto-migrations where appropriate
- Free tier retains only 24 hours of readings
- Pro tier supports configurable retention
- Timestamp columns need indices for range queries

## Testing

- Write unit tests for use cases and scoring logic
- Write unit tests for device capability/profile validation logic
- Use Compose UI tests for critical flows
- Fake or mock Android system APIs in tests

## Monetization

- Free version: small banner ad on detail screens only, not on the dashboard
- Pro version: one-time in-app purchase unlocking extended history, widgets, charger comparison, export, and ad removal
- Use Google Play Billing
- Gate pro features through `ProStatusRepository`

## Build & Release

- Single `app` module unless there is a clear need for more
- ProGuard / R8 enabled for release builds
- Generate signed APK / AAB for Play Store
- Version code auto-increments
- Version name follows semver

## Build Notes

- `android.builtInKotlin=false` is required because KSP 2.3.1 needs the separate Kotlin plugin
- `android.disallowKotlinSourceSets=false` is required for KSP generated sources with AGP 9
- `kotlin.compose` plugin is applied separately for Compose compiler support
- `BatteryManager.BATTERY_PROPERTY_CHARGING_CYCLE_COUNT` and `STATE_OF_HEALTH` are not public SDK constants; use raw constants 8 and 12
- Pull-to-refresh uses `PullToRefreshBox`
- Vico 3.x no longer needs the old `core` module

## Important Notes

- Android-only app; no iOS or cross-platform targets
- Privacy-first: no analytics, no tracking, no accounts
- No external network calls except optional latency / speed-test related checks
- All user data stays on device
- `device-health-monitor-spec.md` contains the full feature spec
- `docs/plans/2026-03-10-phase1-completion-and-roadmap.md` contains roadmap and next steps

## Modification Rules For Codex

- Prefer minimal, targeted changes
- Do not rewrite working modules unless requested
- Follow existing architecture and naming conventions
- Keep UI and business logic separated by layer boundaries
