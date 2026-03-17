# runcheck — Project Overview

Native Android device health monitor. Kotlin + Jetpack Compose. Single dark theme.

---

## Screens & Navigation

Push-based navigation from single Home screen. No bottom nav bar, no tabs.

```
Home
├── Battery Detail
│   └── Charger Comparison [PRO]
├── Network Detail
│   └── Speed Test
├── Thermal Detail
├── Storage Detail
│   └── Cleanup (Large Files / Old Downloads / APK Files)
├── App Usage [PRO]
├── Settings
└── Pro Upgrade
```

---

## Home Screen

### Health Score Card
- Overall score 0–100 displayed in animated ProgressRing (AccentTeal)
- Status label: Healthy / Fair / Poor / Critical
- Description: "Your device is in [status] shape"
- Temperature warning if ≥ 38°C
- Breakdown rows with StatusDot per category:
  - Battery (40% weight)
  - Thermal (25% weight)
  - Network (25% weight)
  - Storage (10% weight)

### Battery Hero Card
- Large battery level (54sp, JetBrains Mono)
- Charging status text
- Animated battery icon with wave animation during charge (2000ms tween, respects reducedMotion)
- MetricPills: Health status (color-coded), Plug Type

### Quick Info Grid (2×2)
| Card | Content |
|------|---------|
| Network | Signal quality label + SignalBars + color indicator |
| Thermal | Battery temp °C + band label (Cool/Normal/Warm/Hot/Critical) |
| Charger | "Test & Compare" — Pro-locked in free tier |
| Storage | Free space + usage % with color |

### Quick Tools
- Speed Test — always available, SpeedGauge icon
- App Usage — Pro-locked, battery usage icon

### Pro Status Cards
- Active trial: days remaining + upgrade prompt
- Trial expired: upgrade card with border
- Pro purchased: "Insights" card with star icon

---

## Battery Detail Screen

### Hero Section
- ProgressRing (152dp) with battery level %
- Status text: "Good · Discharging" (health + charging status)
- mAh remaining (from CHARGE_COUNTER, if available): "Good · Discharging · ~2700 mAh remaining"
- MetricPills row: Drain rate (%/h), Power (W), Remaining time

### Details Panel
| Metric | Source |
|--------|--------|
| Voltage | V (converted from mV) |
| Temperature | °C with color (green < 35, yellow 35-40, red > 40) |
| Health | Good / Overheat / Dead / Over Voltage / Cold / Unknown |
| Technology | Li-ion etc. or "Not available" |
| Cycle Count | API 34+ or null |
| Health % | API 34+ or null |
| Capacity | Estimated / Design mAh (PowerProfile reflection + healthPct) |

### Current / Charging Panel
- Current reading (mA) with ConfidenceBadge (Accurate / Estimated / Unavailable)
- W + mV line under current: "0.9 W · 4125 mV"
- Charging session summary (during charge): start level, gain %, duration, peak temp, speeds, delivered mAh, avg power
- Status/Type MetricPills
- Current stats (in-memory, resets on status change): Average / Minimum / Maximum mA

### Screen On/Off Panel (discharge only)
- Two columns: Screen On vs Screen Off
- Drain rate %/h per state
- Duration per state
- Data from ScreenStateTracker (BroadcastReceiver: ACTION_SCREEN_ON/OFF)

### Sleep Analysis Panel (discharge only)
- Deep Sleep duration (PowerManager.isDeviceIdleMode = true)
- Held Awake duration (isDeviceIdleMode = false while screen off)
- Color: green for deep sleep, yellow for held awake if > deep sleep

### Charger Comparison CTA
- Button navigating to ChargerComparisonScreen

### Session Graph (during charge)
- Metrics: Current (mA) / Power (W)
- Windows: 15m / 30m / All
- TrendChart with downsampling (max 240 points)

### Remaining Charge Time (during charge, Pro)
- To 80% and To 100% estimates based on charge rate

### History Panel (Pro)
- Periods: Since Unplug / 24h / Week / Month / All
- Metrics: Level / Temperature / Current / Voltage
- TrendChart with downsampling (max 300 points)
- "Since Unplug" queries last CHARGING timestamp from Room

### Statistics Panel (Pro)
- Period: last 10 days (from Room)
- Charged / Discharged total %
- Charge sessions count
- Average drain rate %/h
- Full charge estimate (hours)

---

## Network Detail Screen

### Hero Section
- SignalBars (5-bar visual, color by quality)
- Quality label: Excellent / Good / Fair / Poor / No Signal
- dBm value (if available)
- MetricPills: Latency (ms), Speed/Bandwidth (Mbps), Frequency (GHz) or Subtype

### WiFi Name Help Card
- Shown when SSID unknown due to missing location permission
- "Grant Permission" / "Enable Location" / "Open Settings"

### Connection Details Card
**WiFi:** SSID, BSSID (copyable), WiFi Standard, Frequency, Link Speed
**Cellular:** Carrier, Subtype (4G/5G/LTE), Roaming
**Both:** Downstream/Upstream bandwidth, Metered, VPN

### IP & DNS Section
- IPv4 (copyable)
- IPv6 (copyable, truncated with ellipsis)
- DNS 1 & 2 (copyable)
- MTU

### Signal History (Pro)
- Metrics: Signal Strength (dBm) / Latency (ms)
- Periods: 24h / Week / Month / All
- TrendChart

### Speed Test Summary
- Last result: Download / Upload / Ping / Jitter / Server / Time
- "Run Speed Test" button

---

## Speed Test Screen

### States
1. **Ready:** Animated radar icon, "Start Test" button
2. **Cellular warning dialog:** "Speed test on cellular may use data" → Proceed / Cancel
3. **Testing:** Phase indicator (Download/Upload/Ping), progress %, real-time speed
4. **Results:** Download / Upload / Ping / Jitter / Server / "Run Again"
5. **History:** Past results list

---

## Thermal Detail Screen

### Hero Card
- Canvas thermometer (40×120dp) with animated fill (1200ms FastOutSlowIn)
- Large temperature (48sp, JetBrains Mono)
- Band label with color: Cool / Normal / Warm / Hot / Critical
- Session min/max range (annotated string): "↓ 27.3°C · ↑ 35.3°C" (color-coded per temp)

### Heat Strip
- Color gradient visualization (cool → hot) with position indicator

### Metrics Grid
| Metric | Detail |
|--------|--------|
| CPU Temperature | °C or "Unavailable" |
| Thermal Headroom | % (inverted) |
| Thermal Status | Normal / Light / Moderate / Severe / Critical / Emergency / Shutdown |
| Throttling | Active / None |

### Throttling Log (Pro)
- Event list: timestamp, status level, battery temp, CPU temp, foreground app, duration
- Empty state: "No thermal throttling events" with healthy indicator

---

## Storage Detail Screen

### Hero Card
- ProgressRing (152dp) with usage %
- Used / Total (JetBrains Mono, center)
- Free space + fill rate estimate text
- MetricPills: Cache total, Fill Rate, Available

### Media Breakdown Card
- SegmentedBar (Canvas, 12dp, 800ms ease-out, 2dp gaps)
- 6 categories with accent colors:
  - Images (Teal), Videos (Blue), Audio (Orange)
  - Documents (Lime), Downloads (Yellow), Other (Muted)
- SegmentedBarLegend with StatusDot + label + size per row

### Cleanup Tools Section
ActionCards with colored IconCircles:
| Tool | Icon Color | Action |
|------|-----------|--------|
| Large Files | Orange | → CleanupScreen(LARGE_FILES) |
| Old Downloads | Blue | → CleanupScreen(OLD_DOWNLOADS) |
| APK Files | Lime | → CleanupScreen(APK_FILES) |
| Trash (API 30+) | Red | Empty trash (createDeleteRequest) |

### Details Card
- Total / Used (%) / Available / Apps / Cache (across N apps)

### SD Card Card (if present)
- Total / Available

### Quick Actions Card
- Storage Settings (ACTION_INTERNAL_STORAGE_SETTINGS)
- Free Up Space (ACTION_MANAGE_STORAGE)
- Usage Access (ACTION_USAGE_ACCESS_SETTINGS)

---

## Cleanup Screen (Shared)

Single reusable screen for all cleanup types. Navigated via `cleanup/{type}` route.

### Filter Chips
| Type | Options | Default |
|------|---------|---------|
| LARGE_FILES | 10 MB / 50 MB / 100 MB / 500 MB | 50 MB |
| OLD_DOWNLOADS | 30d / 60d / 90d / 1y | 30d |
| APK_FILES | (none) | — |

### States
1. **Scanning:** CircularProgressIndicator
2. **Empty:** "No files found · Your storage is looking clean!"
3. **Results:** Grouped file list (see below)
4. **Deleting:** Progress indicator
5. **Success:** Overlay with CheckCircle (AccentTeal, 64dp) + "X GB freed" (titleLarge, fade in/out)

### Results View
- Summary: "Found N files · X GB"
- File groups (collapsible, sorted by total size descending):
  - Group header: StatusDot + category label + count + total size + "All" checkbox
  - Largest group expanded by default
  - AnimatedVisibility for expand/collapse
- Per file:
  - Checkbox (primary color)
  - Thumbnail (48dp, RoundedCornerShape 8dp) — real thumbnail for images/videos via ThumbnailLoader (LRU cache 50), IconCircle fallback for others
  - File name (titleSmall, ellipsis)
  - Category + relative date (bodySmall)
  - MiniBar showing relative size (3dp, category color)
  - Size (bodyMedium, numericFontFamily)
- APK_FILES: all pre-selected by default

### Sticky Bottom Bar (AnimatedVisibility slide-up)
- Before/after usage projection: "67% → 59%" with MiniBar
- Delete button (error color): "Free X GB · N items"

### Delete Flow
- API 30+: `MediaStore.createDeleteRequest()` → system confirmation dialog via ActivityResultLauncher
- API 29: `ContentResolver.delete()` per URI (no batch dialog)
- On success: Success overlay 1.8s → re-scan

---

## Charger Comparison (Pro)

- FAB to add charger (name input dialog)
- Charger list with test summaries (charge rate mAh/hr or W, date)
- Delete charger (trash icon)
- Historical charge rate comparison

---

## App Usage (Pro)

- Requires PACKAGE_USAGE_STATS permission
- Permission grant card when not available
- App list sorted by foreground time
- Per app: icon, name, foreground time, relative progress bar, percentage

---

## Settings Screen

Grouped card layout. Each section is a card with `CardSectionTitle`.

### Monitoring
- Interval: 15 / 30 / 60 min (radio buttons)

### Notifications
- Master toggle (on/off, dims sub-toggles when off)
- Low Battery (toggle + description)
- High Temperature (toggle + description)
- Low Storage (toggle + description)
- Charge Complete (toggle + description, default off)

### Alert Thresholds
- Low Battery Warning: Slider 5–50%, default 20%, steps of 5%
- High Temperature Warning: Slider 35–50°C, default 42°C, steps of 1°C
- Low Storage Warning: Slider 70–99%, default 90%, steps of 5%
- Value displayed right-aligned in numericFontFamily with primary color

### Display
- Temperature Unit: Celsius (°C) / Fahrenheit (°F) radio

### Data
- Retention: 3mo / 6mo / 1yr / Forever (Pro, radio)
- Export as CSV (Pro, OutlinedButton)
- Clear All Monitoring Data (error color → AlertDialog confirmation)

### Pro
- Status: Active ✓ / Purchase button + price / Restore purchase

### Privacy
- Crash Reporting toggle (Firebase Crashlytics)

### Device
- Model name (titleMedium)
- MetricPill grid: API Level, Current Reading (Reliable/Unreliable with status color), Cycle Count, Thermal Zones

### About
- Version (build)
- Rate on Play Store (→ market:// intent)
- Privacy Policy (→ URL)
- Send Feedback (→ mailto: intent)

---

## Pro Upgrade Screen

### Features Listed
| Feature | Icon |
|---------|------|
| Extended History | Calendar |
| Charger Comparison | Charging |
| Per-App Battery Usage | Data Usage |
| Widgets | Widgets |
| CSV Export | Download |
| Thermal Logs | Thermometer |
| Ad-Free | No Accounts |

- "Purchase €3.49" button (Google Play Billing)
- "One-time purchase, no subscription" footnote
- Success state: "Thank You!" with confetti animation
- Trial system: 7-day free trial, reminders on day 5 and 7

---

## Health Score Algorithm

```
overallScore = battery × 0.4 + thermal × 0.25 + network × 0.25 + storage × 0.1
```

### Battery Score (0–100)
Base 100, penalties for:
- Health status (overheat -40, dead -80, cold -20)
- Temperature outside 20–35°C range (up to -40)
- Voltage outside 3500–4250 mV range (up to -20)
- Health % degradation (up to -60)

### Thermal Score (0–100)
Based on battery/CPU temperature and throttling status.

### Network Score (0–100)
Based on signal quality and speed test data.

### Storage Score (0–100)
Based on usage % (0 penalty at 25–50%, up to -80 at 95%+).

---

## Notifications

| Alert | Trigger | Channel | Priority |
|-------|---------|---------|----------|
| Low Battery | Level below threshold | Alerts | High |
| High Temperature | Temp > 42°C | Alerts | High |
| Low Storage | Usage > 90% | Alerts | High |
| Charge Complete | Charging → Full | Status | Low |
| Trial Day 5 | 5th day of trial | Trial | Default |
| Trial Day 7 | 7th day of trial | Trial | Default |

---

## Visual Design

### Theme
- Single dark theme — no light mode, no AMOLED toggle
- BgPage `#0B1E24`, BgCard `#133040`, BgIconCircle `#1A3A4D`
- Typography: Manrope (body) + JetBrains Mono (numbers)

### Accent Colors
| Color | Hex | Usage |
|-------|-----|-------|
| Teal | `#5DE4C7` | Primary accent, healthy status, images |
| Blue | `#4A9EDE` | Data/info, videos |
| Orange | `#F5963A` | Warning, audio |
| Red | `#F06040` | Critical, error, delete |
| Lime | `#C8E636` | Success, documents, APKs |
| Yellow | `#F5D03A` | Attention, downloads |

### Status Colors
| Level | Score | Color |
|-------|-------|-------|
| Healthy | 75–100 | Teal |
| Fair | 50–74 | Blue |
| Poor | 25–49 | Orange |
| Critical | 0–24 | Red |

### Card Style
- Background: `surfaceContainer`, no border, no shadow, no elevation
- Corner radius: 16dp (cards), 8dp (small elements)
- ActionCards: same + 1dp `outlineVariant` border at 35% alpha
- Dividers: `outlineVariant.copy(alpha = 0.35f)`

### Animations
| Element | Duration | Easing |
|---------|----------|--------|
| ProgressRing | 1200ms | FastOutSlowInEasing |
| MiniBar | 800ms | FastOutSlowInEasing |
| SegmentedBar | 800ms | FastOutSlowInEasing |
| Thermometer fill | 1200ms | FastOutSlowInEasing |
| Battery wave | 2000ms | LinearEasing (loop) |
| Cleanup bottom bar | 200ms | slide vertical |
| Success overlay | 200ms in / 300ms out | fade |
| Screen transitions | 300ms | slide + fade |

All animations respect `MaterialTheme.reducedMotion`.

### Accessibility
- WCAG AA contrast: 4.5:1 body text, 3:1 large text
- Minimum touch target: 48dp
- Status colors always paired with text labels or icons
- Content descriptions on visual elements (charts, rings, bars)
- Reduced motion support

---

## Components (32+)

### Layout
`GridCard` · `ListRow` · `MetricPill` · `MetricRow` · `MetricTile` · `ActionCard`

### Indicators
`ProgressRing` · `MiniBar` · `StatusDot` · `StatusIndicator` · `ConfidenceBadge` · `SignalBars`

### Charts & Visuals
`TrendChart` · `AreaChart` · `SparklineChart` · `SpeedGauge` · `HeatStrip` · `SegmentedBar` · `SegmentedBarLegend`

### Navigation
`PrimaryTopBar` · `DetailTopBar`

### Typography
`AnimatedNumber` · `SectionHeader` · `CardSectionTitle` · `IconCircle`

### Pro
`ProBadgePill` · `ProFeatureCalloutCard` · `ProFeatureLockedState`

### Interactive
`PullToRefreshWrapper` · `PrimaryButton` · `SecondaryButton` · `ActionCard`

---

## Localization

| Language | File |
|----------|------|
| English (default) | `res/values/strings.xml` |
| Finnish | `res/values-fi/strings.xml` |
