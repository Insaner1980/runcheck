# runcheck — UI Specification

---

## 1. Home Screen (Card Grid)

The app opens to a card-grid home screen. There is no bottom navigation. All categories are presented as tappable cards in a 2-column grid. Each card shows a real-time summary of its category; tapping opens the full detail screen. A back button/gesture returns to the home grid.

**Top bar:** "runcheck" title on the left, gear icon (Settings) on the right.

### Card Grid Layout

**Free user:**

```
┌─────────────┐ ┌─────────────┐
│  Dashboard   │ │   Battery   │
│  Score: 89   │ │  57% Disch. │
│  [mini arc]  │ │  Healthy    │
└─────────────┘ └─────────────┘
┌─────────────┐ ┌─────────────┐
│   Network   │ │   Thermal   │
│  4G LTE     │ │  36.6°C     │
│  Good       │ │  Normal     │
└─────────────┘ └─────────────┘
┌─────────────┐ ┌─────────────┐
│  Chargers   │ │  App Usage  │
│  [lock] Pro │ │  [lock] Pro │
└─────────────┘ └─────────────┘
┌─────────────────────────────┐
│  Unlock runcheck Pro     │
│  Trends, insights & more    │
└─────────────────────────────┘
```

**Pro user:**

```
┌─────────────┐ ┌─────────────┐
│  Dashboard   │ │   Battery   │
│  Score: 89   │ │  57% Disch. │
│  [mini arc]  │ │  Healthy    │
└─────────────┘ └─────────────┘
┌─────────────┐ ┌─────────────┐
│   Network   │ │   Thermal   │
│  4G LTE     │ │  36.6°C     │
│  Good       │ │  Normal     │
└─────────────┘ └─────────────┘
┌─────────────┐ ┌─────────────┐
│  Chargers   │ │  App Usage  │
│  Last: 22W  │ │  Top: Maps  │
└─────────────┘ └─────────────┘
┌─────────────────────────────┐
│  Insights                    │
│  "Battery health dropped 2% │
│   this month"                │
└─────────────────────────────┘
```

### Card Content Summary

Each card displays a category icon, the category name, 1–2 key real-time values, and a status indicator (color-coded dot or text: Healthy/Fair/Poor).

| Card | Key Values Shown | Status Indicator |
|------|-----------------|-----------------|
| Dashboard | Health score (0–100), mini arc gauge | Arc color (green/yellow/red) |
| Battery | Level %, charging state (Charging/Discharging), health | Healthy / Degraded / Poor |
| Network | Connection type (WiFi/5G/4G), quality rating | Excellent / Good / Fair / Poor |
| Thermal | Battery temperature °C, throttling state | Normal / Warm / Hot / Critical |
| Storage | Available space (GB), usage % | Healthy / Getting Full / Critical |
| Chargers (Pro) | Last charger power (W) or "Tap to unlock" if free | — |
| App Usage (Pro) | Top battery consumer app or "Tap to unlock" if free | — |

### Pro Card (Free Users)

A full-width card at the bottom of the grid. Visually distinct: subtle accent-colored border or gradient tint. Shows a brief value proposition ("Trends, charger testing, insights & more") and tapping opens the Pro purchase screen. This card is **removed entirely** when the user purchases Pro.

### Insights Card (Pro Users)

Replaces the Pro card for paying users. Full-width card at the bottom. Displays one algorithmically generated insight at a time — the most significant recent observation from accumulated data. Examples:

- "Battery health dropped 2% this month"
- "Your charger delivers 15W avg — below your phone's 27W max"
- "Storage filling up — 3 months at current rate"
- "Battery temperature spikes daily around 2 PM — heavy app usage?"
- "Network quality is 40% worse at home vs. your workplace"

Insights refresh when new significant observations are detected (not on a fixed timer). Tapping the Insights card opens an Insights detail screen showing recent insights history.

### Dashboard Detail Screen

Tapping the Dashboard card opens the detail screen with the full-size health score gauge (animated arc, 0–100) and summary cards for each category showing their key metrics, mini sparkline charts, and status badges.

---

## 2. Detail Screens

### 2.1 Battery Detail Screen

**Real-time metrics (always visible):**

- Battery level (%)
- Voltage (mV)
- Temperature (°C)
- Charging status (Charging / Discharging / Full / Not Charging)
- Charging type (AC / USB / Wireless)
- Battery health (Good / Degraded / etc.)
- Battery technology (Li-ion, Li-poly, etc.)

**Charging current (mA) — with confidence indicator:**

- If reliable: shows real-time mA with a green confidence badge
- If unreliable: shows "Not available on this device" or "Estimated" with an info tooltip explaining why

**Conditional metrics (API 34+ / device-dependent):**

- Battery health percentage (e.g., "92% of original capacity")
- Charge cycle count
- Estimated remaining charge time

**24-hour mini chart** showing battery level over the last day (free tier limit).

### 2.2 Network Detail Screen

**Real-time metrics:**

- Connection type (WiFi / 4G LTE / 5G / None)
- Signal strength in dBm (with visual bar indicator)
- WiFi: SSID, link speed (Mbps), frequency band (2.4/5/6 GHz)
- Mobile: carrier name, network type, cell ID
- Approximate latency (measured via ping)

**Signal quality rating** — translated from raw dBm to a human-readable scale (Excellent / Good / Fair / Poor / No Signal)

### 2.3 Thermal Detail Screen

**Real-time metrics:**

- Battery temperature (°C) — always available
- CPU thermal zone temperatures — device-dependent
- Thermal status: None → Light → Moderate → Severe → Critical → Emergency → Shutdown
- Current throttling state (if detectable)

**Visual heat indicator** — color gradient from cool blue to hot red based on temperature.

### 2.4 Storage Detail Screen

**Metrics:**

- Total internal storage
- Used / available space
- Usage breakdown by category (Apps, Images, Videos, Audio, Documents, Other)
- SD card info (if present)

**Fill rate estimate** — "At current rate, storage will be full in ~X months"

### 2.5 Settings

Accessed via gear icon in the top-right corner of the home screen (not a separate card).

- **Theme mode** — Light / Dark / System default
- **Theme style** — Material You (dynamic wallpaper colors) / Custom Dark (curated glassmorphism palette)
- **AMOLED Black** — sub-toggle within Dark mode: pure black surfaces
- **Monitoring interval** — 15 min / 30 min / 1 hour
- **Notifications** — toggle alerts for low battery, high temperature, low storage
- **Measurement info** — which metrics are available and reliable on this device
- **About** — app version, licenses, links
- **Upgrade to Pro** — feature comparison and purchase (hidden if already Pro)

---

## 3. Insights Engine (Pro)

The Insights card on the home screen (Pro only, replaces the Pro promotional card) displays algorithmically generated observations from accumulated device data. The engine analyzes trends, anomalies, and correlations across all monitored categories.

**Insight categories:**

- **Battery degradation** — health percentage change over weeks/months, projected health at 1-year mark
- **Charger performance** — comparison of average power delivery across saved chargers, identification of underperforming cables
- **Thermal patterns** — recurring temperature spikes correlated with time of day or specific apps
- **Storage projections** — estimated time until storage is full based on growth rate
- **Network patterns** — signal quality differences by location or time of day
- **App battery impact** — apps with disproportionate battery consumption relative to usage time

**Insight generation rules:**

- Insights require a minimum data history (e.g., 7 days for thermal patterns, 30 days for battery degradation trends)
- Each insight has a significance threshold — only surface observations that represent meaningful changes, not noise
- Insights are prioritized by actionability (things the user can do something about rank higher)
- Maximum one new insight per day to avoid notification fatigue; insights queue if multiple are generated simultaneously
- Tapping the Insights card on the home screen opens an Insights history screen showing past insights in reverse chronological order

---

## 4. General UI/UX

- **Card-grid navigation** — home screen is a 2-column card grid, no bottom navigation bar. Tapping a card opens its detail screen. Android back button/gesture returns to the home grid. This keeps navigation simple and gives all categories equal visibility.
- **Dual theme system** — user chooses between Material You (dynamic wallpaper colors) and Custom Dark (glassmorphism-inspired curated palette).
- **Glassmorphism depth** (Custom Dark theme) — cards and elevated surfaces use semi-transparent backgrounds with subtle blur, layered borders, and tonal elevation for visual depth. Material You theme uses standard M3 tonal elevation instead.
- **Typography hierarchy** — clear distinction between values (large, high-contrast, monospace for numbers), labels (smaller, muted), and section headers (medium weight). Real-time numeric values use monospace font to prevent layout jitter.
- **Pull-to-refresh** on all detail screens for instant re-measurement
- **Smooth animations** — gauge fill animations, card transitions, chart drawing
- **Adaptive layout** — works on phones and tablets
- **Settings-style screens:** grouped cards for each section with glassmorphism surface treatment in Custom Dark. Radio buttons and toggles use the accent color for active states.

### Navigation

```
Home Screen (card grid):
┌─────────────────────────────────┐
│ runcheck              [gear] │ ← top bar
├────────────┬────────────────────┤
│ Dashboard  │ Battery            │ ← tap → detail screen
│ Network    │ Thermal            │ ← tap → detail screen
│ Chargers   │ App Usage          │ ← tap → detail / Pro gate
│ Pro / Insights (full-width)     │ ← tap → Pro purchase / Insights history
└─────────────────────────────────┘

Navigation flow:
  Home → [tap card] → Detail Screen → [back] → Home
  Home → [tap gear] → Settings → [back] → Home
  Home → [tap locked card] → Pro Purchase → [back] → Home
  Home → [tap Pro/Insights] → Pro Purchase / Insights History → [back] → Home
```

No bottom navigation. No hamburger menu. No drawer. All navigation is through the card grid and the system back gesture/button.

---

## 5. Design System

The app supports two visual modes, selectable in Settings:

1. **Material You mode** — uses dynamic wallpaper colors (Android 12+). Standard M3 tonal elevation, rounded shapes, and system typography.
2. **Custom Dark mode** — curated glassmorphism-inspired dark theme with depth layers, semi-transparent surfaces, subtle blur effects, and a refined color palette. Default on devices below Android 12.

Both modes share the same typography scale, spacing system, and layout structure. The difference is in surface treatments, color derivation, and elevation style.

- **Typography:** System default (Roboto) following M3 type scale; monospace (Roboto Mono) for real-time numeric values
- **Shape:** Rounded corners (16dp for cards, full rounding for buttons/badges)
- **Minimum touch target:** 48dp on all interactive elements

### Theme Modes

**Brightness:** Light / Dark / System (follows Android setting)
**Style:** Material You / Custom Dark

AMOLED Black is a sub-toggle within Dark mode (both styles), enabling pure black surfaces.

| Combination | Surface | Elevation | Cards |
|------------|---------|-----------|-------|
| Material You + Light | M3 light surfaces | Tonal elevation | Solid M3 containers |
| Material You + Dark | M3 dark surfaces (#121212) | Tonal elevation | Solid M3 containers |
| Material You + AMOLED | Pure black (#000000) | Tonal elevation | Solid #0A0A0A |
| Custom Dark + Dark | Deep dark (#0D0D14) | Glassmorphism layers | Semi-transparent + blur |
| Custom Dark + AMOLED | Pure black (#000000) | Glassmorphism layers | Semi-transparent + blur |
| Custom Dark + Light | Not available — falls back to Material You + Light |

### Color Palette

#### Material You Fallback Colors (below Android 12)

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

#### Custom Dark Theme Palette (Glassmorphism)

**Background layers (bottom to top):**

| Layer | Color | Usage |
|-------|-------|-------|
| Deep background | #0A0A0F | Deepest layer, visible behind everything |
| Base surface | #0D0D14 | Home screen background, screen backgrounds |
| Card surface | rgba(255, 255, 255, 0.03) + blur(12px) | Home grid cards, detail screen metric tiles |
| Elevated surface | rgba(255, 255, 255, 0.06) + blur(20px) | Modals, dialogs, overlays, expanded cards |
| AMOLED override | #000000 (deep bg), #050508 (base surface) | When AMOLED Black is enabled with Custom Dark |

**Text colors (Custom Dark):**

| Token | Color | Usage |
|-------|-------|-------|
| Text primary | #E8E8ED | Main values, headings |
| Text secondary | #9898A8 | Labels, descriptions |
| Text muted | #5A5A6A | Timestamps, tertiary info, disabled text |
| Border subtle | rgba(255, 255, 255, 0.06) | Card borders, dividers |
| Border active | rgba(255, 255, 255, 0.12) | Focused inputs, active card borders |

**Accent color (Custom Dark):**

| Token | Color | Usage |
|-------|-------|-------|
| Primary accent | #5EEAD4 (teal 300) | CTAs, active states, links |
| Primary accent muted | #2DD4BF at 20% opacity | Subtle highlights, selected card tint |

**Key color rules (both themes):**

- Never use pure white (#FFFFFF) text on dark backgrounds — use #E0E0E0 / #E8E8ED instead
- Dark theme uses #121212 (Material You) or #0D0D14 (Custom Dark), not pure black. AMOLED Black (#000000) is opt-in only.
- Contrast ratio: WCAG AA minimum 4.5:1 for body text, 3:1 for large text and UI elements. Target 5:1+ for critical reading content.

#### Semantic Status Colors

| Status | Light Theme | Dark / AMOLED Theme | Usage |
|--------|------------|-------------------|-------|
| Healthy / Good | #16A34A | #4ADE80 | Score 75-100, normal temps, good signal |
| Fair / Attention | #D97706 | #FBBF24 | Score 50-74, warm temps, fair signal |
| Poor / Warning | #EA580C | #FB923C | Score 25-49, hot temps, poor signal |
| Critical / Danger | #DC2626 | #F87171 | Score 0-24, overheating, no signal |
| Neutral / Info | #2563EB | #60A5FA | Informational, links, non-status data |
| Unavailable | #737373 | #737373 | Disabled metrics, unavailable features |

Colors are always paired with icons or text labels — never color alone — for accessibility.

#### Confidence Badge Colors

| Badge | Light | Dark | Label |
|-------|-------|------|-------|
| Accurate | #16A34A bg, #FFFFFF text | #065F46 bg, #6EE7B7 text | "Accurate" |
| Estimated | #D97706 bg, #FFFFFF text | #78350F bg, #FDE68A text | "Estimated" |
| Unavailable | #D4D4D4 bg, #525252 text | #404040 bg, #A3A3A3 text | "N/A" |

### Spacing System

Based on a 4dp grid:

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
| Card corner radius | 16dp |
| Home grid card gap | 12dp (both horizontal and vertical) |
| Home grid card min height | 120dp |
| Pro / Insights card (full-width) min height | 80dp |
| Detail screen card gap (vertical list) | 12dp |
| Dashboard health gauge diameter | 200dp |
| Battery detail gauge diameter | 160dp |
| Top bar height | 56dp (standard toolbar) |
| Metric tile min height | 72dp |
| Sparkline chart height (inline) | 40dp |
| Trend chart height (detail screen) | 200dp |
| Confidence badge height | 24dp, corner radius full |
| Status bar padding top | system inset + 8dp |

### Typography Scale

| Style | Font | Size | Weight | Color (Custom Dark) | Usage |
|-------|------|------|--------|-------------------|-------|
| Display Large | Roboto | 57sp | 400 | Text primary | Health score number in dashboard gauge |
| Display Small | Roboto Mono | 32sp | 400 | Text primary | Hero metric values on home grid cards (e.g., "57%", "36.6°C") |
| Headline Medium | Roboto | 28sp | 400 | Text primary | Screen titles (detail screens) |
| Title Medium | Roboto | 16sp | 500 | Text primary | Card titles, section headers |
| Body Large | Roboto Mono | 18sp | 400 | Text primary | Real-time metric values in detail screens (mA, mV, °C) |
| Body Medium | Roboto | 14sp | 400 | Text secondary | Metric labels, descriptions |
| Body Small | Roboto | 12sp | 400 | Text muted | Timestamps, secondary info |
| Label Medium | Roboto | 12sp | 500 | (per badge color) | Confidence badges, chip labels, status text |
| Label Small | Roboto | 11sp | 500 | Text muted | Chart axis labels |

**Key hierarchy rules:**

- On home grid cards: the value (e.g., "57%") uses Display Small (32sp, monospace). The label (e.g., "Battery") uses Title Medium (16sp). The sub-label (e.g., "Discharging") uses Body Small (12sp, muted). This creates an unmistakable visual hierarchy where the number jumps out.
- On detail screens: metric values use Body Large (18sp, monospace). Metric labels use Body Medium (14sp, secondary color). Deliberately smaller than home cards because detail screens show many values at once.
- Roboto Mono for all numeric values prevents layout jitter when digits change rapidly.

### Animations

#### Gauge Animations

- **Health score arc fill:** Animates from 0° to target angle over **800ms** using spring (dampingRatio = 0.7, stiffness = 200). Slight overshoot and settle for organic feel.
- **Gauge color transitions:** When score crosses a threshold (e.g., fair→good), arc color cross-fades over **300ms**.
- **Real-time current gauge:** Smooth spring tracking (dampingRatio = 0.8) of fluctuating values. No hard jumps.

#### Number Animations

- **Real-time metric values** (mA, mV, °C, %): Rolling counter effect over **200ms** — number smoothly counts up or down rather than snapping.
- **Battery percentage:** Animates on first display, static on subsequent updates.

#### Card & Screen Transitions

- **Home grid cards entrance:** Staggered fade-in + slide-up. Each card delayed by **40ms** from the previous. Duration **300ms** per card. Total entrance sequence ~580ms for 8 cards. Pro/Insights card enters with **80ms** extra delay for visual distinction.
- **Card-to-detail navigation:** Container transform — tapped card expands into detail screen. Duration **350ms**. Card's position and size morph into the detail screen layout for spatial continuity.
- **Back navigation:** Reverse container transform — detail screen shrinks back into its card. Duration **300ms**. Works with system back gesture too.
- **Pull-to-refresh:** Rotating pulse animation on indicator, spring return on release.

#### Chart Animations

- **Sparkline (dashboard cards):** Path draws left-to-right on card entrance over **600ms**. Monitor trace feel.
- **Trend chart (detail screens):** Line draws in with gradient fill fading in over **800ms**. Data points pop in with scale animation staggered along the line.
- **Touch-to-inspect:** Data point scales to 1.3x with subtle bounce, tooltip fades in over **150ms**.

#### Thermal Visualization

- **Temperature gradient strip:** Colors shift smoothly as temperature changes. Critical range (>42°C): subtle pulsing glow — opacity oscillates 0.7–1.0 over **2 seconds**.
- **Thermal status badges:** Crossfade between states over **300ms**.

#### Micro-interactions

- **Card press feedback:** Scale down to 0.97x on press, spring back on release. **100ms** down, **200ms** spring return.
- **Toggle switches:** Spring physics thumb slide, track color cross-fade over **200ms**.
- **Confidence badge appear:** Scale 0→1 with spring bounce (dampingRatio = 0.6).

#### Performance Rules

- Disable or reduce animations when system "Remove animations" setting is enabled.
- Real-time gauge updates throttled to max **3 updates/second** to prevent jank.
- Chart animations only play on first appearance, not on re-composition.

### Key Visual Components

1. **Home grid cards** — 2-column responsive grid. Each card shows category icon, name, hero value (large monospace), sub-label, and status indicator. Custom Dark: glassmorphism surface with subtle border. Material You: standard M3 card container.
2. **Pro card** (free users) — full-width card at bottom with accent-colored border/gradient tint. Removed when Pro is purchased.
3. **Insights card** (Pro users) — full-width card replacing Pro card. Shows one algorithmic insight with subtle animated transition when insight content changes.
4. **Locked card overlay** — Chargers and App Usage (free users): card visually dimmed with lock icon. Tapping opens Pro purchase.
5. **Circular health gauge** (dashboard detail) — animated arc with score number, color transitions red→yellow→green.
6. **Mini sparkline charts** — inline trend indicators on dashboard detail summary cards.
7. **Real-time gauge** (battery current) — animated needle/arc gauge with min/max markers.
8. **Confidence badges** — small colored pills: green "Accurate", yellow "Estimated", gray "Unavailable".
9. **Trend charts** (Pro) — line charts with gradient fills, touch-to-inspect data points.
10. **Heat map visualization** (thermal) — color gradient strip showing temperature zones.
11. **Metric tiles** — compact cards in detail screens: value + label + optional sparkline, responsive grid. Labels use Body Medium (14sp, secondary), values use Body Large (18sp, monospace, primary).

### Detail Screen Layout

All detail screens share a consistent visual treatment:

**Metric tiles:** Each piece of data lives in its own tile (card) with the same surface treatment as home grid cards. Label above value in clearly distinct size/color:

```
┌──────────────────────────┐
│ Battery Level             │  ← Body Medium, 14sp, secondary color
│ 57%                       │  ← Body Large, 18sp, monospace, primary color
│ Discharging               │  ← Body Small, 12sp, muted
└──────────────────────────┘
```

**Section grouping:** Related metrics grouped under section headers (e.g., "Real-time", "Health", "Charging"). Section headers use Title Medium (16sp, 500 weight) with 24dp above and 8dp below.

**Charts (History / Trends):**

- Y-axis labels on the left, X-axis time labels at the bottom — Label Small (11sp, muted)
- Grid lines: very subtle (rgba(255, 255, 255, 0.04) in Custom Dark)
- Line color: primary accent (#5EEAD4 in Custom Dark)
- Fill: gradient from accent at 20% opacity (top) to transparent (bottom)
- **Touch-to-inspect:** tapping/dragging shows vertical crosshair with tooltip (exact value + timestamp). Data point scales 1.3x with bounce. Tooltip uses elevated glassmorphism surface.
- **Time range selector:** segmented control above chart: "24h" (free), "7d" / "30d" / "All" (Pro). Free users see subtle "Pro" label on locked ranges.
- **Empty state:** "Collecting data... Check back in [time]" rather than empty chart frame.
