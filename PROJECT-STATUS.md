# runcheck — Project Status

**Date:** 2026-03-28
**Version:** 1.0.0 (versionCode 1)
**Build:** Compiles successfully (Gradle 9.4.0, AGP 9.1.0, Kotlin 2.3.0)
**Tests:** 29 unit tests passing, 1 instrumented test (Room migration)

---

## Implemented Features

### Core Monitoring (4 categories)

**Battery**
- Real-time level, temperature, voltage, current (mA), power (W)
- Health score, charging status, battery health percentage
- Design capacity via PowerProfile, estimated capacity from health
- Current stats (avg/min/max) that reset on charge state change
- Screen on/off drain tracking with deep sleep / held awake detection
- Long-term statistics (charged/discharged totals, session counts, drain rates)
- History charts: Since Unplug, Day, Week, Month, All periods
- Session graph: current and power charts with 15m/30m/All windows
- Manufacturer-specific handling (Samsung, OnePlus, Pixel, API 34+, generic fallback)
- Confidence badges (Accurate / Estimated / Unavailable)

**Network**
- WiFi/cellular type detection, signal strength (dBm), signal bars
- Latency and jitter measurement
- Speed test (NDT7 protocol) with upload/download, server selection, history
- Live signal history chart with period selection

**Thermal**
- CPU temperature from thermal zones, thermal headroom
- Session min/max tracking
- Temperature history chart
- Normal/warm/critical status indicators

**Storage**
- Used/available space with fill rate (linear regression, 7-day lookback)
- Media breakdown: Images, Videos, Audio, Documents, Downloads, Other (SegmentedBar)
- Cleanup tools: Large Files, Old Downloads, APK Files, Trash (API 30+)
- Cleanup screen with thumbnails, category groups, before/after projection
- SD card detection
- Storage details: file system, encryption, volumes

### Home Screen (Dashboard)
- Unified health score from all 4 categories
- Grid of metric cards with sparkline trend charts
- Quick Tools card (Learn, Charger Comparison, App Usage links)
- Pull-to-refresh
- Adaptive layout (2x2 phones, 1x4 wide screens)

### Background Monitoring
- WorkManager periodic readings (configurable: 15/30/60 min)
- Real-time foreground service for live notification (opt-in)
- Live notification: BigTextStyle, updates every 5s, configurable metrics
- Alert system: Low Battery, High Temperature, Low Storage, Charge Complete
- Configurable thresholds (battery 5-50%, temp 35-50C, storage 70-99%)
- Boot receiver for restart after reboot

### Home Screen Widgets (Pro)
- Battery Widget (2x1): level, temperature, current
- Health Widget (2x2): overall score + 4 mini indicators
- Glance API (Compose-based)

### Charger Comparison (Pro)
- Charger session tracking and profiles
- Fill rate calculation
- Session history with comparison view

### Per-App Battery Usage (Pro)
- Foreground app detection
- Battery drain correlation
- Daily/weekly rankings

### Educational Content (free, 3 tiers)
- Tier 1: Info Bottom Sheets — 51 metric explanations via (?) icons
- Tier 2: Contextual Info Cards — 11 dismissible cards shown conditionally
- Tier 3: Learn Section — 15 articles in 5 topics, accessible from Home

### Settings
- Monitoring interval selection
- Live notification with per-metric toggles
- Per-alert notification toggles with threshold sliders
- Temperature unit (C/F)
- Data retention (Pro), CSV export (Pro), clear data with confirmation dialogs
- Pro status display, purchase, restore
- Device info (model, API level, reliability, cycle count, thermal zones)

### Pro / Monetization
- One-time in-app purchase (EUR 3.49) via Google Play Billing 8.3.0
- 7-day free trial with expiration notification
- 6 gated features: Extended History, Charger Comparison, Per-App Battery, Widgets, CSV Export, Thermal Logs
- Debug builds auto-enable Pro

### Charts & Animations
- TrendChart: oscilloscope sweep entry, status gradient line, quality zones, tap/drag tooltip, min/avg/max pills
- AreaChart: oscilloscope sweep + height-proportional fill
- LiveChart: smooth scroll with glow pulse
- All animations respect reduced motion preference

### Code Quality & Security
- Static analysis: Detekt + Compose Rules, ktlint, Android Lint, SonarCloud
- SonarCloud issue counts and quality-gate state are tracked in the dashboard instead of being hardcoded here
- CI: CodeQL security scanning
- R8/ProGuard enabled for release
- Explicit PendingIntents with FLAG_IMMUTABLE
- No cleartext traffic, no exported components without justification
- No analytics, no tracking, no crash reporting, no network calls except latency/speed test

---

## Not Yet Implemented

### Localization
- English only — Finnish translations were removed (preserved in git history)
- No multi-language support in current build

### Play Store Assets
- No feature graphic (1024x500)
- No screenshots for store listing
- Play Store listing text drafted (`docs/play-store-listing.md`) but not submitted
- Privacy policy drafted (`docs/privacy-policy.md`) but not hosted

### Testing Gaps
- 29 unit tests + 1 instrumented test — covers core logic but not comprehensive
- No Compose UI tests
- No end-to-end tests
- No device compatibility test matrix

### Release Process
- Release signing configured in build.gradle.kts but keystore not in repo (expected)
- Release checklist drafted (`docs/release-checklist.md`) but not executed
- No Play Console account setup documented
- No beta testing track configured
- No CI/CD for automated release builds (only CodeQL)

### Missing Minor Features
- PACKAGE_USAGE_STATS permission not declared in manifest (needed for per-app battery usage)
- Thermal throttling log screen exists but correlation with app usage not fully integrated
- Widget click-through navigation may need refinement on some launchers

---

## Project Structure

```
37 domain use cases
34 UI components (28 shared + 6 info)
15 screens with navigation
11 ViewModels
13 repository interfaces
10 Room database tables
29 unit tests + 1 instrumented test
15 Learn articles
51 info bottom sheets
11 contextual info cards
```

---

## Key Documentation

| File | Content |
|------|---------|
| `CLAUDE.md` | Architecture, conventions, design system |
| `docs/plans/2026-03-10-phase1-completion-and-roadmap.md` | Original Phase 1-4 roadmap |
| `docs/release-checklist.md` | Release build and Play Store checklist |
| `docs/play-store-listing.md` | Play Store listing content |
| `docs/privacy-policy.md` | Privacy policy |
| `educational-content-system.md` | Three-tier educational content spec |
| `docs/CHANGELOG.md` | Auto-generated changelog from git |
