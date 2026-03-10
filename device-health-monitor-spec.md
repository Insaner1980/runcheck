# DevicePulse — Android Device Health Monitor

## Project Specification v1.0

---

## 1. Overview

**DevicePulse** is a comprehensive Android device health monitoring app that provides real-time diagnostics across four key areas: battery, network, thermal performance, and storage. Unlike existing apps that focus on a single metric (e.g., Ampere for charging current, AccuBattery for battery health), DevicePulse combines all critical device health data into one clean, modern interface with long-term trend tracking.

### Core Differentiators

- **Unified health dashboard** — battery, network, thermal, and storage in one app
- **Adaptive device detection** — automatically identifies manufacturer, model, and API level to select the most accurate measurement methods per device
- **Measurement confidence indicator** — transparently shows users when a reading may be unreliable instead of displaying potentially inaccurate data without warning
- **Material You design** — dynamic color theming, modern UI, light/dark themes
- **Offline-first & privacy-focused** — no account required, no data leaves the device

### Target Platform

- Android only (no iOS — most metrics are unavailable on iOS)
- Minimum API level: 26 (Android 8.0)
- Target API level: 35 (Android 15)
- Enhanced features for API 34+ (Android 14+: battery cycle count, health percentage)

### Technology

- **Kotlin** (native Android) — chosen over Flutter/React Native for direct, unmediated access to Android system APIs (BatteryManager, ConnectivityManager, ThermalManager, StorageStatsManager). No plugin layers, no platform channel overhead, maximum reliability for sensor data.
- **Jetpack Compose** — for Material You / Material 3 UI
- **Room** — local database for historical data
- **Kotlin Coroutines + Flow** — reactive data streams from sensors

---

## 2. Architecture

### High-Level Structure

```
app/
├── data/
│   ├── battery/          # BatteryManager, sysfs readers
│   ├── network/          # ConnectivityManager, TelephonyManager
│   ├── thermal/          # ThermalManager, CPU temp readers
│   ├── storage/          # StorageStatsManager, disk benchmarks
│   ├── device/           # Device detection & capability mapping
│   └── db/               # Room database, DAOs, entities
├── domain/
│   ├── models/           # Domain models for each health category
│   ├── usecases/         # Business logic (trend calculation, scoring)
│   └── scoring/          # Health score algorithm
├── ui/
│   ├── dashboard/        # Main dashboard screen
│   ├── battery/          # Battery detail screen
│   ├── network/          # Network detail screen
│   ├── thermal/          # Thermal detail screen
│   ├── storage/          # Storage detail screen
│   ├── settings/         # Settings screen
│   ├── theme/            # Material You theming
│   └── components/       # Shared UI components (gauges, charts, cards)
└── service/
    └── monitor/          # Background monitoring service
```

### Device Detection System

The app uses a `DeviceCapabilityManager` that runs at first launch and determines:

1. **Manufacturer** (`Build.MANUFACTURER`) — Samsung, OnePlus, Google, Xiaomi, etc.
2. **Model** (`Build.MODEL`) — specific device model
3. **API level** (`Build.VERSION.SDK_INT`) — determines available APIs
4. **Available sysfs paths** — probes `/sys/class/power_supply/battery/` for available nodes
5. **Measurement validation** — reads `CURRENT_NOW` multiple times and checks:
   - Is the value non-zero?
   - Does it change over time (not a static dummy value)?
   - Is it within a plausible range (e.g., -10000 to +10000 mA)?
   - Is the sign convention correct for current charge state?

Results are stored as a `DeviceProfile` that the app uses throughout its lifecycle to decide which data sources to use and which to mark as unreliable.

```kotlin
data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val apiLevel: Int,
    val currentNowReliable: Boolean,
    val currentNowUnit: CurrentUnit, // MICROAMPS or MILLIAMPS
    val currentNowSignConvention: SignConvention, // POSITIVE_CHARGING or NEGATIVE_CHARGING
    val cycleCountAvailable: Boolean,
    val batteryHealthPercentAvailable: Boolean,
    val thermalZonesAvailable: List<String>,
    val storageHealthAvailable: Boolean
)
```

### Manufacturer-Specific Adapters

```kotlin
interface BatteryDataSource {
    fun getCurrentNow(): Flow<MeasuredValue<Int>> // value + confidence
    fun getVoltage(): Flow<Int>
    fun getTemperature(): Flow<Float>
    fun getHealth(): Flow<BatteryHealth>
    fun getCycleCount(): Flow<Int?>
    fun getCapacity(): Flow<Int?>
}

// Factory selects the best implementation based on DeviceProfile
class BatteryDataSourceFactory(private val profile: DeviceProfile) {
    fun create(): BatteryDataSource = when {
        profile.apiLevel >= 34 -> Android14BatterySource(profile)
        profile.manufacturer == "samsung" -> SamsungBatterySource(profile)
        profile.manufacturer == "oneplus" -> OnePlusBatterySource(profile)
        else -> GenericBatterySource(profile)
    }
}
```

---

## 3. Features — Free Version

### 3.1 Dashboard (Home Screen)

The main screen displays an overall **Device Health Score** (0–100) with a large circular gauge, plus four summary cards for each category:

- **Battery** — current level %, health status, charging state
- **Network** — signal strength, connection type (WiFi/5G/4G), latency
- **Thermal** — battery temperature, CPU temperature (if available)
- **Storage** — used/total space, usage percentage

Each card shows a color-coded status: green (good), yellow (fair), red (poor).

Tapping a card navigates to the detailed view for that category.

#### Health Score Algorithm

The overall score is a weighted average:

| Category | Weight | Scoring Criteria |
|----------|--------|-----------------|
| Battery | 35% | Health %, temperature, voltage stability |
| Network | 20% | Signal strength (dBm), latency, stability |
| Thermal | 25% | Temperature vs. safe thresholds, throttling state |
| Storage | 20% | Available space %, estimated fill rate |

### 3.2 Battery Detail Screen

**Real-time metrics (always visible):**

- Battery level (%)
- Voltage (mV)
- Temperature (°C)
- Charging status (Charging / Discharging / Full / Not Charging)
- Charging type (AC / USB / Wireless)
- Battery health (Good / Degraded / etc.)
- Battery technology (Li-ion, Li-poly, etc.)

**Charging current (mA) — with confidence indicator:**

- If `DeviceProfile.currentNowReliable == true`: shows real-time mA with a green confidence badge
- If unreliable: shows "Not available on this device" or "Estimated" with an info tooltip explaining why

**Conditional metrics (API 34+ / device-dependent):**

- Battery health percentage (e.g., "92% of original capacity")
- Charge cycle count
- Estimated remaining charge time

**24-hour mini chart** showing battery level over the last day (free tier limit).

### 3.3 Network Detail Screen

**Real-time metrics:**

- Connection type (WiFi / 4G LTE / 5G / None)
- Signal strength in dBm (with visual bar indicator)
- WiFi: SSID, link speed (Mbps), frequency band (2.4/5/6 GHz)
- Mobile: carrier name, network type, cell ID
- Approximate latency (measured via ping to configurable endpoint)

**Signal quality rating** — translated from raw dBm to a human-readable scale (Excellent / Good / Fair / Poor / No Signal)

### 3.4 Thermal Detail Screen

**Real-time metrics:**

- Battery temperature (°C) — always available
- CPU thermal zone temperatures — device-dependent, read from `/sys/class/thermal/`
- Thermal status (from `PowerManager.getThermalStatus()` on API 29+):
  - None → Light → Moderate → Severe → Critical → Emergency → Shutdown
- Current throttling state (if detectable)

**Visual heat indicator** — color gradient from cool blue to hot red based on temperature.

### 3.5 Storage Detail Screen

**Metrics:**

- Total internal storage
- Used / available space
- Usage breakdown by category (Apps, Images, Videos, Audio, Documents, Other)
- SD card info (if present)

**Fill rate estimate** — based on storage change over time: "At current rate, storage will be full in ~X months"

### 3.6 Settings

- **Theme** — Light / Dark / System default (follows Android system setting)
- **Dynamic colors** — On/Off (Material You color extraction from wallpaper)
- **Monitoring interval** — how often background service records data (15 min / 30 min / 1 hour)
- **Notifications** — toggle alerts for low battery, high temperature, low storage
- **Measurement info** — shows DeviceProfile details; which metrics are available and reliable on this device
- **About** — app version, licenses, links
- **Upgrade to Pro** — feature comparison and purchase

### 3.7 General UI/UX

- **Material You / Material 3** throughout — dynamic color, rounded shapes, elevation system
- **Bottom navigation** — 4 tabs: Dashboard, Battery, Network, More (Thermal + Storage + Settings)
- **Pull-to-refresh** on all detail screens for instant re-measurement
- **Smooth animations** — gauge fill animations, card transitions, chart drawing
- **Adaptive layout** — works on phones and tablets

---

## 4. Features — Pro Version

**Target price: €3.49 one-time purchase** (positioned between AccuBattery's "buy us coffee" tier and a full utility app price; no subscription)

### 4.1 Extended History & Trends

- Full historical data retention (free version: 24 hours only)
- **Weekly / Monthly / All-time** trend charts for all metrics
- Battery health degradation over months (the killer feature for long-term users)
- Network quality patterns by time of day and location
- Storage growth trends with fill-date projection

### 4.2 Charger & Cable Comparison Tool

- Name and save different chargers/cables ("Samsung 25W", "IKEA USB-C", etc.)
- Automated charging session recording: current, voltage, power (W), time-to-full
- Side-by-side comparison view of charger performance
- Identify underperforming cables with clear visual indicators

### 4.3 Per-App Battery Consumption

- Track which apps consume the most battery (using foreground app detection + current draw correlation)
- Daily/weekly app battery usage rankings
- Identify battery-draining apps with abnormal consumption patterns

### 4.4 Thermal Throttling Log

- Record when CPU throttling occurs and what triggered it (gaming, camera, charging)
- Correlate temperature spikes with app usage
- Temperature timeline with event markers

### 4.5 Home Screen Widgets

- **Battery widget** (2x1) — level, temperature, charging current
- **Health score widget** (2x2) — overall score with four mini-indicators
- **Network widget** (2x1) — signal strength, type, latency

### 4.6 Enhanced Notifications

- Detailed real-time battery stats in persistent notification (current mA, voltage, temperature)
- Customizable alert thresholds (e.g., "alert when temperature exceeds 40°C")
- Charging complete notification with session summary

### 4.7 Data Export

- Export historical data as CSV
- Shareable device health report (summary PDF or text)

### 4.8 No Ads

- Free version displays a small, non-intrusive banner ad on detail screens (not on dashboard)
- Pro removes all ads

---

## 5. Data Model

### Room Database Schema

```
-- Core tables
devices (
    id TEXT PRIMARY KEY,
    manufacturer TEXT,
    model TEXT,
    api_level INTEGER,
    first_seen INTEGER,
    profile_json TEXT
)

battery_readings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER,
    level INTEGER,           -- 0-100
    voltage_mv INTEGER,
    temperature_c REAL,
    current_ma INTEGER,      -- nullable if unreliable
    current_confidence TEXT,  -- HIGH / LOW / UNAVAILABLE
    status TEXT,             -- CHARGING / DISCHARGING / FULL / NOT_CHARGING
    plug_type TEXT,          -- AC / USB / WIRELESS / NONE
    health TEXT,
    cycle_count INTEGER,     -- nullable
    health_pct INTEGER       -- nullable, API 34+
)

network_readings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER,
    type TEXT,               -- WIFI / CELLULAR / NONE
    signal_dbm INTEGER,
    wifi_speed_mbps INTEGER,
    wifi_frequency INTEGER,
    carrier TEXT,
    network_subtype TEXT,    -- LTE / NR (5G) / etc.
    latency_ms INTEGER
)

thermal_readings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER,
    battery_temp_c REAL,
    cpu_temp_c REAL,         -- nullable
    thermal_status INTEGER,  -- 0-6 (NONE to SHUTDOWN)
    throttling BOOLEAN
)

storage_readings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER,
    total_bytes INTEGER,
    available_bytes INTEGER,
    apps_bytes INTEGER,
    media_bytes INTEGER
)

-- Pro feature tables
charger_profiles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    created INTEGER
)

charging_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    charger_id INTEGER REFERENCES charger_profiles(id),
    start_time INTEGER,
    end_time INTEGER,
    start_level INTEGER,
    end_level INTEGER,
    avg_current_ma INTEGER,
    max_current_ma INTEGER,
    avg_voltage_mv INTEGER,
    avg_power_mw INTEGER,
    plug_type TEXT
)
```

### Data Retention Policy

- **Free:** 24 hours of readings, unlimited current session
- **Pro:** unlimited retention, with optional auto-cleanup setting (3 months / 6 months / 1 year / forever)
- Readings are taken at user-configured intervals (default: 30 minutes)
- Real-time display updates every 2-3 seconds when the screen is open

---

## 6. Permissions

```xml
<!-- Required -->
<uses-permission android:name="android.permission.BATTERY_STATS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Optional — for latency measurement -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Optional — for network cell info -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />

<!-- Optional — for per-app storage breakdown -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
```

Note: The app should work without optional permissions, gracefully hiding features that require them. Request permissions contextually (when user navigates to a feature that needs them), not at launch.

---

## 7. UI Design Guidelines

### Design System

- **Material 3 / Material You** with `DynamicColors` from the user's wallpaper
- **Typography:** System default (Roboto) following M3 type scale; monospace (Roboto Mono) for real-time numeric values to prevent layout jitter during updates
- **Shape:** Rounded corners (M3 default shape system — medium for cards, full for buttons/FABs)
- **Elevation:** Subtle, using M3 tonal elevation (color-based, not shadow-based)
- **Minimum touch target:** 48dp on all interactive elements

### Theme Modes

The app provides three theme options:

1. **Light** — clean white surfaces, optimal for daylight use
2. **Dark** (default in dark mode) — dark gray surfaces, best balance of readability and comfort
3. **AMOLED Black** (opt-in toggle in settings) — pure black surfaces for maximum OLED battery saving

The user selects between Light / Dark / System (follows Android setting). AMOLED Black is a sub-option within Dark mode, toggled separately.

### Color Palette

#### Fallback Colors (when DynamicColors unavailable, i.e., below Android 12)

| Token | Light | Dark | AMOLED Black |
|-------|-------|------|-------------|
| Primary | #0D9488 (teal 600) | #5EEAD4 (teal 300) | #5EEAD4 |
| On Primary | #FFFFFF | #003731 | #003731 |
| Primary Container | #B2F5EA | #1A3B36 | #0F2623 |
| Surface | #FAFAFA | #121212 | #000000 |
| Surface Container | #F0F0F0 | #1E1E1E | #0A0A0A |
| Surface Container High | #E8E8E8 | #2A2A2A | #141414 |
| On Surface | #1A1A1A | #E0E0E0 | #E0E0E0 |
| On Surface Variant | #5F5F5F | #ABABAB | #ABABAB |
| Outline | #C4C4C4 | #3D3D3D | #2A2A2A |

Key rules:
- **Never use pure white (#FFFFFF) text on dark backgrounds.** Use #E0E0E0 for primary text, #ABABAB for secondary text. Pure white causes excessive contrast and halation (glow effect), especially for users with astigmatism.
- **Dark theme uses #121212, not pure black.** This prevents OLED smearing during fast scrolling and allows for tonal elevation (lighter shades for elevated surfaces).
- **AMOLED Black uses #000000 surface** for maximum battery saving, but cards/sheets use #0A0A0A to maintain visual hierarchy.
- Contrast ratio must meet **WCAG AA minimum: 4.5:1** for body text, **3:1** for large text and UI elements. Target **5:1+** for critical reading content.

#### Semantic Status Colors

Used for health indicators, gauges, and status badges. Same hues in all themes, adjusted for lightness:

| Status | Light Theme | Dark / AMOLED Theme | Usage |
|--------|------------|-------------------|-------|
| Healthy / Good | #16A34A | #4ADE80 | Score 75-100, normal temps, good signal |
| Fair / Attention | #D97706 | #FBBF24 | Score 50-74, warm temps, fair signal |
| Poor / Warning | #EA580C | #FB923C | Score 25-49, hot temps, poor signal |
| Critical / Danger | #DC2626 | #F87171 | Score 0-24, overheating, no signal |
| Neutral / Info | #2563EB | #60A5FA | Informational, links, non-status data |
| Unavailable | #737373 | #737373 | Disabled metrics, unavailable features |

These colors are always paired with icons or text labels — never color alone — for accessibility (color blindness).

#### Confidence Badge Colors

| Badge | Light | Dark | Label |
|-------|-------|------|-------|
| Accurate | #16A34A bg, #FFFFFF text | #065F46 bg, #6EE7B7 text | "Accurate" |
| Estimated | #D97706 bg, #FFFFFF text | #78350F bg, #FDE68A text | "Estimated" |
| Unavailable | #D4D4D4 bg, #525252 text | #404040 bg, #A3A3A3 text | "N/A" |

### Spacing System

Based on a 4dp grid, consistent throughout the app:

| Token | Value | Usage |
|-------|-------|-------|
| `space-xs` | 4dp | Tight internal padding (badge padding, icon-to-label gap) |
| `space-sm` | 8dp | Inner element padding (chip padding, small gaps) |
| `space-md` | 12dp | Gap between cards in a list, compact section spacing |
| `space-base` | 16dp | Standard content padding, card inner padding, screen horizontal margins |
| `space-lg` | 24dp | Section dividers, space between dashboard gauge and cards |
| `space-xl` | 32dp | Major section breaks, top/bottom screen padding |

#### Layout Dimensions

| Element | Dimension |
|---------|-----------|
| Screen horizontal padding | 16dp |
| Card inner padding | 16dp |
| Card corner radius | 16dp (M3 medium) |
| Card gap (vertical list) | 12dp |
| Dashboard health gauge diameter | 200dp |
| Battery detail gauge diameter | 160dp |
| Bottom navigation height | 80dp (M3 standard) |
| Metric tile min height | 72dp |
| Sparkline chart height (inline) | 40dp |
| Trend chart height (detail screen) | 200dp |
| Confidence badge height | 24dp, corner radius full |
| Status bar padding top | system inset + 8dp |

### Typography Scale

| Style | Font | Size | Weight | Usage |
|-------|------|------|--------|-------|
| Display Large | Roboto | 57sp | 400 | Health score number in dashboard gauge |
| Headline Medium | Roboto | 28sp | 400 | Screen titles |
| Title Medium | Roboto | 16sp | 500 | Card titles, section headers |
| Body Large | Roboto Mono | 18sp | 400 | Real-time metric values (mA, mV, °C) |
| Body Medium | Roboto | 14sp | 400 | Metric labels, descriptions |
| Body Small | Roboto | 12sp | 400 | Timestamps, secondary info |
| Label Medium | Roboto | 12sp | 500 | Confidence badges, chip labels |
| Label Small | Roboto | 11sp | 500 | Chart axis labels |

Note: Roboto Mono for numeric values prevents layout jitter when digits change rapidly in real-time displays.

### Animations

#### Gauge Animations

- **Health score arc fill:** On screen load, arc animates from 0° to target angle over **800ms** using `spring(dampingRatio = 0.7, stiffness = 200)`. The spring slightly overshoots and settles, giving an organic feel.
- **Gauge color transitions:** When score crosses a threshold (e.g., 74→75, fair→good), the arc color cross-fades over **300ms** with `tween(easing = FastOutSlowIn)`.
- **Real-time current gauge:** Needle/arc updates with `animateFloatAsState` using **spring(dampingRatio = 0.8)** for smooth tracking of fluctuating values. No hard jumps.

#### Number Animations

- **Real-time metric values** (mA, mV, °C, %): Animate between old and new values using a rolling counter effect over **200ms**. The number smoothly counts up or down rather than snapping. Implemented via `animateIntAsState` or custom `AnimatedContent` with vertical slide transition.
- **Battery percentage:** Animates on first display, static on subsequent updates (to avoid distraction).

#### Card & Screen Transitions

- **Dashboard cards entrance:** Staggered fade-in + slide-up animation on first load. Each card delayed by **60ms** from the previous one. Duration **300ms** per card, easing `FastOutSlowIn`. Total entrance sequence ~500ms for 4 cards.
- **Screen navigation (bottom nav):** Material 3 shared axis transition — horizontal slide with fade. Duration **300ms**, easing `FastOutSlowIn`.
- **Detail screen entry (from card tap):** Container transform or shared element transition where the card expands into the full detail screen. Duration **350ms**.
- **Pull-to-refresh:** Custom rotating pulse animation on the indicator, suggesting active measurement. Smooth spring return on release.

#### Chart Animations

- **Sparkline (dashboard cards):** Path draws left-to-right on card entrance over **600ms** with `tween(easing = LinearEasing)`. Feels like a monitor drawing a trace.
- **Trend chart (detail screens):** Line draws in with gradient fill fading in simultaneously over **800ms**. Data points pop in with slight scale animation (0→1) staggered along the line.
- **Touch-to-inspect:** Data point scales up to 1.3x with a subtle bounce, tooltip fades in over **150ms**.

#### Thermal Visualization

- **Temperature gradient strip:** Colors shift smoothly as temperature changes. When in critical range (>42°C), the strip has a subtle **pulsing glow** animation — opacity oscillates between 0.7 and 1.0 over **2 seconds**, `tween(easing = LinearEasing, repeatMode = Reverse)`. Draws attention without being annoying.
- **Thermal status badges:** Crossfade between states (None → Light → Moderate etc.) over **300ms**.

#### Micro-interactions

- **Card press feedback:** Subtle scale down to 0.97x on press, spring back on release. Duration **100ms** down, **200ms** spring return.
- **Toggle switches:** Thumb slides with spring physics, track color cross-fades over **200ms**.
- **Confidence badge appear:** Scale from 0 to 1 with `spring(dampingRatio = 0.6)` on first display — slight bounce makes it noticeable.

#### Performance Rules

- All animations use Compose `animate*AsState` or `Animatable` — never block the main thread.
- Disable or reduce animations when `AccessibilityManager.isReducedMotionEnabled` is true (respect system "Remove animations" setting).
- Real-time gauge updates throttled to max **3 updates/second** to prevent jank. Sensor reads can be faster; display updates are debounced.
- Chart animations only play on first appearance, not on re-composition.

### Key Visual Components

1. **Circular health gauge** (dashboard) — animated arc with score number, color transitions from red→yellow→green
2. **Mini sparkline charts** — inline trend indicators on summary cards
3. **Real-time gauge** (battery current) — animated needle/arc gauge with min/max markers
4. **Confidence badges** — small colored pills: green "Accurate", yellow "Estimated", gray "Unavailable"
5. **Trend charts** (Pro) — line charts with gradient fills, touch-to-inspect data points
6. **Heat map visualization** (thermal) — color gradient strip showing temperature zones
7. **Metric tiles** — compact cards showing value + label + optional sparkline, arranged in responsive grid

### Navigation

```
Bottom Navigation (4 items):
├── Dashboard    (home icon)
├── Battery      (battery icon)
├── Network      (signal icon)
└── More
    ├── Thermal
    ├── Storage
    ├── Settings (includes theme toggle, AMOLED Black option)
    └── About / Pro
```

---

## 8. Monetization Strategy

### Pricing

| | Free | Pro (€3.49 one-time) |
|--|------|---------------------|
| Real-time metrics (all categories) | ✓ | ✓ |
| Device auto-detection | ✓ | ✓ |
| Confidence indicators | ✓ | ✓ |
| Light/Dark theme | ✓ | ✓ |
| Dynamic colors (Material You) | ✓ | ✓ |
| 24-hour history | ✓ | ✓ |
| Extended history & trends | — | ✓ |
| Charger/cable comparison | — | ✓ |
| Per-app battery analysis | — | ✓ |
| Thermal throttling log | — | ✓ |
| Home screen widgets | — | ✓ |
| Enhanced notifications | — | ✓ |
| CSV data export | — | ✓ |
| Ad-free | — | ✓ |

### Why One-Time Purchase (No Subscription)

- Device diagnostics apps have low recurring costs (no server, no API calls)
- Users strongly prefer one-time payments for utility apps in this category
- AccuBattery and Ampere both use one-time purchase models successfully
- Builds trust and goodwill — important for an app that asks for system-level access

### Ad Strategy (Free Version)

- Small banner ad at the bottom of detail screens only
- No ads on the dashboard (first impression matters)
- No interstitial/fullscreen ads ever
- No ads during active measurement (annoying and affects readings)

---

## 9. Play Store Positioning

### App Name Options

1. **DevicePulse — Phone Health Monitor**
2. **DevicePulse — Battery, Network & Thermal Check**

### Category

Tools (primary), optionally also listed under Device Care

### Key ASO Keywords

`battery health`, `charging speed`, `phone diagnostics`, `device health`, `battery monitor`, `thermal throttling`, `signal strength`, `storage check`, `charger test`, `battery current mA`

### Competitive Positioning

| Competitor | Focus | DevicePulse Advantage |
|-----------|-------|----------------------|
| Ampere | Charging current only | Full device health + honesty about accuracy |
| AccuBattery | Battery health + wear | Adds network, thermal, storage monitoring |
| CPU-Z / AIDA64 | Raw specs dump | User-friendly presentation + trends |
| Samsung Members | Samsung only | Universal, works on all Android devices |
| DevInfo | Static device info | Real-time monitoring + historical trends |

### Unique Selling Points for Store Listing

1. "The only app that tells you when a measurement ISN'T reliable"
2. "Battery + Network + Thermal + Storage — one app, full picture"
3. "See how your battery health changes over months, not just today"
4. "Test and compare your chargers and cables with real data"
5. "Material You design — beautiful and useful"

---

## 10. Development Phases

### Phase 1 — MVP (Free Version Core)

- Device detection system + DeviceProfile
- Battery detail screen with real-time metrics + confidence indicators
- Dashboard with health score
- Network detail screen (basic: type, signal, speed)
- Thermal screen (battery temp + thermal status)
- Storage screen (used/available)
- Light/dark theme + Material You dynamic colors
- Settings screen
- Room database + 24-hour history
- Background monitoring service

### Phase 2 — Polish & Pro Foundation

- Sparkline charts on dashboard cards
- Smooth gauge animations
- Pull-to-refresh on all screens
- Ad integration (free version)
- In-app purchase setup
- Pro: extended history + trend charts
- Pro: home screen widgets

### Phase 3 — Pro Features

- Charger/cable comparison tool
- Per-app battery consumption tracking
- Thermal throttling log with event correlation
- Enhanced notifications
- CSV export

### Phase 4 — Launch & Iterate

- Play Store listing + ASO optimization
- Closed beta testing
- Public launch
- Collect device compatibility reports from users
- Expand DeviceProfile database based on real-world data
- Localization (English + Finnish as first languages)

---

## 11. Technical Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| `CURRENT_NOW` unreliable on many devices | Core feature degraded | Confidence indicator system; never show bad data silently |
| Thermal zone paths vary wildly across devices | Missing CPU temp data | Probe multiple known paths; graceful fallback to battery temp only |
| Background service killed by aggressive OEMs (Xiaomi, Huawei, Samsung) | Gaps in historical data | Guide users to disable battery optimization; detect and warn about gaps |
| `PACKAGE_USAGE_STATS` permission requires special user action | Per-app tracking fails | Contextual permission request with clear explanation; feature degrades gracefully |
| Rapid sensor polling drains battery | Ironic for a battery app | Configurable interval; smart polling (faster when screen on, slower in background) |
| Play Store policy on battery/system info apps | Rejection risk | Follow all permission guidelines; no misleading claims; clear disclaimer |

---

## 12. Success Metrics

- **Install-to-active ratio** > 60% (day 7 retention)
- **Free-to-Pro conversion** > 3-5% (typical for well-designed utility apps)
- **Play Store rating** > 4.3 stars
- **Device compatibility** > 90% of devices show all basic metrics correctly
- **Crash-free rate** > 99.5%

---

## Appendix A: Android API Reference

| Data Point | API / Source | Min API |
|-----------|-------------|---------|
| Battery level, voltage, temp, health | `BatteryManager` + `ACTION_BATTERY_CHANGED` | 21 |
| Charging current (mA) | `BatteryManager.BATTERY_PROPERTY_CURRENT_NOW` | 21 |
| Average current | `BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE` | 21 |
| Remaining capacity (µAh) | `BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER` | 21 |
| Charge cycle count | `BatteryManager.BATTERY_PROPERTY_CHARGING_CYCLE_COUNT` | 34 |
| Battery health % | `BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH` | 34 |
| WiFi info | `WifiManager.getConnectionInfo()` | 21 |
| Cell signal strength | `TelephonyManager` + `SignalStrength` | 21 |
| Network type (5G/LTE) | `TelephonyManager.getDataNetworkType()` | 24 |
| Thermal status | `PowerManager.getCurrentThermalStatus()` | 29 |
| CPU temperatures | `/sys/class/thermal/thermal_zone*/temp` | — (sysfs) |
| Storage stats | `StorageStatsManager` | 26 |
| Per-app storage | `StorageStatsManager.queryStatsForPackage()` | 26 |
