# UI-SPEC.md — runcheck Design System & Component Reference

Authoritative UI specification for the runcheck Android app. All UI code must conform to this document.

---

## 1. Design Philosophy

- **Single dark theme** — no light mode, no AMOLED toggle, no dynamic colors
- **Flat design** — no elevation, no shadows, no borders on cards (except ActionCard)
- **Data-first** — numeric values dominate, labels are secondary
- **Honest readings** — always show confidence level, never fake precision
- **Accessibility by default** — 4.5:1 contrast, 48dp touch targets, reduced motion support

---

## 2. Color System

Defined in `ui/theme/Color.kt`. Applied via `darkColorScheme()` in `ui/theme/Theme.kt`.

### Backgrounds

| Token | Hex | Material3 Role | Usage |
|-------|-----|----------------|-------|
| BgPage | `#0B1E24` | `background`, `surface` | Page background |
| BgCard | `#133040` | `surfaceContainer` | Card backgrounds |
| BgCardAlt | `#0F2A35` | `surfaceContainerHigh` | Info cards, elevated surfaces |
| BgIconCircle | `#1A3A48` | `surfaceContainerHighest`, `surfaceVariant` | Icon circle backgrounds |

### Accents

| Token | Hex | Material3 Role | Usage |
|-------|-----|----------------|-------|
| AccentBlue | `#4A9EDE` | `primary` | Primary accent, buttons, links, brand color |
| AccentTeal | `#5DE4C7` | `secondary` | Healthy status, positive values |
| AccentAmber | `#E8C44A` | `tertiary` | Fair status, warnings |
| AccentOrange | `#F5963A` | — | Poor status |
| AccentRed | `#F06040` | `error` | Critical status, destructive actions |
| AccentLime | `#C8E636` | — | Storage: Documents category |
| AccentYellow | `#F5D03A` | — | Storage: Downloads category |

### Text

| Token | Hex | Material3 Role | Usage |
|-------|-----|----------------|-------|
| TextPrimary | `#E8E8ED` | `onSurface`, `onBackground` | Main text, data values |
| TextSecondary | `#90A8B0` | `onSurfaceVariant` | Labels, descriptions |
| TextMuted | `#7A949E` | `outline`, `outlineVariant` | Hints, dividers, disabled text |
| TextOnLime | `#1A2E0A` | — | Text on lime-colored backgrounds |

### Color Rules

- **Data values** default to `onSurface` (TextPrimary). Never color a value unless it represents status.
- **Status labels** use `statusColors` (Teal/Amber/Orange/Red). Always paired with icon or text label.
- **Dividers** use `outlineVariant.copy(alpha = 0.35f)` everywhere — no hardcoded divider colors.
- **Contrast minimum:** 4.5:1 body text, 3:1 large text (WCAG AA).

---

## 3. Status System

Defined in `ui/theme/StatusColors.kt`. Accessed via `MaterialTheme.statusColors` extension.

### Status Tiers & Thresholds

| Status | Color | Battery % | Temperature | Storage Used | Signal |
|--------|-------|-----------|-------------|--------------|--------|
| Healthy | Teal `#5DE4C7` | ≥ 75% | < 35°C | < 75% | Excellent, Good |
| Fair | Amber `#E8C44A` | 50–74% | 35–39°C | 75–84% | Fair |
| Poor | Orange `#F5963A` | 25–49% | 40–44°C | 85–94% | Poor |
| Critical | Red `#F06040` | < 25% | ≥ 45°C | ≥ 95% | No Signal |

### Confidence Badges

| Level | Background | Text Color | Label |
|-------|-----------|------------|-------|
| Accurate | AccentBlue | BgPage | "Accurate" |
| Estimated | AccentAmber | BgPage | "Estimated" |
| Unavailable | TextMuted | TextPrimary | "Unavailable" |

---

## 4. Typography

Defined in `ui/theme/Type.kt`. Two font families loaded from `res/font/`.

### Font Families

| Family | Usage | Access |
|--------|-------|--------|
| **Manrope** | All body text, headers, labels, buttons | `MaterialTheme.typography` |
| **JetBrains Mono** | Numeric displays, metric values, charts | `MaterialTheme.numericFontFamily` |

### Type Scale (Manrope)

| Style | Size | Weight | Tracking | Usage |
|-------|------|--------|----------|-------|
| displayLarge | 48sp | Bold | -0.04em | Hero values |
| displayMedium | 36sp | Bold | — | Large headings |
| displaySmall | 28sp | SemiBold | — | Section headings |
| headlineLarge | 20sp | SemiBold | — | Card titles |
| headlineMedium | 16sp | SemiBold | — | Subsection titles |
| headlineSmall | 14sp | SemiBold | — | Top bar title |
| titleLarge | 20sp | Medium | — | Grid card titles |
| titleMedium | 16sp | Medium | — | Metric pill values, info sheet title |
| titleSmall | 14sp | Medium | — | Info card headline, list labels |
| bodyLarge | 15sp | Normal | — | Row labels, primary body text |
| bodyMedium | 14sp | Normal | — | Metric row labels, descriptions |
| bodySmall | 13sp | Normal | — | Pill labels, small descriptions |
| labelLarge | 12sp | SemiBold | 0.08em | Section headers, badge text |
| labelMedium | 10sp | SemiBold | — | Badge text, small labels |
| labelSmall | 10sp | Medium | — | Chart axis labels, timestamps |

### Numeric Text Styles (JetBrains Mono)

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| numericHeroValueTextStyle | 48sp | Bold | Battery/thermal hero values |
| numericHeroLargeValueTextStyle | 54sp | — | Battery level percentage |
| numericHeroLevelTextStyle | 48sp, -2sp tracking | Bold | Compact hero display |
| numericHeroUnitTextStyle | 20sp | SemiBold | Units (%, °C, mA) |
| numericRingValueTextStyle | 32sp | Bold | ProgressRing center |
| numericSpeedHeroValueTextStyle | 40sp | — | Speed test hero |
| chartAxisTextStyle | 10sp | Medium | Chart axis text |
| chartTooltipTextStyle | 11sp | — | Chart tooltip text |

---

## 5. Spacing & Layout

Defined in `ui/theme/Spacing.kt`. Accessed via `MaterialTheme.spacing`.

### Spacing Tokens (4dp grid)

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4dp | Tight gaps, inline spacing |
| sm | 8dp | Small gaps, inter-row |
| md | 12dp | Between cards, standard gaps |
| base | 16dp | Card padding, section spacing |
| lg | 24dp | Between major sections |
| xl | 32dp | Page margins, large separations |

Shared touch targets, icon sizes, icon circles, and common CTA heights are defined in `ui/theme/UiTokens.kt` and accessed via `MaterialTheme.uiTokens`.

### Shared UI Metrics

| Token | Value | Usage |
|-------|-------|-------|
| touchTarget | 48dp | Minimum interactive height/width |
| iconSmall | 16dp | Small inline icons |
| iconMedium | 18dp | Compact trailing/action icons |
| iconLarge | 20dp | Standard row and feature icons |
| iconXLarge | 24dp | Larger CTA / feature icons |
| iconCircle | 44dp | Standard icon circle container |
| compactIconCircle | 36dp | Compact card icon circle |
| primaryButtonHeight | 56dp | Primary CTA buttons |
| compactButtonHeight | 52dp | Compact sheet CTA buttons |
| dialogIcon | 64dp | Dialog hero icon |
| celebrationIcon | 80dp | Thank-you / success hero icon |

### Layout Rules

- **Card internal padding:** 16dp (base) on all sides
- **Card gap:** 12dp (md) between adjacent cards
- **Section gap:** 24dp (lg) between titled sections
- **Grid card min height:** 48dp touch target on interactive rows
- **Column content:** vertical scroll, no LazyColumn on detail screens (all data loads at once)

---

## 6. Shapes

Defined in `ui/theme/Shapes.kt`.

| Shape | Radius | Usage |
|-------|--------|-------|
| large | 16dp | Cards, panels, dialogs, bottom sheets |
| medium | 8dp | Badges, chips, small containers |
| small | 8dp | Compact interactive elements |
| extraLarge | 50% | Circles (icons, dots, avatar shapes) |

---

## 7. Component Reference

All shared composables live in `ui/components/` (32 components + `info/` subpackage).

### 7.1 Layout Components

#### GridCard
Home screen quick-access card. Icon circle + title + subtitle + optional status.

| Property | Value |
|----------|-------|
| Background | `surfaceContainer` |
| Corners | 16dp (`shapes.large`) |
| Elevation | None |
| Icon circle | 44dp, `surfaceContainerHighest` bg, 22dp icon |
| Padding | 16dp horizontal, 12dp vertical |
| Status label | After subtitle with `·` separator, colored |
| Lock overlay | 18% alpha scrim + `ProBadgePill` top-right |

#### ListRow
Settings / menu row with optional icon, value, trailing content.

| Property | Value |
|----------|-------|
| Min height | 48dp |
| Label | `bodyLarge`, `onSurface` |
| Value | `bodyMedium`, `onSurfaceVariant` |
| Icon | 20dp, `onSurfaceVariant` |
| Trailing arrow | `KeyboardArrowRight` in rounded surface |
| Padding | 12dp vertical |

#### ActionCard
Outlined card for cleanup tool actions.

| Property | Value |
|----------|-------|
| Background | `surfaceContainer` |
| Border | 1dp, `outlineVariant` at 35% alpha |
| Corners | 16dp |
| Icon circle | 40dp |
| Action | TextButton with `KeyboardArrowRight` |

### 7.2 Data Display Components

#### MetricPill
Small label-value display for stats.

| Property | Value |
|----------|-------|
| Label | `bodySmall`, `onSurfaceVariant` |
| Value | `titleMedium`, `onSurface` (customizable) |
| Gap | 2dp between label and value |
| Info icon | 16dp icon, 48dp touch target (optional) |

#### MetricRow
Full-width label-value pair for detail screens.

| Property | Value |
|----------|-------|
| Label | `bodyMedium`, `onSurfaceVariant` |
| Value | `titleLarge` SemiBold, `numericFontFamily`, `onSurface` |
| Divider | `outlineVariant` 35% alpha (optional) |
| Copyable | Copy icon + Toast on tap (optional) |
| Info icon | Optional, opens InfoBottomSheet |

#### ProgressRing
Circular progress indicator for hero sections.

| Property | Value |
|----------|-------|
| Stroke | 10dp default |
| Track color | `iconCircleColor` (BgIconCircle) |
| Progress color | Status-based or `primary` |
| Caps | `StrokeCap.Round` |
| Direction | Starts at 270° (top), clockwise |
| Animation | 1200ms `FastOutSlowInEasing` |
| Content | Composable slot for center text |

#### MiniBar
Small horizontal progress bar.

| Property | Value |
|----------|-------|
| Height | 6dp default (12dp for files) |
| Track | `iconCircleColor` |
| Shape | Pill (extraLarge) |
| Animation | 800ms `FastOutSlowInEasing` |

#### StatusDot
Colored circle indicator.

| Property | Value |
|----------|-------|
| Size | 8dp default |
| Shape | Circle |
| Semantics | Cleared (decorative) |

#### ConfidenceBadge
Pill badge showing measurement reliability.

| Property | Value |
|----------|-------|
| Shape | Pill (`extraLarge`) |
| Padding | 12dp horizontal, 4dp vertical |
| Text | `labelMedium` |
| Animation | Spring (dampingRatio 0.6, stiffness Medium) |

#### SignalBars
5-bar signal strength indicator.

| Property | Value |
|----------|-------|
| Bar heights | 10 / 18 / 26 / 36 / 48dp |
| Bar width | 12dp |
| Gap | 4dp |
| Corners | 3dp |
| Active color | Status color for signal quality |
| Inactive | `surfaceVariant` 30% alpha |

#### HeatStrip
Horizontal gradient temperature visualization.

| Property | Value |
|----------|-------|
| Height | 24dp |
| Corners | 12dp |
| Gradient | Teal → Amber → Orange → Red (soft 1°C transitions) |
| Indicator | White circle, 8dp radius |
| Critical pulse | 0.7–1.0 alpha, 2000ms, `LinearEasing` |

#### IconCircle
Centered icon in circular background.

| Property | Value |
|----------|-------|
| Size | 44dp default |
| Icon size | 22dp default |
| Background | `iconCircleColor` |

### 7.3 Typography Components

#### SectionHeader
Page-level section divider.

| Property | Value |
|----------|-------|
| Text | Uppercase |
| Style | `labelLarge`, SemiBold |
| Color | `onSurfaceVariant` |
| Semantic | Heading role |

#### CardSectionTitle
Subsection title within cards.

| Property | Value |
|----------|-------|
| Text | Uppercase |
| Style | `labelLarge` |
| Color | `onSurfaceVariant` |
| Semantic | Heading role |

#### AnimatedIntText / AnimatedFloatText
Count-up animation for numeric values.

| Property | Value |
|----------|-------|
| Animation | 200ms linear tween |
| Float precision | Configurable decimal places |
| Reduced motion | Instant (0ms duration) |

### 7.4 Navigation Components

#### PrimaryTopBar
Home screen top bar.

| Property | Value |
|----------|-------|
| Background | `surface` |
| Title | `headlineSmall`, `onSurface` |
| Padding | Status bar + 4/8/2/4dp |
| Actions | Custom composable slot (right side) |

#### DetailTopBar
Detail screen top bar with back navigation.

| Property | Value |
|----------|-------|
| Background | `surface` |
| Title | `titleLarge`, `onSurface`, centered |
| Back icon | `ArrowBack` (left side) |
| Right spacer | 48dp (for title symmetry) |

### 7.5 Chart Components

#### TrendChart
Primary chart with axes, grid, tooltip, quality zones. Uses "Instrument Sweep" animation language.

**Embedded mode (default):**

| Property | Value |
|----------|-------|
| Height | 200dp |
| Line stroke | 2dp |
| Grid stroke | 1dp |
| Padding | 8dp |
| Point markers | 4dp selected outer, 2dp inner |

**Fullscreen mode:**

| Property | Value |
|----------|-------|
| Height | Fills container (min 180dp) |
| Line stroke | 3dp |
| Grid stroke | 1.5dp |
| Padding | 16dp |
| Point markers | 6dp selected outer, 3dp inner |
| Font sizes | 12sp labels, 13sp tooltips |

**Shared:**

| Property | Value |
|----------|-------|
| Line caps | Round |
| Max points | 300 embedded, 600 fullscreen |
| Interaction | Tap/drag to select point, tap again to deselect (disabled during sweep) |
| Tooltip | Rounded container, positioned above/below point |
| Quality zones | Semi-transparent background bands (0.06–0.08f alpha) |

**Animation — 3-phase entry ("Instrument Sweep"):**

| Phase | Duration | Easing | Description |
|-------|----------|--------|-------------|
| 1. Grid materialization | 200ms | `FastOutSlowInEasing` | Grid lines, axes, quality zone bands fade in together |
| 2. Oscilloscope sweep | 1000ms | `CubicBezier(0.25, 0.1, 0.25, 1)` | `clipRect`-based left-to-right reveal + vertical scan line (1.5dp, lineColor 50% alpha). Scan line fades out during final 30% (delay 700ms + 300ms fade) |
| 3. Last value emphasis | 200ms | `FastOutSlowInEasing` | Glow dot (6dp, 30% alpha) + dashed horizontal line (4dp dash, 40% alpha) to Y-axis at last data point |

**Animation — Data transition (period/metric change):**

| Phase | Duration | Easing | Description |
|-------|----------|--------|-------------|
| Fade out old data | 300ms | `FastOutSlowInEasing` | Previous data line fades, reconstructed from stored data points |
| Sweep in new data | 800ms | `CubicBezier(0.25, 0.1, 0.25, 1)` | Same oscilloscope sweep, faster than initial entry. Grid stays visible |
| Emphasis | 200ms | `FastOutSlowInEasing` | Last value emphasis appears |

200ms overlap between fade-out start and sweep start.

**Status gradient line:**

When `qualityZones` are provided, the data line color follows the value at each point using `Brush.horizontalGradient` with per-point color stops. Colors derived from `qualityZoneColorForValue()` at full alpha. Metrics without quality zones keep a single solid color.

**Improved gradient fill:**

Strip-based rendering — each vertical strip between adjacent data points gets its own `Brush.verticalGradient` where top alpha depends on data value height: `lerp(0.08f, 0.30f, normalizedY)`. Single `Path` object reused with `reset()` per strip.

**Last value emphasis:**

Drawn outside the sweep `clipRect`, controlled by `emphasisAlpha`:
- Outer glow circle: 6dp radius, lineColor at 30% × emphasisAlpha
- Inner dot: 3dp radius, lineColor at 100% × emphasisAlpha
- Dashed line: `PathEffect.dashPathEffect(4dp, 4dp)`, 1dp stroke, 40% alpha, from last point to Y-axis

#### AreaChart
Simple area-under-curve chart (no interaction). Used for blurred Pro preview states.

| Property | Value |
|----------|-------|
| Line stroke | 1.5dp, 70% alpha |
| Fill | Strip-based gradient, alpha `lerp(0.08f, 0.25f, normalizedY)` |
| Animation | 800ms oscilloscope sweep (`CubicBezier(0.25, 0.1, 0.25, 1)`) + scan line |
| Vertical padding | 10% of height |

#### LiveChart
Real-time sparkline for live battery current/power monitoring.

| Property | Value |
|----------|-------|
| Height | 80dp |
| Line stroke | 1.5dp |
| Fill | Vertical gradient, 20% → 2% alpha |
| Alignment | Right-aligned (newest data at right edge) |
| Grid | 3 horizontal lines at 25%, 50%, 75% |
| Max points | 60 (default) |
| Current value dot | Outer glow 5dp/30% alpha + inner 3dp solid |

**Smooth scroll interpolation:**

When new data arrives, entire path shifts left with 150ms `LinearEasing` animation via `Animatable` scroll offset. Previous data size tracked to detect new points.

**Glow pulse on new data:**

New data point triggers pulse: radius 8→5dp + alpha 0.5→0.3 over 300ms. Settles to normal glow state (5dp, 0.3 alpha).

#### SegmentedBar
Proportional stacked bar for media breakdown.

| Property | Value |
|----------|-------|
| Height | 12dp |
| Track | `surfaceVariant` 50% alpha |
| Corners | 6dp (first/last segments) |
| Gap | 2dp between segments |
| Min segment width | 4dp |
| Animation | 800ms `FastOutSlowInEasing` |

**Storage media category colors:**

| Category | Color |
|----------|-------|
| Images | AccentTeal `#5DE4C7` |
| Videos | AccentBlue `#4A9EDE` |
| Audio | AccentOrange `#F5963A` |
| Documents | AccentLime `#C8E636` |
| Downloads | AccentYellow `#F5D03A` |
| Other | TextMuted `#7A949E` |

#### SegmentedBarLegend
Legend rows with StatusDots.

| Property | Value |
|----------|-------|
| Layout | Vertical column, 4dp gaps |
| Dot | StatusDot (8dp) |
| Label | `bodySmall`, `onSurfaceVariant` |
| Value | `bodySmall`, `onSurface` |

#### ExpandableChartContainer
Chart wrapper with fullscreen expand button.

| Property | Value |
|----------|-------|
| Button position | Top-right, 8dp padding |
| Button bg | `surfaceContainerHigh` 90% alpha |
| Icon | `OpenInFull` (16dp) + "EXPAND" label |

### 7.6 Pro Feature Components

#### ProBadgePill
Small lock pill for pro-gated features.

| Property | Value |
|----------|-------|
| Background | Primary 12% alpha |
| Shape | 8dp (`shapes.small`) |
| Padding | 8dp horizontal, 3dp vertical |
| Icon | Lock (12dp) + text |
| Text | `labelMedium`, primary color |

#### ProFeatureCalloutCard
In-screen upgrade prompt.

| Property | Value |
|----------|-------|
| Background | `surfaceContainer`, 16dp corners |
| Padding | 16dp |
| Message | `bodyMedium`, `onSurfaceVariant` |
| Button | `OutlinedButton`, primary accent border |

#### ProFeatureLockedState
Full-screen locked overlay.

| Property | Value |
|----------|-------|
| Layout | Centered column, fills container |
| Padding | 32dp horizontal |
| Title | `titleLarge`, `onSurface` |
| Message | `bodyMedium`, `onSurfaceVariant` |
| Button | `OutlinedButton`, primary accent |

### 7.7 Educational Components (`ui/components/info/`)

#### InfoIcon
Small (?) help icon.

| Property | Value |
|----------|-------|
| Icon | `HelpOutline` (16dp) |
| Touch target | 48dp (`IconButton`) |
| Tint | `onSurfaceVariant` |

#### InfoBottomSheet
Modal bottom sheet with metric explanation.

| Property | Value |
|----------|-------|
| Container | `surfaceContainer`, 16dp top corners |
| Max height | 60% screen, scrollable |
| Padding | 24dp horizontal, 32dp bottom |
| Sections | Title → Explanation → "What's normal" card → "Why it matters" → Expandable deeper detail |
| "What's normal" card | `surfaceContainerHigh`, 8dp corners, 12dp padding |
| Deeper detail | Expand/collapse animated (`expandVertically`/`shrinkVertically`) |

**Content data class (`InfoSheetContent`):**
```
title: @StringRes
explanation: @StringRes
normalRange: @StringRes
whyItMatters: @StringRes
deeperDetail: @StringRes? (optional expandable section)
```

Coverage: Battery 21 metrics, Thermal 4, Network 13, Storage 6, Speed Test 4 = 48 total.

#### InfoCard
Dismissible contextual education card.

| Property | Value |
|----------|-------|
| Background | `surfaceContainerHigh`, 16dp corners |
| Left accent border | 3dp, primary color (`Modifier.drawBehind`) |
| Icon | `Info` (20dp), primary color |
| Headline | `titleSmall`, `onSurface` |
| Body | `bodySmall`, `onSurfaceVariant` |
| Close | `Close` (16dp) in 48dp `IconButton` |
| Exit animation | 300ms fadeOut + shrinkVertically |
| Persistence | Dismissed state stored in DataStore |

Coverage: Battery 5 (including live notification info), Thermal 2, Network 2, Storage 2 = 11 total.

#### CrossLinkButton
Navigation link to related article.

| Property | Value |
|----------|-------|
| Background | `surfaceContainerHigh`, 16dp corners |
| Padding | 16dp |
| Label | `labelLarge`, primary color |
| Arrow | `ArrowForward` (18dp), primary color |
| Layout | Full-width, space-between |

---

## 8. Screen Structure Patterns

### Detail Screen Template

All detail screens follow this structure:

```
DetailTopBar(title, onBack)
PullToRefreshWrapper(isRefreshing, onRefresh)
  Column(Modifier.verticalScroll())
    HeroCard                        // ProgressRing + primary metric
      MetricPills (3 across)        // Secondary metrics
    DetailsCard                     // MetricRows with dividers
      CardSectionTitle
      MetricRow(label, value, divider, infoIcon?)
      ...
    InfoCard? (conditional)         // Dismissible education
    ChartSection? (Pro-gated)       // Period chips + TrendChart + Min/Avg/Max pills
    ActionSection? (if applicable)  // Cleanup tools, speed test, etc.
    RelatedArticlesSection          // CrossLinkButtons to Learn articles
```

### State Management

- **ViewModel → UI:** `StateFlow<ScreenUiState>` collected with `collectAsStateWithLifecycle()`
- **UI state pattern:** `sealed interface` with `Loading`, `Success(data)`, `Error(message: UiText)`
- **Info sheet state:** `var activeInfoSheet by rememberSaveable { mutableStateOf<String?>(null) }`
- **Dismissed cards:** `Set<String>` in UiState, persisted in DataStore

### UiText Pattern

ViewModels never hold Context. Use `UiText` sealed interface:
- `UiText.Resource(@StringRes id)` → string resource
- `UiText.Dynamic(value: String)` → runtime string
- Resolved in composables via `.resolve()` extension

---

## 9. Navigation

Push-based from single Home screen. No bottom nav, no tabs.

### Transitions

| Route | Enter | Exit |
|-------|-------|------|
| Standard | Slide + fade 300ms | Slide + fade 300ms |
| Fullscreen chart | Scale (0.94→1.0) + fade | Scale (1.0→0.94) + fade |
| Reduced motion | Instant (no animation) | Instant |

### Navigation Rules

- All routes use `navigateSingleTop` to prevent duplicate instances
- Detail screens receive `onNavigateTo*` callbacks, not NavController
- Nested routes (Charger, SpeedTest) navigate parent first, then child
- Fullscreen chart returns result via `savedStateHandle`
- Free-tier entry into `charger` or `app_usage` redirects to `pro_upgrade`

### Route Map

```
home
├── battery
│   ├── charger [PRO]
│   └── fullscreen_chart/{source}/{metric}/{period}
├── network
│   ├── speed_test
│   └── fullscreen_chart/{source}/{metric}/{period}
├── thermal
├── storage
│   └── cleanup/{type}
├── app_usage [PRO]
├── learn
│   └── learn/{articleId}
├── settings
└── pro_upgrade
```

---

## 10. Animations

All animations respect `MaterialTheme.reducedMotion` (instant when enabled).

| Component | Duration | Easing | Description |
|-----------|----------|--------|-------------|
| ProgressRing | 1200ms | `FastOutSlowInEasing` | Arc fill from 0 to target |
| MiniBar | 800ms | `FastOutSlowInEasing` | Bar fill from 0 to target |
| SegmentedBar | 800ms | `FastOutSlowInEasing` | Segments grow from 0 |
| TrendChart grid fade | 200ms | `FastOutSlowInEasing` | Grid, axes, quality zones fade in (Phase 1) |
| TrendChart sweep (entry) | 1000ms | `CubicBezier(0.25, 0.1, 0.25, 1)` | clipRect-based line/fill reveal + scan line (Phase 2) |
| TrendChart sweep (transition) | 800ms | `CubicBezier(0.25, 0.1, 0.25, 1)` | Same sweep, faster for data changes |
| TrendChart scan line fade | 300ms | Linear | Scan line fades during final 30% of sweep |
| TrendChart emphasis | 200ms | `FastOutSlowInEasing` | Glow dot + dashed line at last value (Phase 3) |
| TrendChart fade-out | 300ms | `FastOutSlowInEasing` | Previous data fades before new sweep |
| AreaChart sweep | 800ms | `CubicBezier(0.25, 0.1, 0.25, 1)` | Same oscilloscope sweep as TrendChart |
| AreaChart scan line | 240ms | Linear | Scan line fades during final 30% |
| LiveChart scroll | 150ms | `LinearEasing` | Smooth leftward shift on new data |
| LiveChart glow pulse | 300ms | Linear | Radius 8→5dp, alpha 0.5→0.3 on new data |
| AnimatedNumber | 200ms | Linear tween | Count-up/down |
| ConfidenceBadge | Spring | Damping 0.6, Medium stiffness | Pop-in on mount |
| HeatStrip pulse | 2000ms | `LinearEasing` | Alpha 0.7–1.0 loop (critical only) |
| InfoCard dismiss | 300ms | — | fadeOut + shrinkVertically |
| Nav transitions | 300ms | — | Slide + fade |
| Fullscreen chart | 300ms | — | Scale (0.94) + fade |
| Scroll reveal (web) | 650ms | cubic-bezier(0.16,1,0.3,1) | translateY(28px) → 0 |

**No card entrance animations** — cards appear instantly.

---

## 11. Accessibility

### Touch Targets
- Minimum 48dp for all interactive elements
- InfoIcon: 16dp icon in 48dp IconButton
- ListRow: 48dp minimum height

### Semantics
- ProgressRing: `progressBarRangeInfo` + optional `contentDescription`
- Charts: `Role.Image` + summary description
- StatusDot: semantics cleared (decorative)
- IconCircle: semantics cleared (decorative)
- SectionHeader / CardSectionTitle: heading role
- ConfidenceBadge: `Role.Image` with level description

### Reduced Motion
- Checked via `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`
- Provided as `LocalReducedMotion` CompositionLocal
- All animations skip to final state when enabled
- Navigation transitions become instant

### Color Independence
- Status colors always paired with text labels or icons
- Never rely on color alone to convey information

---

## 12. Pro Feature Gating

### Gated Features
- Extended history (battery, network)
- Charger comparison
- Per-app battery usage
- Home screen widgets
- CSV data export
- Thermal throttling logs
- Remaining charge time estimates

### UI Patterns

| Context | Component | Behavior |
|---------|-----------|----------|
| Home card | `GridCard(locked=true)` | Scrim overlay + ProBadgePill |
| Quick Tools row | ProBadgePill trailing | Badge on right side |
| Chart section | `ProFeatureCalloutCard` | Inline upgrade prompt |
| Full screen | `ProFeatureLockedState` | Centered lock screen |
| Route redirect | NavGraph logic | `charger`/`app_usage` → `pro_upgrade` |

### Debug Behavior
`BillingManager.initialize()` sets `_isProState = true` when `BuildConfig.DEBUG`, so all Pro features are visible during development.

---

## 13. Live Notification

Opt-in persistent notification showing real-time battery stats via `RealTimeMonitorService`.

### Settings UI (Settings → Live Notification)

| Element | Behavior |
|---------|----------|
| Master toggle | Starts/stops foreground service. Requests POST_NOTIFICATIONS permission on API 33+. Disabled by default. |
| Sub-toggles | Only visible when master is enabled. Each controls a line in the notification. |

**Configurable metrics:**

| Toggle | Default | Notification content |
|--------|---------|---------------------|
| Current (mA / W) | ON | `284 mA (1.8 W)` |
| Charging status | ON | `Charging` / `Discharging` |
| Temperature | ON | Shown in title: `78% · Discharging · 31.2°C` |
| Screen on/off stats | OFF | Screen tracking status |
| Remaining time | OFF | Remaining time estimate (discharge only) |

### Notification Format

**Collapsed (ContentText):** First body line only
**Expanded (BigTextStyle):** All enabled metric lines

**Title format:** `{level}% · {status} · {temp}` (temp only if enabled)

### Service Behavior

| Property | Value |
|----------|-------|
| Update interval | 5 seconds |
| Service type | `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` (API 34+) |
| Channel | `real_time_monitor`, `IMPORTANCE_LOW`, no badge |
| Persistence | Stays active when app is closed, until user disables toggle |
| Tap action | Opens MainActivity |

### Educational Support

`InfoCardCatalog.BatteryLiveNotification` — dismissible info card shown on Battery detail screen explaining the feature and directing to Settings.

---

## 14. Localization

- **English only** (`values/strings.xml`) — Finnish translations preserved in git history for future use
- All user-visible strings in `strings.xml` — no hardcoded text in composables
- Numeric formatting via `UiFormatters` (locale-aware)
- Temperature: user-selectable °C / °F via DataStore preference
- Date/time: `formatLocalizedDateTime()` with `DateTimePatternGenerator`

---

## 15. Data Formatting Conventions

Defined in `ui/common/UiFormatters.kt`.

| Data Type | Format | Example |
|-----------|--------|---------|
| Percentage (int) | `"%d%%"` | `78%` |
| Percentage (float) | `"%.1f%%"` | `78.5%` |
| Temperature | Value + unit | `31.2°C` or `88.2°F` |
| Storage size | `Formatter.formatFileSize()` | `1.2 GB` |
| Current | Integer + unit | `-284 mA` |
| Voltage | Integer + unit | `3 852 mV` |
| Power | 1 decimal + unit | `1.8 W` |
| Speed | Integer or 1 decimal + unit | `142 Mbps` |
| Latency | Integer + unit | `14 ms` |
| Date/time | Locale-aware skeleton | `09:41`, `Mar 22` |
| Duration | Hours + minutes | `12h 30m` |

---

## 16. File Structure Reference

```
ui/
├── theme/
│   ├── Color.kt          # Color tokens
│   ├── StatusColors.kt   # Status color system + threshold functions
│   ├── Type.kt           # Typography + numeric text styles
│   ├── Theme.kt          # darkColorScheme, CompositionLocals
│   ├── Shapes.kt         # Corner radii
│   └── Spacing.kt        # Spacing tokens
├── components/
│   ├── GridCard.kt
│   ├── ListRow.kt
│   ├── ActionCard.kt
│   ├── MetricPill.kt
│   ├── MetricRow.kt
│   ├── ProgressRing.kt
│   ├── MiniBar.kt
│   ├── StatusDot.kt
│   ├── ConfidenceBadge.kt
│   ├── SignalBars.kt
│   ├── HeatStrip.kt
│   ├── IconCircle.kt
│   ├── SectionHeader.kt
│   ├── CardSectionTitle.kt
│   ├── AnimatedNumber.kt
│   ├── PrimaryTopBar.kt
│   ├── DetailTopBar.kt
│   ├── TrendChart.kt       # Oscilloscope sweep, gradient line, improved fill, emphasis
│   ├── AreaChart.kt        # Oscilloscope sweep, improved fill
│   ├── LiveChart.kt        # Smooth scroll, glow pulse
│   ├── SegmentedBar.kt
│   ├── ExpandableChartContainer.kt
│   ├── ProBadgePill.kt
│   ├── ProFeatureCalloutCard.kt
│   ├── ProFeatureLockedState.kt
│   ├── PullToRefreshWrapper.kt
│   └── info/
│       ├── InfoIcon.kt
│       ├── InfoBottomSheet.kt
│       ├── InfoSheetContent.kt
│       ├── InfoCard.kt
│       ├── InfoCardCatalog.kt
│       └── CrossLinkButton.kt
├── common/
│   ├── UiText.kt         # Context-free text abstraction
│   └── UiFormatters.kt   # Formatting utilities
├── chart/
│   ├── ChartModels.kt       # Enums, data classes, constants
│   ├── ChartRenderModel.kt  # Pre-computed chart-ready data + builder functions
│   ├── ChartHelpers.kt      # Point extraction, axis builders, quality zones, qualityZoneColorForValue
│   └── ChartAccessibility.kt # Chart accessibility support
├── navigation/
│   ├── Screen.kt         # Route sealed class
│   └── NavGraph.kt       # Composition, transitions
├── home/
├── battery/
├── network/
├── thermal/
├── storage/
├── charger/
├── appusage/
├── learn/
├── settings/
├── fullscreen/
├── pro/
├── ads/
└── widget/
```
