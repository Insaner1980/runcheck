# Silent Observer UI Redesign

**Date:** 2026-03-27
**Inspiration:** Google Stitch "Silent Observer" concept — laboratory instrument aesthetic with typography-driven value display

## Design Philosophy

Shift from ring-gauge-centric displays to **typography-dominant hero values** with contextual status indicators. Keep existing strengths (chart animations, quality zones, push-nav, dark theme) while adding visual distinctiveness through tonal layering and status strips.

## Changes

### 1. New Components

#### StatusStrip
- 4dp wide vertical strip on left edge of cards
- Color follows `statusColors` (healthy/fair/poor/critical)
- Applied via `Modifier.drawBehind` — no layout change needed
- Used on: Home GridCards, detail screen summary cards

#### SegmentedStatusBar
- Horizontal bar showing value position on a scale
- 3-5 labeled segments (e.g., Optimal / Normal / Warm / Critical)
- Active segment highlighted with status color, others `surfaceVariant` at 0.3 alpha
- Height: 6dp bar + labels below
- Used on: Home health score, Thermal detail, Battery detail

### 2. Home Screen

#### Health Score Hero (replaces ProgressRing)
- **Before:** 152dp ProgressRing with "83%" inside
- **After:** Large "83" (64sp JetBrains Mono Bold) + "/100" (28sp TextSecondary) on same baseline
- Below: SegmentedStatusBar with 4 segments for each category
- Below that: status text ("Device health is good")
- Card background: BgCardDeep (#0D2530)

#### GridCards Enhancement
- Add StatusStrip to left edge (4dp, status-colored)
- Value text larger (titleLarge → headlineSmall, numericFont)
- Icon circle stays but shifts to 36dp (from 44dp)
- Subtitle text: bodySmall in TextMuted

### 3. Detail Screen Heroes

#### Battery
- **Before:** 152dp ProgressRing with level inside
- **After:** Side-by-side layout:
  - Left: 100dp ProgressRing (decorative, thinner 6dp stroke)
  - Right: Large "92" (64sp) + "%" (28sp) on baseline, status text below
- SegmentedStatusBar below (Charging / Discharging / Full)

#### Thermal
- **Before:** ThermometerIcon + temperature number
- **After:** Large "38" (64sp) + "°C" (28sp), status text below
- SegmentedStatusBar (Optimal / Normal / Warm / Critical) replaces thermometer
- Min/Max session range stays

#### Network
- **Before:** SignalBars (48dp) + quality label
- **After:** Large signal quality text + dBm/latency as secondary metrics
- SignalBars shrinks to 32dp, moves to right of heading
- Latency and signal strength as prominent MetricPills

#### Storage
- **Before:** 152dp ProgressRing with "XX%" inside
- **After:** Same approach as Battery:
  - Left: 100dp ProgressRing (thinner)
  - Right: Large percentage + "Used" label, free space below

### 4. Theme Additions

#### Colors
- `BgCardDeep = Color(0xFF0D2530)` — hero card backgrounds (deeper than BgCard)
- No other color changes — existing palette stays

#### Typography
- `numericHeroDisplayTextStyle`: 64sp, Bold, JetBrains Mono, -3sp letterSpacing
- `numericHeroDisplayUnitTextStyle`: 28sp, SemiBold, JetBrains Mono, TextSecondary

### 5. Tonal Layering

Surface depth hierarchy:
1. **Screen background** — BgPage (#0B1E24)
2. **Hero cards** — BgCardDeep (#0D2530) — "recessed instrument panel"
3. **Data cards** — BgCard (#133040)
4. **Interactive elements** — BgCardAlt (#0F2A35)

## What Stays Unchanged

- Chart animations (TrendChart oscilloscope sweep, LiveChart, AreaChart)
- Push-based navigation (no bottom nav)
- Quality zones in charts
- All existing components not mentioned above
- StatusColors system
- Accessibility annotations
- Info bottom sheets system
