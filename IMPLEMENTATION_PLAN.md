# DevicePulse — Implementation Plan

This document describes every file to create, in what order, and what each file does. The plan follows Phase 1 (MVP / Free Version Core) from the spec, building the app bottom-up: project scaffold → data layer → domain layer → UI layer → service layer.

---

## Phase 0: Project Scaffold & Build Configuration

### 0.1 Gradle & Build Files

| # | File | Purpose |
|---|------|---------|
| 1 | `build.gradle.kts` (root) | Root Gradle build file. Defines Kotlin, AGP, Hilt, and KSP plugin versions. |
| 2 | `settings.gradle.kts` | Declares the single `:app` module and plugin repositories. |
| 3 | `gradle.properties` | AndroidX opt-in, Kotlin code style, JVM args. |
| 4 | `app/build.gradle.kts` | App module build file. Sets minSdk 26 / targetSdk 35, applies Hilt + KSP + Compose plugins, declares all dependencies (Compose BOM, Material 3, Room, Hilt, Coroutines, Vico charts, Navigation Compose, WorkManager, DataStore Preferences). Enables `buildFeatures.compose = true`. |
| 5 | `gradle/libs.versions.toml` | Version catalog for all dependencies. Centralizes version management. |

### 0.2 Android Manifest & Resources

| # | File | Purpose |
|---|------|---------|
| 6 | `app/src/main/AndroidManifest.xml` | Declares permissions (BATTERY_STATS, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, FOREGROUND_SERVICE, POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, INTERNET), application class (`DevicePulseApp`), main activity (`MainActivity`), and foreground service. |
| 7 | `app/src/main/res/values/strings.xml` | All user-facing strings (screen titles, labels, error messages, confidence badge text, status labels). |
| 8 | `app/src/main/res/values/themes.xml` | Minimal theme stub pointing to Compose theme (for splash/activity window). |
| 9 | `app/src/main/res/font/roboto_mono.xml` | Font family declaration for Roboto Mono (used in numeric displays). |
| 10 | `app/src/main/res/values/dimens.xml` | Spacing tokens (space_xs=4dp through space_xl=32dp) and layout dimensions from the spec. |

### 0.3 Application Entry Points

| # | File | Purpose |
|---|------|---------|
| 11 | `app/src/main/java/com/devicepulse/DevicePulseApp.kt` | `@HiltAndroidApp` Application class. Initializes Hilt dependency graph. |
| 12 | `app/src/main/java/com/devicepulse/MainActivity.kt` | Single-Activity host. `@AndroidEntryPoint`, sets Compose content with the app theme and navigation graph. Handles edge-to-edge display and system bar insets. |

---

## Phase 1: Data Layer

Build from the bottom up — models, database, then data sources.

### 1.1 Room Database

| # | File | Purpose |
|---|------|---------|
| 13 | `app/src/main/java/com/devicepulse/data/db/entity/BatteryReadingEntity.kt` | Room `@Entity` for `battery_readings` table. Fields: id, timestamp, level, voltage_mv, temperature_c, current_ma (nullable), current_confidence, status, plug_type, health, cycle_count (nullable), health_pct (nullable). Index on `timestamp`. |
| 14 | `app/src/main/java/com/devicepulse/data/db/entity/NetworkReadingEntity.kt` | Room `@Entity` for `network_readings`. Fields: id, timestamp, type, signal_dbm, wifi_speed_mbps, wifi_frequency, carrier, network_subtype, latency_ms. Index on `timestamp`. |
| 15 | `app/src/main/java/com/devicepulse/data/db/entity/ThermalReadingEntity.kt` | Room `@Entity` for `thermal_readings`. Fields: id, timestamp, battery_temp_c, cpu_temp_c (nullable), thermal_status, throttling. Index on `timestamp`. |
| 16 | `app/src/main/java/com/devicepulse/data/db/entity/StorageReadingEntity.kt` | Room `@Entity` for `storage_readings`. Fields: id, timestamp, total_bytes, available_bytes, apps_bytes, media_bytes. Index on `timestamp`. |
| 17 | `app/src/main/java/com/devicepulse/data/db/entity/DeviceEntity.kt` | Room `@Entity` for `devices` table. Stores manufacturer, model, api_level, first_seen, profile_json. |
| 18 | `app/src/main/java/com/devicepulse/data/db/dao/BatteryReadingDao.kt` | DAO with insert, query by time range, delete older than timestamp, get latest reading. Returns `Flow<List<BatteryReadingEntity>>` for reactive queries. |
| 19 | `app/src/main/java/com/devicepulse/data/db/dao/NetworkReadingDao.kt` | DAO for network readings — insert, query by time range, delete old, get latest. |
| 20 | `app/src/main/java/com/devicepulse/data/db/dao/ThermalReadingDao.kt` | DAO for thermal readings — insert, query by time range, delete old, get latest. |
| 21 | `app/src/main/java/com/devicepulse/data/db/dao/StorageReadingDao.kt` | DAO for storage readings — insert, query by time range, delete old, get latest. |
| 22 | `app/src/main/java/com/devicepulse/data/db/dao/DeviceDao.kt` | DAO for device profile — insertOrUpdate, getDevice. |
| 23 | `app/src/main/java/com/devicepulse/data/db/DevicePulseDatabase.kt` | `@Database` class listing all entities, all DAOs, version 1. Singleton via Room.databaseBuilder. |
| 24 | `app/src/main/java/com/devicepulse/data/db/Converters.kt` | Room `@TypeConverter` for enums (ChargingStatus, PlugType, etc.) and any JSON serialization needed for profile_json. |

### 1.2 Device Detection

| # | File | Purpose |
|---|------|---------|
| 25 | `app/src/main/java/com/devicepulse/data/device/DeviceProfile.kt` | Data class: manufacturer, model, apiLevel, currentNowReliable, currentNowUnit (enum MICROAMPS/MILLIAMPS), currentNowSignConvention (enum POSITIVE_CHARGING/NEGATIVE_CHARGING), cycleCountAvailable, batteryHealthPercentAvailable, thermalZonesAvailable (List<String>), storageHealthAvailable. |
| 26 | `app/src/main/java/com/devicepulse/data/device/DeviceCapabilityManager.kt` | Runs at first launch (or on demand). Probes `Build.MANUFACTURER`, `Build.MODEL`, `Build.VERSION.SDK_INT`. Reads `CURRENT_NOW` multiple times to validate (non-zero, changing, plausible range). Scans `/sys/class/thermal/thermal_zone*` for available zones. Stores result as `DeviceProfile`. |
| 27 | `app/src/main/java/com/devicepulse/data/device/DeviceProfileRepository.kt` | Persists `DeviceProfile` to Room (via DeviceDao). Provides `getProfile(): Flow<DeviceProfile>` and `refreshProfile()`. Acts as single source of truth. |

### 1.3 Battery Data Sources

| # | File | Purpose |
|---|------|---------|
| 28 | `app/src/main/java/com/devicepulse/data/battery/MeasuredValue.kt` | Generic data class `MeasuredValue<T>(val value: T, val confidence: Confidence)` where `Confidence` is an enum: HIGH, LOW, UNAVAILABLE. Used throughout for measurements with reliability info. |
| 29 | `app/src/main/java/com/devicepulse/data/battery/BatteryDataSource.kt` | Interface defining: `getCurrentNow(): Flow<MeasuredValue<Int>>`, `getVoltage(): Flow<Int>`, `getTemperature(): Flow<Float>`, `getHealth(): Flow<BatteryHealth>`, `getCycleCount(): Flow<Int?>`, `getCapacity(): Flow<Int?>`, `getChargingStatus(): Flow<ChargingStatus>`, `getPlugType(): Flow<PlugType>`, `getLevel(): Flow<Int>`, `getTechnology(): Flow<String>`. |
| 30 | `app/src/main/java/com/devicepulse/data/battery/GenericBatterySource.kt` | Default implementation using `BatteryManager` APIs. Reads `ACTION_BATTERY_CHANGED` broadcast as a Flow. Normalizes current based on DeviceProfile unit/sign convention. Marks current confidence based on `DeviceProfile.currentNowReliable`. |
| 31 | `app/src/main/java/com/devicepulse/data/battery/SamsungBatterySource.kt` | Samsung-specific subclass. Handles max-theoretical-current-only readings. Falls back to GenericBatterySource for other metrics. |
| 32 | `app/src/main/java/com/devicepulse/data/battery/OnePlusBatterySource.kt` | OnePlus-specific subclass. Handles SUPERVOOC sign convention quirks. |
| 33 | `app/src/main/java/com/devicepulse/data/battery/Android14BatterySource.kt` | API 34+ source. Uses `BATTERY_PROPERTY_CHARGING_CYCLE_COUNT` and `BATTERY_PROPERTY_STATE_OF_HEALTH`. Extends GenericBatterySource. |
| 34 | `app/src/main/java/com/devicepulse/data/battery/BatteryDataSourceFactory.kt` | Factory: inspects `DeviceProfile` and returns the appropriate `BatteryDataSource` implementation. |
| 35 | `app/src/main/java/com/devicepulse/data/battery/BatteryRepository.kt` | Repository that wraps `BatteryDataSource`. Exposes combined `BatteryState` flow for UI consumption. Also handles persisting readings to Room at configured intervals. |

### 1.4 Network Data Source

| # | File | Purpose |
|---|------|---------|
| 36 | `app/src/main/java/com/devicepulse/data/network/NetworkDataSource.kt` | Reads `ConnectivityManager` for connection type, `WifiManager` for WiFi details (SSID, link speed, frequency), `TelephonyManager` for cellular info. Exposes as Flow. |
| 37 | `app/src/main/java/com/devicepulse/data/network/LatencyMeasurer.kt` | Measures latency via ICMP ping or HTTP HEAD to a configurable endpoint. Returns latency in ms. |
| 38 | `app/src/main/java/com/devicepulse/data/network/NetworkRepository.kt` | Combines NetworkDataSource + LatencyMeasurer. Exposes `NetworkState` flow. Persists readings to Room. |

### 1.5 Thermal Data Source

| # | File | Purpose |
|---|------|---------|
| 39 | `app/src/main/java/com/devicepulse/data/thermal/ThermalDataSource.kt` | Reads battery temperature from BatteryManager, CPU temps from `/sys/class/thermal/thermal_zone*/temp` (using paths from DeviceProfile), thermal status from `PowerManager.getCurrentThermalStatus()` (API 29+). Exposes as Flow. |
| 40 | `app/src/main/java/com/devicepulse/data/thermal/ThermalRepository.kt` | Wraps ThermalDataSource. Exposes `ThermalState` flow. Persists readings. |

### 1.6 Storage Data Source

| # | File | Purpose |
|---|------|---------|
| 41 | `app/src/main/java/com/devicepulse/data/storage/StorageDataSource.kt` | Uses `StorageStatsManager` and `StatFs` to get total/available space. Calculates per-category breakdown (apps, images, videos, audio, documents, other). Checks for SD card. |
| 42 | `app/src/main/java/com/devicepulse/data/storage/StorageRepository.kt` | Wraps StorageDataSource. Exposes `StorageState` flow. Persists readings. Calculates fill rate from historical data. |

### 1.7 Settings / Preferences

| # | File | Purpose |
|---|------|---------|
| 43 | `app/src/main/java/com/devicepulse/data/preferences/UserPreferencesRepository.kt` | Uses Jetpack DataStore Preferences to store: theme mode (Light/Dark/System), AMOLED black toggle, dynamic colors toggle, monitoring interval (15/30/60 min), notification toggles. Exposes as `Flow<UserPreferences>`. |

### 1.8 Pro Status

| # | File | Purpose |
|---|------|---------|
| 44 | `app/src/main/java/com/devicepulse/data/billing/ProStatusRepository.kt` | Stub for Google Play Billing. Provides `isProUser: Flow<Boolean>`. For MVP, always returns false. Will be wired to billing in Phase 2. |

---

## Phase 2: Domain Layer

### 2.1 Domain Models

| # | File | Purpose |
|---|------|---------|
| 45 | `app/src/main/java/com/devicepulse/domain/model/BatteryState.kt` | Data class: level, voltage, temperature, currentMa (MeasuredValue), chargingStatus, plugType, health, technology, cycleCount, healthPercent. |
| 46 | `app/src/main/java/com/devicepulse/domain/model/NetworkState.kt` | Data class: connectionType (WIFI/CELLULAR/NONE), signalDbm, signalQuality (enum Excellent/Good/Fair/Poor/NoSignal), wifiSsid, wifiSpeedMbps, wifiFrequency, carrier, networkSubtype, latencyMs. |
| 47 | `app/src/main/java/com/devicepulse/domain/model/ThermalState.kt` | Data class: batteryTempC, cpuTempC (nullable), thermalStatus (enum None→Shutdown), isThrottling. |
| 48 | `app/src/main/java/com/devicepulse/domain/model/StorageState.kt` | Data class: totalBytes, availableBytes, usedBytes, usagePercent, appsBytes, mediaBytes, sdCardInfo (nullable), fillRateEstimate (nullable). |
| 49 | `app/src/main/java/com/devicepulse/domain/model/HealthScore.kt` | Data class: overallScore (0-100), batteryScore, networkScore, thermalScore, storageScore, status (enum Healthy/Fair/Poor/Critical). |
| 50 | `app/src/main/java/com/devicepulse/domain/model/Enums.kt` | Shared enums: ChargingStatus, PlugType, BatteryHealth, ConnectionType, SignalQuality, ThermalStatus, HealthStatus, Confidence, CurrentUnit, SignConvention. |

### 2.2 Use Cases

| # | File | Purpose |
|---|------|---------|
| 51 | `app/src/main/java/com/devicepulse/domain/scoring/HealthScoreCalculator.kt` | Pure function: takes BatteryState, NetworkState, ThermalState, StorageState → HealthScore. Implements weighted algorithm (battery 35%, thermal 25%, network 20%, storage 20%). Each sub-score 0-100 based on spec criteria. |
| 52 | `app/src/main/java/com/devicepulse/domain/usecase/GetBatteryStateUseCase.kt` | Wraps BatteryRepository. Returns `Flow<BatteryState>`. Injected via Hilt. |
| 53 | `app/src/main/java/com/devicepulse/domain/usecase/GetNetworkStateUseCase.kt` | Wraps NetworkRepository. Returns `Flow<NetworkState>`. |
| 54 | `app/src/main/java/com/devicepulse/domain/usecase/GetThermalStateUseCase.kt` | Wraps ThermalRepository. Returns `Flow<ThermalState>`. |
| 55 | `app/src/main/java/com/devicepulse/domain/usecase/GetStorageStateUseCase.kt` | Wraps StorageRepository. Returns `Flow<StorageState>`. |
| 56 | `app/src/main/java/com/devicepulse/domain/usecase/CalculateHealthScoreUseCase.kt` | Combines all four state flows, passes to HealthScoreCalculator, returns `Flow<HealthScore>`. |
| 57 | `app/src/main/java/com/devicepulse/domain/usecase/GetBatteryHistoryUseCase.kt` | Queries BatteryReadingDao for last 24 hours (free) or full range (pro). Returns `Flow<List<BatteryReading>>` for charts. |
| 58 | `app/src/main/java/com/devicepulse/domain/usecase/CleanupOldReadingsUseCase.kt` | Deletes readings older than retention limit (24h for free, configurable for pro). Called after each write. |

---

## Phase 3: Dependency Injection

| # | File | Purpose |
|---|------|---------|
| 59 | `app/src/main/java/com/devicepulse/di/DatabaseModule.kt` | Hilt `@Module` providing Room database instance, all DAOs. `@Singleton` scoped. |
| 60 | `app/src/main/java/com/devicepulse/di/DataModule.kt` | Hilt `@Module` providing repositories (BatteryRepository, NetworkRepository, ThermalRepository, StorageRepository, DeviceProfileRepository, UserPreferencesRepository, ProStatusRepository), DeviceCapabilityManager, BatteryDataSourceFactory. |
| 61 | `app/src/main/java/com/devicepulse/di/DomainModule.kt` | Hilt `@Module` providing use cases and HealthScoreCalculator. |

---

## Phase 4: UI Layer

### 4.1 Theme System

| # | File | Purpose |
|---|------|---------|
| 62 | `app/src/main/java/com/devicepulse/ui/theme/Color.kt` | Defines all color constants from the spec: fallback palette (Light/Dark/AMOLED), semantic status colors (healthy green, fair amber, poor orange, critical red, neutral blue, unavailable gray), confidence badge colors. |
| 63 | `app/src/main/java/com/devicepulse/ui/theme/Type.kt` | M3 Typography with spec type scale. Includes Roboto Mono `FontFamily` for numeric body text. |
| 64 | `app/src/main/java/com/devicepulse/ui/theme/Shape.kt` | M3 Shapes — medium (16dp) for cards, full for badges/chips. |
| 65 | `app/src/main/java/com/devicepulse/ui/theme/Spacing.kt` | Custom `Spacing` data class with xs/sm/md/base/lg/xl tokens. Provided via `CompositionLocal`. Composables access via `MaterialTheme.spacing`. |
| 66 | `app/src/main/java/com/devicepulse/ui/theme/StatusColors.kt` | Custom `StatusColors` data class holding semantic colors (healthy, fair, poor, critical, neutral, unavailable) + confidence badge colors. Provided via `CompositionLocal`. Separate palettes for light and dark. |
| 67 | `app/src/main/java/com/devicepulse/ui/theme/Theme.kt` | `DevicePulseTheme` composable. Selects light/dark/AMOLED color scheme. Applies `DynamicColors` on Android 12+, falls back to teal palette. Provides `Spacing` and `StatusColors` via CompositionLocals. Checks `AccessibilityManager.isReducedMotionEnabled` and provides via CompositionLocal for animation control. |

### 4.2 Shared UI Components

| # | File | Purpose |
|---|------|---------|
| 68 | `app/src/main/java/com/devicepulse/ui/components/HealthGauge.kt` | Circular arc gauge composable. Accepts score (0-100), animates arc fill (800ms spring), color transitions across thresholds. Shows score number in Display Large. Respects reduced motion. |
| 69 | `app/src/main/java/com/devicepulse/ui/components/MetricTile.kt` | Compact card composable showing a label, value (in Roboto Mono), optional unit, optional sparkline. Used in dashboard summary cards and detail screens. Min height 72dp. |
| 70 | `app/src/main/java/com/devicepulse/ui/components/ConfidenceBadge.kt` | Small pill composable: green "Accurate", yellow "Estimated", gray "N/A". Takes `Confidence` enum. Scale-in animation on first display. |
| 71 | `app/src/main/java/com/devicepulse/ui/components/StatusIndicator.kt` | Color-coded status dot + text label composable. Takes HealthStatus enum. Always shows both icon and text (accessibility). |
| 72 | `app/src/main/java/com/devicepulse/ui/components/CategoryCard.kt` | Tappable card used on dashboard for each health category. Shows title, key metric, status color, optional mini sparkline. Press feedback animation (scale 0.97x). |
| 73 | `app/src/main/java/com/devicepulse/ui/components/AnimatedNumber.kt` | Composable that animates between number values using rolling counter effect (200ms). Uses `animateIntAsState` / `animateFloatAsState`. Respects reduced motion. |
| 74 | `app/src/main/java/com/devicepulse/ui/components/SparklineChart.kt` | Small inline line chart (40dp height) for dashboard card trend indicators. Draws path left-to-right on entrance (600ms). Uses Canvas/DrawScope. |
| 75 | `app/src/main/java/com/devicepulse/ui/components/TrendChart.kt` | Larger chart (200dp height) for detail screens. Line + gradient fill. Uses Vico library. Touch-to-inspect data points. |
| 76 | `app/src/main/java/com/devicepulse/ui/components/HeatStrip.kt` | Thermal color gradient strip composable. Cool blue → hot red based on temperature value. Pulsing glow animation in critical range (>42°C). |
| 77 | `app/src/main/java/com/devicepulse/ui/components/PullToRefreshWrapper.kt` | Wraps M3 pull-to-refresh around a screen content lambda. Triggers a refresh callback. Custom rotating pulse indicator. |

### 4.3 Navigation

| # | File | Purpose |
|---|------|---------|
| 78 | `app/src/main/java/com/devicepulse/ui/navigation/Screen.kt` | Sealed class defining all navigation destinations: Dashboard, Battery, Network, Thermal, Storage, Settings. |
| 79 | `app/src/main/java/com/devicepulse/ui/navigation/BottomNavBar.kt` | M3 `NavigationBar` composable with 4 items: Dashboard, Battery, Network, More. 80dp height. |
| 80 | `app/src/main/java/com/devicepulse/ui/navigation/NavGraph.kt` | `NavHost` composable defining the navigation graph. Wires all screen destinations. M3 shared axis transitions (horizontal slide + fade, 300ms). |
| 81 | `app/src/main/java/com/devicepulse/ui/navigation/MoreMenu.kt` | Expanded menu for the "More" tab: lists Thermal, Storage, Settings options. |

### 4.4 Dashboard Screen

| # | File | Purpose |
|---|------|---------|
| 82 | `app/src/main/java/com/devicepulse/ui/dashboard/DashboardUiState.kt` | Sealed interface: Loading, Success(healthScore, batteryState, networkState, thermalState, storageState), Error(message). |
| 83 | `app/src/main/java/com/devicepulse/ui/dashboard/DashboardViewModel.kt` | `@HiltViewModel`. Combines all state flows via `CalculateHealthScoreUseCase` and individual state use cases. Exposes `StateFlow<DashboardUiState>`. |
| 84 | `app/src/main/java/com/devicepulse/ui/dashboard/DashboardScreen.kt` | Main composable. Shows HealthGauge at top, four CategoryCards below (battery, network, thermal, storage) with staggered entrance animation (60ms delay each). Pull-to-refresh. Handles Loading/Success/Error states. |

### 4.5 Battery Detail Screen

| # | File | Purpose |
|---|------|---------|
| 85 | `app/src/main/java/com/devicepulse/ui/battery/BatteryUiState.kt` | Sealed interface: Loading, Success(batteryState, history list), Error. |
| 86 | `app/src/main/java/com/devicepulse/ui/battery/BatteryViewModel.kt` | `@HiltViewModel`. Collects battery state + 24h history. Exposes `StateFlow<BatteryUiState>`. |
| 87 | `app/src/main/java/com/devicepulse/ui/battery/BatteryDetailScreen.kt` | Shows all real-time battery metrics using MetricTiles with AnimatedNumbers. Charging current with ConfidenceBadge. Conditional API 34+ metrics. 24h mini TrendChart at bottom. Pull-to-refresh. |

### 4.6 Network Detail Screen

| # | File | Purpose |
|---|------|---------|
| 88 | `app/src/main/java/com/devicepulse/ui/network/NetworkUiState.kt` | Sealed interface: Loading, Success(networkState), Error. |
| 89 | `app/src/main/java/com/devicepulse/ui/network/NetworkViewModel.kt` | `@HiltViewModel`. Collects network state. Exposes `StateFlow<NetworkUiState>`. |
| 90 | `app/src/main/java/com/devicepulse/ui/network/NetworkDetailScreen.kt` | Shows connection type, signal strength (dBm + visual bar), WiFi/cellular details, latency. Signal quality rating. Pull-to-refresh. |

### 4.7 Thermal Detail Screen

| # | File | Purpose |
|---|------|---------|
| 91 | `app/src/main/java/com/devicepulse/ui/thermal/ThermalUiState.kt` | Sealed interface: Loading, Success(thermalState), Error. |
| 92 | `app/src/main/java/com/devicepulse/ui/thermal/ThermalViewModel.kt` | `@HiltViewModel`. Collects thermal state. Exposes `StateFlow<ThermalUiState>`. |
| 93 | `app/src/main/java/com/devicepulse/ui/thermal/ThermalDetailScreen.kt` | Shows battery temp, CPU temp (if available), thermal status with badge crossfade, throttling state. HeatStrip visualization. Pull-to-refresh. |

### 4.8 Storage Detail Screen

| # | File | Purpose |
|---|------|---------|
| 94 | `app/src/main/java/com/devicepulse/ui/storage/StorageUiState.kt` | Sealed interface: Loading, Success(storageState), Error. |
| 95 | `app/src/main/java/com/devicepulse/ui/storage/StorageViewModel.kt` | `@HiltViewModel`. Collects storage state. Exposes `StateFlow<StorageUiState>`. |
| 96 | `app/src/main/java/com/devicepulse/ui/storage/StorageDetailScreen.kt` | Shows total/used/available space, usage breakdown by category (visual bar chart or pie), fill rate estimate, SD card info. Pull-to-refresh. |

### 4.9 Settings Screen

| # | File | Purpose |
|---|------|---------|
| 97 | `app/src/main/java/com/devicepulse/ui/settings/SettingsUiState.kt` | Data class: themeMode, amoledBlack, dynamicColors, monitoringInterval, notificationsEnabled, deviceProfile. |
| 98 | `app/src/main/java/com/devicepulse/ui/settings/SettingsViewModel.kt` | `@HiltViewModel`. Reads/writes UserPreferencesRepository. Reads DeviceProfile for measurement info section. |
| 99 | `app/src/main/java/com/devicepulse/ui/settings/SettingsScreen.kt` | Theme selection (Light/Dark/System), AMOLED Black toggle (only when dark), Dynamic Colors toggle, monitoring interval selector, notification toggles, measurement info section showing DeviceProfile details, About section, Upgrade to Pro stub. |

---

## Phase 5: Background Service

| # | File | Purpose |
|---|------|---------|
| 100 | `app/src/main/java/com/devicepulse/service/monitor/HealthMonitorWorker.kt` | `CoroutineWorker` for WorkManager. Reads all four data sources once, writes readings to Room, runs `CleanupOldReadingsUseCase`. Scheduled at user-configured interval (15/30/60 min). |
| 101 | `app/src/main/java/com/devicepulse/service/monitor/MonitorScheduler.kt` | Utility to schedule/reschedule `HealthMonitorWorker` via WorkManager. Called at app startup and when interval setting changes. Uses `PeriodicWorkRequest`. |
| 102 | `app/src/main/java/com/devicepulse/service/monitor/RealTimeMonitorService.kt` | Foreground service started when user is viewing real-time data. Polls sensors every 2-3 seconds. Stops when user navigates away. Uses a notification channel. |
| 103 | `app/src/main/java/com/devicepulse/service/monitor/BootReceiver.kt` | `BroadcastReceiver` for `RECEIVE_BOOT_COMPLETED`. Re-schedules WorkManager periodic work after device reboot. |

---

## Phase 6: Unit Tests

| # | File | Purpose |
|---|------|---------|
| 104 | `app/src/test/java/com/devicepulse/domain/scoring/HealthScoreCalculatorTest.kt` | Tests health score algorithm: weighted average, boundary conditions (all 0, all 100, mixed), threshold transitions. |
| 105 | `app/src/test/java/com/devicepulse/domain/usecase/CalculateHealthScoreUseCaseTest.kt` | Tests use case wiring with fake repositories. Verifies flow emission. |
| 106 | `app/src/test/java/com/devicepulse/data/device/DeviceCapabilityManagerTest.kt` | Tests DeviceProfile validation: plausible current range, sign convention detection, thermal zone discovery. Uses mocked BatteryManager. |
| 107 | `app/src/test/java/com/devicepulse/data/battery/GenericBatterySourceTest.kt` | Tests current normalization, unit conversion, confidence assignment with mocked system APIs. |
| 108 | `app/src/test/java/com/devicepulse/domain/usecase/CleanupOldReadingsUseCaseTest.kt` | Tests retention logic: free tier deletes >24h, pro retains based on setting. |

---

## Implementation Order (Build Sequence)

The files above are numbered for reference. The actual build order groups them into logical coding sessions:

### Session 1: Project Setup (files 1-12)
Create the Gradle build files, manifest, resources, Application class, and MainActivity. **Goal:** project compiles and shows a blank Compose screen.

### Session 2: Domain Models & Enums (files 45-50)
Define all domain models and enums first since they have zero dependencies. **Goal:** all data structures the app works with are defined.

### Session 3: Room Database (files 13-24)
Create entities, DAOs, database class, converters. **Goal:** database compiles and can be tested.

### Session 4: Device Detection (files 25-27)
DeviceProfile, DeviceCapabilityManager, DeviceProfileRepository. **Goal:** can detect device capabilities.

### Session 5: Data Sources & Repositories (files 28-44)
Battery sources (interface, factory, implementations), network, thermal, storage data sources and repositories. Preferences and Pro status stub. **Goal:** all data flows are available.

### Session 6: Use Cases & Scoring (files 51-58)
Health score calculator and all use cases. **Goal:** business logic layer complete.

### Session 7: Dependency Injection (files 59-61)
Hilt modules wiring everything together. **Goal:** DI graph compiles.

### Session 8: Theme System (files 62-67)
Colors, typography, shapes, spacing, status colors, theme composable. **Goal:** app has proper Material You theming.

### Session 9: Shared Components (files 68-77)
All reusable composables: gauges, badges, tiles, charts, animations. **Goal:** component library ready.

### Session 10: Navigation (files 78-81)
Screen destinations, bottom nav bar, nav graph. **Goal:** can navigate between blank screens.

### Session 11: Dashboard (files 82-84)
Dashboard UI state, ViewModel, and screen composable. **Goal:** main screen shows health score and category cards.

### Session 12: Detail Screens (files 85-99)
Battery, Network, Thermal, Storage, and Settings screens with ViewModels. **Goal:** all screens functional.

### Session 13: Background Service (files 100-103)
WorkManager worker, scheduler, foreground service, boot receiver. **Goal:** periodic readings happen in background.

### Session 14: Tests (files 104-108)
Unit tests for scoring, use cases, device detection, battery source. **Goal:** core logic has test coverage.

---

## Key Design Decisions

1. **Single module** — per CLAUDE.md, no multi-module until necessary.
2. **DataStore for preferences, Room for readings** — lightweight for settings, structured for time-series data.
3. **Factory pattern for battery sources** — cleanly handles manufacturer-specific quirks without polluting the generic path.
4. **Confidence throughout the stack** — `MeasuredValue<T>` carries reliability info from data source all the way to UI badge.
5. **Foreground service only for real-time** — background periodic work uses WorkManager (battery-friendly), foreground service only when user is actively viewing live data.
6. **Pro gating via repository** — `ProStatusRepository.isProUser` is checked at the use case / UI level to show/hide features. Stubbed false for MVP.
7. **All strings in strings.xml** — per CLAUDE.md, no hardcoded strings for localization support.
8. **AMOLED Black as separate toggle** — not a third theme mode but an opt-in within dark mode, per spec.

---

## Total File Count

- Build & config: ~5 files
- Manifest & resources: ~5 files
- App entry points: 2 files
- Data layer: ~32 files
- Domain layer: ~14 files
- DI: 3 files
- UI: ~38 files
- Service: 4 files
- Tests: 5 files

**Total: ~108 files**
