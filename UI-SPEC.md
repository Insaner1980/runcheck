# UI-SPEC.md - runcheck UI Reference

Code-derived visual specification for the runcheck Android app.

This file describes only the UI that exists in the current Compose codebase.

Audit date: 2026-07-24

Primary source files:

- `app/src/main/java/com/runcheck/ui/theme/Color.kt`
- `app/src/main/java/com/runcheck/ui/theme/Theme.kt`
- `app/src/main/java/com/runcheck/ui/theme/Type.kt`
- `app/src/main/java/com/runcheck/ui/theme/Shapes.kt`
- `app/src/main/java/com/runcheck/ui/theme/Spacing.kt`
- `app/src/main/java/com/runcheck/ui/theme/UiTokens.kt`
- `app/src/main/java/com/runcheck/ui/theme/MotionTokens.kt`
- `app/src/main/java/com/runcheck/ui/theme/StatusColors.kt`
- `app/src/main/java/com/runcheck/ui/components/`
- `app/src/main/java/com/runcheck/ui/home/`
- `app/src/main/java/com/runcheck/ui/battery/`
- `app/src/main/java/com/runcheck/ui/network/`
- `app/src/main/java/com/runcheck/ui/thermal/`
- `app/src/main/java/com/runcheck/ui/storage/`
- `app/src/main/java/com/runcheck/ui/storage/cleanup/`
- `app/src/main/java/com/runcheck/ui/settings/`
- `app/src/main/java/com/runcheck/ui/pro/`
- `app/src/main/java/com/runcheck/ui/learn/`
- `app/src/main/java/com/runcheck/ui/insights/`
- `app/src/main/java/com/runcheck/ui/charger/`
- `app/src/main/java/com/runcheck/ui/tools/`
- `app/src/main/java/com/runcheck/ui/fullscreen/`
- `app/src/main/java/com/runcheck/ui/navigation/`

---

## 1. Product-Level Visual Constraints

The app is a Kotlin/Jetpack Compose Android app using Material 3 Expressive.
The visual system supports system-selected, light, and dark theme modes.

Current global constraints:

- System, light, and dark theme modes only.
- No AMOLED theme implementation.
- No dynamic color implementation.
- Material 3 color roles are supplied by `DarkRuncheckColorScheme` and
  `LightRuncheckColorScheme`.
- Cards are flat by default: zero tonal elevation and zero shadow elevation.
- General cards have no border.
- `ActionCard` and explicit outline buttons use 1dp outlines.
- Primary UI icons use `Icons.Outlined` or `Icons.AutoMirrored.Outlined`.
- Body text uses Manrope.
- Large numeric values and chart labels use JetBrains Mono.
- Shared dimensions live in `Spacing`, `UiTokens`, `Shapes`, `MotionTokens`,
  and theme extensions.
- Screen-level content is constrained through `ContentContainer` to a maximum
  width of 600dp.

---

## 2. Color System

Defined in `Color.kt` and exposed through `Theme.kt`.

### 2.1 Dark Base Colors

| Token | Hex | Current Material role or usage |
|---|---:|---|
| `BgPage` | `#0B1E24` | `background`, `surface`; full page background |
| `BgCard` | `#133040` | `surfaceContainer`; default card background |
| `BgCardDeep` | `#0D2530` | `heroCardColor`; hero card background |
| `BgCardAlt` | `#0F2A35` | `surfaceContainerLow`; alternate surfaces |
| `BgIconCircle` | `#1A3A48` | `surfaceContainerHigh`, `surfaceContainerHighest`, `surfaceVariant`; icon circles and tracks |
| `AccentTeal` | `#5DE4C7` | `secondary`; secondary accent |
| `AccentBlue` | `#4A9EDE` | `primary`; primary actions, links, main accent |
| `AccentAmber` | `#E8C44A` | `tertiary`; warning accent |
| `AccentOrange` | `#F5963A` | category accent |
| `AccentRed` | `#F06040` | `error`; destructive accent |
| `AccentLime` | `#C8E636` | storage/category accent |
| `AccentYellow` | `#F5D03A` | storage/category accent |
| `TextPrimary` | `#F4F7F8` | `onSurface`, `onBackground`; primary text |
| `TextSecondary` | `#B5C7CE` | `onSurfaceVariant`; labels/descriptions |
| `TextMuted` | `#7A949E` | `outline`, `outlineVariant`; muted text/dividers |
| `TextOnLime` | `#1A2E0A` | text on lime backgrounds |

### 2.2 Light Base Colors

| Token | Hex | Current Material role |
|---|---:|---|
| `LightBackground` | `#F4F7F8` | `background` |
| `LightSurface` | `#FFFFFF` | `surface` |
| `LightSurfaceContainerLow` | `#F0F4F5` | `surfaceContainerLow` |
| `LightSurfaceContainer` | `#E9EFF1` | `surfaceContainer` |
| `LightSurfaceContainerHigh` | `#DEE7EA` | `surfaceContainerHigh` |
| `LightSurfaceContainerHighest` | `#D4E0E4` | `surfaceContainerHighest` |
| `LightPrimary` | `#246A9F` | `primary` |
| `LightSecondary` | `#006B5A` | `secondary` |
| `LightTertiary` | `#795F00` | `tertiary` |
| `LightError` | `#B3261E` | `error` |
| `LightOnSurface` | `#16262C` | `onSurface` |
| `LightOnSurfaceVariant` | `#4E6570` | `onSurfaceVariant` |
| `LightOutline` | `#647A83` | `outline` |
| `LightOutlineVariant` | `#C0CDD1` | `outlineVariant` |

### 2.3 Material Color Scheme Mapping

`RuncheckTheme(themeMode)` selects the fixed light or dark scheme and passes it
to `MaterialExpressiveTheme` with `MotionScheme.expressive()`. Dynamic colors
are not used.

| Material role | Dark token | Light token |
|---|---|---|
| `background` | `BgPage` | `LightBackground` |
| `surface` | `BgPage` | `LightSurface` |
| `surfaceContainerLow` | `BgCardAlt` | `LightSurfaceContainerLow` |
| `surfaceContainer` | `BgCard` | `LightSurfaceContainer` |
| `surfaceContainerHigh` | `BgIconCircle` | `LightSurfaceContainerHigh` |
| `surfaceContainerHighest` | `BgIconCircle` | `LightSurfaceContainerHighest` |
| `primary` | `AccentBlue` | `LightPrimary` |
| `secondary` | `AccentTeal` | `LightSecondary` |
| `tertiary` | `AccentAmber` | `LightTertiary` |
| `error` | `AccentRed` | `LightError` |
| `onSurface` | `TextPrimary` | `LightOnSurface` |
| `onSurfaceVariant` | `TextSecondary` | `LightOnSurfaceVariant` |
| `outline` | `TextMuted` | `LightOutline` |
| `outlineVariant` | `TextMuted` | `LightOutlineVariant` |

Theme extensions:

- `MaterialTheme.heroCardColor`: `BgCardDeep` in dark mode,
  `LightSurfaceContainerLow` in light mode
- `MaterialTheme.chartColors`: centralized appearance-specific grid, axis,
  line, fill, selected-point, and glow tokens
- `MaterialTheme.iconCircleColor`: `surfaceContainerHighest`
- `MaterialTheme.cardStrokeColor`: `outlineVariant.copy(alpha = 0.35f)`
- `runcheckCardColors()`: `CardDefaults.cardColors(containerColor = surfaceContainer)`
- `runcheckHeroCardColors()`: `CardDefaults.cardColors(containerColor = heroCardColor)`
- `runcheckCardElevation()`: `CardDefaults.cardElevation(defaultElevation = 0.dp)`
- `runcheckOutlinedCardBorder()`: 1dp `outlineVariant.copy(alpha = 0.35f)`

### 2.4 Status Colors

Defined in `StatusColors.kt`.

| Status token | Opaque base/container |
|---|---|
| `healthy` | `#006B57` |
| `fair` | `#795F00` |
| `poor` | `#9C4E00` |
| `critical` | `#B3261E` |
| `neutral` | `#4E6570` |
| `unavailable` | `#647A83` |

Each status exposes an explicit opaque container and a foreground selected from
`#0B1E24` and `#FFFFFF` by the greater WCAG contrast ratio.

Thresholds:

| Domain | Healthy | Fair | Poor | Critical |
|---|---|---|---|---|
| Percent status | `>= 75` | `>= 50` | `>= 25` | `< 25` |
| Temperature C | `< 35` | `35..39.999` | `40..44.999` | `>= 45` |
| Storage used % | `< 75` | `75..84.999` | `85..94.999` | `>= 95` |
| Signal quality | Excellent/Good | Fair | Poor | No signal |

Confidence badge colors:

| Confidence | Background | Text |
|---|---|---|
| Accurate/High | `AccentBlue` | `BgPage` |
| Estimated/Low | `AccentAmber` | `BgPage` |
| Unavailable | `TextMuted` | `TextPrimary` |

Storage media category colors from `categoryColor()`:

| Category | Color |
|---|---|
| Image | `AccentBlue` |
| Video | `AccentLime` |
| Audio | `AccentYellow` |
| Document | `onSurfaceVariant` |
| Download | `AccentBlue.copy(alpha = 0.6f)` |
| APK | `onSurfaceVariant` |
| Other | `outline` |

---

## 3. Typography

Defined in `Type.kt`.

Font families:

- Material typography: Manrope from `R.font.manrope`.
- Numeric and chart typography: JetBrains Mono from `R.font.jetbrains_mono`.

### 3.1 Material Typography Scale

| Style | Size | Weight | Line height | Letter spacing |
|---|---:|---|---:|---:|
| `displayLarge` | 48sp | Bold | default | `-0.04.em` |
| `displayMedium` | 36sp | Bold | default | default |
| `displaySmall` | 28sp | SemiBold | default | default |
| `headlineLarge` | 20sp | SemiBold | default | default |
| `headlineMedium` | 16sp | SemiBold | default | default |
| `headlineSmall` | 14sp | SemiBold | default | default |
| `titleLarge` | 20sp | Medium | default | default |
| `titleMedium` | 16sp | Medium | default | default |
| `titleSmall` | 14sp | Medium | default | default |
| `bodyLarge` | 15sp | Normal | default | default |
| `bodyMedium` | 14sp | Normal | default | default |
| `bodySmall` | 13sp | Normal | default | default |
| `labelLarge` | 12sp | SemiBold | default | `0.08.em` |
| `labelMedium` | 10sp | SemiBold | default | default |
| `labelSmall` | 10sp | Medium | default | default |

### 3.2 Numeric Extensions

All styles below use JetBrains Mono.

| Extension | Base or size | Weight | Line height | Letter spacing |
|---|---:|---|---:|---:|
| `numericHeroDisplayTextStyle` | 64sp | Bold | 64sp | `-3.sp` |
| `numericHeroDisplayUnitTextStyle` | 28sp | SemiBold | 28sp | default |
| `numericHeroValueTextStyle` | 48sp displayLarge | Bold | 48sp | inherited |
| `numericHeroLevelTextStyle` | hero value | Bold | 48sp | `-2.sp` |
| `numericHeroLargeValueTextStyle` | 54sp | Bold | 54sp | `-3.sp` |
| `numericHeroUnitTextStyle` | headlineLarge | SemiBold | 20sp | default |
| `numericRingValueTextStyle` | 32sp | Bold | 32sp | `-3.sp` |
| `numericSpeedHeroValueTextStyle` | 40sp | Bold | 44sp | `-3.sp` |
| `numericMetricDisplayTextStyle` | 48sp | Bold | 48sp | `-3.sp` |
| `chartAxisTextStyle` | 12sp | labelSmall weight | 14sp | default |
| `chartTooltipTextStyle` | 13sp | bodySmall weight | 16sp | default |

Common usage:

- Hero values on Home/Battery/Thermal/Storage use `numericHeroDisplayTextStyle`
  or `numericHeroLargeValueTextStyle`.
- Detail metric hero values use `numericMetricDisplayTextStyle`.
- Trend chart axes and tooltips use chart numeric styles.
- `MetricRow`, `MetricPill`, app usage size values, cleanup file sizes, and
  settings numeric values use JetBrains Mono where code applies
  `MaterialTheme.numericFontFamily`.

---

## 4. Spacing, Sizing, Shape, and Elevation

### 4.1 Spacing Tokens

Defined in `Spacing.kt`.

| Token | Value |
|---|---:|
| `xxs` | 2dp |
| `xs` | 4dp |
| `sm` | 8dp |
| `md` | 12dp |
| `base` | 16dp |
| `lg` | 24dp |
| `xl` | 32dp |

Common spacing patterns:

- Screen horizontal padding: usually `base` 16dp.
- Home cards and hero content: commonly `base` 16dp or `lg` 24dp.
- Detail screen vertical item gap: usually `md` 12dp.
- Card internal padding: usually `base` 16dp.
- Hero cards with stronger emphasis: 24dp horizontal/vertical in Home and
  Thermal, 16dp in Battery/Network/Storage.
- Footer spacer on scrolling detail pages: usually `xl` 32dp.
- Filter chip row gaps: `sm` 8dp, except fullscreen controls use `xs` 4dp.

Raw geometry constants also exist for visual primitives and charts:

- 3dp: `SignalBars` corner radius, segmented status gap, cleanup MiniBar
  height.
- 4dp: status bar/track heights, progress bars, group/header micro-spacing.
- 5dp: embedded chart tick length.
- 6dp: embedded chart y-label gap, segmented bar corner.
- 8dp: fullscreen chart y-label gap/tick length, tooltip horizontal padding,
  chart expand button padding, charger comparison bar height.
- 10dp: ProgressRing default stroke; first signal bar height.
- 12dp: segmented bar height, signal bar width.
- 16dp: card radius and many standard paddings.
- 24dp: major screen/hero spacing.
- 32dp: large bottom/footer spacing.

### 4.2 UI Tokens

Defined in `UiTokens.kt`.

| Token | Value |
|---|---:|
| `touchTarget` | 48dp |
| `iconTiny` | 12dp |
| `iconSmall` | 16dp |
| `iconMedium` | 18dp |
| `iconLarge` | 20dp |
| `iconXLarge` | 24dp |
| `iconCircle` | 44dp |
| `iconCircleInner` | 22dp |
| `compactIconCircle` | 36dp |
| `dialogIcon` | 64dp |
| `celebrationIcon` | 80dp |
| `primaryButtonHeight` | 56dp |
| `compactButtonHeight` | 52dp |
| `badgeHorizontalPadding` | 12dp |
| `badgeVerticalPadding` | 4dp |
| `proBadgeHorizontalPadding` | 8dp |
| `proBadgeVerticalPadding` | 3dp |
| `outlineWidth` | 1dp |
| `outlineAlpha` | `0.35f` |
| `lockScrimAlpha` | `0.18f` |
| `proBadgeBackgroundAlpha` | `0.12f` |

### 4.3 Shapes

Defined in `Shapes.kt`.

| Shape | Value | Usage |
|---|---|---|
| `small` | 8dp rounded corners | small surfaces, file thumbnails, normal range block |
| `medium` | 16dp rounded corners | regular cards and medium Material surfaces |
| `large` | 16dp rounded corners | cards, dialogs, buttons |
| `extraLarge` | 28dp rounded corners | hero cards and large Material surfaces |
| `RuncheckPillShape` | 50 percent rounded | pills and circular/fully rounded elements |
| `NavigationIndicatorShape` | pill | navigation indicator |
| `BadgeShape` | pill | badges |
| `BottomSheetShape` | top start 28dp, top end 28dp | modal bottom sheets |

### 4.4 Elevation, Borders, Dividers

- `runcheckCardElevation()` is 0dp.
- Most `Card` surfaces use `shape = MaterialTheme.shapes.large`,
  `colors = runcheckCardColors()`, and `elevation = runcheckCardElevation()`.
- Hero cards use `runcheckHeroCardColors()`.
- Standard dividers use `outlineVariant.copy(alpha = 0.35f)`.
- `ActionCard` uses `runcheckOutlinedCardBorder()`:
  1dp `outlineVariant.copy(alpha = 0.35f)`.
- `Surface` wrappers used for bottom bars and overlays use 0dp tonal elevation.

---

## 5. Motion and Reduced Motion

Defined in `MotionTokens.kt`, `Theme.kt`, and component code.

### 5.1 Reduced Motion

`LocalReducedMotion` is calculated from
`Settings.Global.ANIMATOR_DURATION_SCALE == 0f`.

Theme extension:

- `MaterialTheme.reducedMotion`

Current behavior:

- Navigation transitions become `EnterTransition.None` and
  `ExitTransition.None` when reduced motion is true.
- ProgressRing, MiniBar, SegmentedBar, SpeedTestHero, CleanupBottomBar,
  CleanupSuccessOverlay, HeatStrip pulse, InfoCard exit, InfoBottomSheet detail
  expand, TrendChart, AreaChart, LiveChart, and ConfidenceBadge either snap,
  remove, or shorten animation when reduced motion is true.

### 5.2 Motion Tokens

| Token | Value | Usage |
|---|---:|---|
| `SHORT` | 200ms | short text/value and emphasis transitions |
| `MEDIUM` | 300ms | navigation, live chart glow, cleanup bars/overlay |
| `SWEEP` | 800ms | mini bars, segmented bars, area chart, trend chart updates |
| `RING` | 1200ms | progress rings |
| `CONTINUOUS` | 2000ms | looping battery wave, thermal critical pulse |
| `SCROLL` | 150ms | live chart append scroll |
| `FULLSCREEN_ENTER_SCALE` | 260ms | fullscreen chart enter scale |
| `FULLSCREEN_ENTER_FADE` | 220ms | fullscreen chart enter fade/pop exit fade |
| `FULLSCREEN_EXIT` | 180ms | fullscreen chart exit/pop enter fade |
| `SPEED_GAUGE` | 1700ms | speed test idle pulse |
| `SPEED_SWEEP` | 1800ms | speed test ping sweep |
| `SPEED_RESULT` | 700ms | speed test progress/result |

Easings:

- `EaseOut`: `FastOutSlowInEasing`
- `SweepEasing`: `CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)`
- Speed ping rotation uses `LinearEasing`.

### 5.3 Navigation Motion

Default route transitions in `NavGraph.kt`:

- Forward enter: slide into container from Start direction plus fade in,
  `MotionTokens.MEDIUM` 300ms.
- Forward exit: slide out toward Start direction plus fade out, 300ms.
- Pop enter: slide into container from End direction plus fade in, 300ms.
- Pop exit: slide out toward End direction plus fade out, 300ms.

Fullscreen chart route:

- Enter: `scaleIn(initialScale = 0.94f)` over 260ms plus fade in over 220ms.
- Exit: `scaleOut(targetScale = 1.02f)` over 180ms plus fade out over 180ms.
- Pop enter: fade in over 180ms.
- Pop exit: `scaleOut(targetScale = 0.96f)` over 220ms plus fade out over
  220ms.

---

## 6. Layout Foundation

### 6.1 ContentContainer

`ContentContainer` wraps primary screen content:

- Outer `Box`: `fillMaxWidth()`.
- Alignment: `Alignment.TopCenter`.
- Inner content: `fillMaxWidth().widthIn(max = 600.dp)`.
- Used by Home, detail screens, settings, learn, insights, Pro, charger, and
  fullscreen-related surfaces.

### 6.2 Top Bars

`PrimaryTopBar`:

- Used on Home.
- `fillMaxWidth()`.
- `statusBarsPadding()`.
- Padding: start 4dp, end 8dp, top 2dp, bottom 4dp.
- Background: `MaterialTheme.colorScheme.surface`.
- Horizontal arrangement: `spacedBy(8.dp)`.
- Title style: `headlineSmall`.
- Optional menu icon button; absent button leaves a 4dp spacer.

`DetailTopBar`:

- Used on detail/subscreens.
- `fillMaxWidth()`.
- `statusBarsPadding()`.
- Padding: start 4dp, end 4dp, top 2dp, bottom 4dp.
- Background: `MaterialTheme.colorScheme.surface`.
- Back `IconButton`.
- Center title: `titleLarge`, heading semantics.
- Trailing spacer width: 48dp.

### 6.3 Scroll Patterns

Common detail pattern:

- `ExpressiveDetailScaffold(fillMaxSize())`.
- Collapsible `LargeFlexibleTopAppBar`.
- `ContentContainer`.
- The scaffold owns horizontal 16dp content padding.
- Vertical item spacing usually 12dp.
- Top spacer usually 8dp.
- Bottom spacer usually 32dp.

Screen-specific scroll containers:

- Home: `Column.verticalScroll`, horizontal 16dp padding, bottom nav padding.
- Battery: `PullToRefreshWrapper` plus vertical scroll column.
- Network: `PullToRefreshWrapper` plus vertical scroll column.
- Storage: `PullToRefreshWrapper` plus vertical scroll column.
- Thermal: `PullToRefreshWrapper` plus `LazyColumn`.
- Settings: vertical scroll column.
- Learn and App Usage: `LazyColumn`.
- Cleanup: `Scaffold` plus `LazyColumn`.
- Charger: expressive detail scaffold plus `LazyColumn`.

---

## 7. Shared Components

### 7.1 GridCard

Used on Home grid metrics.

- Card shape: large 16dp.
- Colors: `runcheckCardColors()`.
- Elevation: 0dp.
- Optional left status strip.
- Status strip: 4dp wide, 16dp corner radius, full card height.
- Start padding: 16dp when status strip exists, otherwise 12dp.
- End padding: 12dp.
- Top/bottom padding: 16dp.
- Main icon: `IconCircle(size = 36.dp, iconSize = 20.dp)`.
- Icon/text gap: 8dp.
- Title: `titleMedium`, `onSurfaceVariant`.
- Measurement subtitle (default): `headlineSmall`, JetBrains Mono.
- Prose subtitle: `bodyMedium`; the Tools bento cards select this style while
  existing numeric Home cards retain the measurement default.
- Status label: `labelMedium`.
- Locked overlay: `scrim.copy(alpha = 0.18f)`.
- Pro badge overlay: top end, 12dp padding.

### 7.2 ListRow

- Minimum height: 48dp.
- Optional clickable row with merged semantics.
- Vertical padding: 12dp.
- Optional leading icon: 20dp.
- Leading icon gap: 12dp.
- Label: `bodyLarge`, `onSurface`.
- Value: `bodyMedium`, `onSurfaceVariant`.
- Optional trailing arrow surface:
  - Shape: `RuncheckPillShape`.
  - Color: `surfaceVariant.copy(alpha = 0.35f)`.
  - Inner padding: 2dp.
  - Icon: 20dp.
- Gap before trailing arrow: 4dp.

### 7.3 ActionCard

- Full-width `Card`.
- Shape: large 16dp.
- Colors: `runcheckCardColors()`.
- Border: 1dp `outlineVariant.copy(alpha = 0.35f)`.
- Elevation: 0dp.
- Row padding: 16dp.
- Leading `IconCircle`: 40dp (`iconCircle - xs`), icon default 22dp.
- Icon/text gap: 8dp.
- Text column gap: 2dp.
- Title: `titleSmall`.
- Subtitle: `bodySmall`.
- Trailing action: `TextButton` with auto-mirrored outlined arrow icon 18dp.

### 7.4 MetricPill

- Column with merged semantics.
- Label row: `bodySmall`, `onSurfaceVariant`.
- Optional `InfoIcon`.
- Label/value gap: 2dp.
- Value: `titleMedium`, default `onSurface`, optional custom color.

### 7.5 MetricRow

- Full-width row.
- Optional clickable/copyable row.
- Label: `bodyMedium`, `onSurfaceVariant`.
- Optional `InfoIcon`.
- Value: `titleLarge`, JetBrains Mono, SemiBold.
- Optional `maxLines = 1` with ellipsis and end alignment.
- Optional divider:
  - Top spacer: 8dp.
  - Divider: `outlineVariant.copy(alpha = 0.35f)`.
- Copyable rows write to clipboard and show a Toast.

### 7.6 ProgressRing

- Canvas-based circular progress.
- Default stroke: 10dp.
- Track color default: `MaterialTheme.iconCircleColor`.
- Progress color default: `primary`.
- Progress animation: `animateFloatAsState` over 1200ms with
  FastOutSlowIn easing.
- Reduced motion duration: 0ms.
- Track arc: full circle.
- Progress arc: starts at 270 degrees, clockwise.
- Stroke cap: round.
- Optional semantics: content description and `ProgressBarRangeInfo`.

### 7.7 MiniBar

- Default height: 6dp.
- Default track: `iconCircleColor`.
- Default fill: `primary`.
- Shape: `RuncheckPillShape`.
- Animation: 800ms FastOutSlowIn.
- Reduced motion duration: 0ms.
- Optional clear semantics content description.
- Cleanup file rows override height to 3dp.
- Cleanup bottom bar overrides height to 4dp.

### 7.8 SegmentedBar and Legend

`SegmentedBar`:

- Data model: label, value, formatted value, color.
- Height: 12dp.
- Segment gap: 2dp.
- Corner radius: 6dp.
- Background: `surfaceVariant.copy(alpha = 0.5f)`.
- Minimum segment width: 4dp.
- Overall reveal animation: 800ms with `MotionTokens.EaseOut`.
- Reveal restarts from zero when the segment data changes.
- Reduced motion: snap to final state.
- Semantics role: image with joined labels.

`SegmentedBarLegend`:

- Vertical spacing: 4dp.
- Dot size: 8dp.
- Dot/label gap: 8dp.
- Text: `bodySmall`.

### 7.9 SegmentedStatusBar

- Default height: 6dp.
- Segment gap: 3dp.
- Inactive alpha: 0.2.
- Labels appear below after 4dp.
- Current segment: last segment whose range start is <= current value.
- Inactive label color: `onSurfaceVariant.copy(alpha = 0.5f)`.

### 7.10 SignalBars

- Five bars.
- Heights: 10dp, 18dp, 26dp, 36dp, 48dp.
- Width: 12dp.
- Gap: 4dp.
- Corner radius: 3dp.
- Active counts:
  - Excellent: 5.
  - Good: 4.
  - Fair: 3.
  - Poor: 2.
  - No signal: 0.
- Inactive color: `surfaceVariant.copy(alpha = 0.3f)`.
- Semantics describes signal label and active bar count.

### 7.11 HeatStrip

- Height: 24dp.
- Shape: large 16dp.
- Default temperature range: 15C to 50C.
- Critical threshold for pulse: above 42C.
- Gradient aligns status colors around 35C, 40C, and 45C transitions.
- Indicator: on-surface/white circle, radius 8dp, clamped inside strip.
- Critical pulse:
  - The gradient remains stable; only the indicator halo animates.
  - Halo radius animates 9dp to 13dp while alpha fades 0.26 to 0.08.
  - Reverse repeat.
  - Duration 2000ms.
  - Easing: `MotionTokens.EaseOut`.
  - Disabled when reduced motion is true.

### 7.12 StatusDot

- Default size: 8dp.
- Shape: `CircleShape`.
- Semantics cleared.

### 7.13 ConfidenceBadge

- Shape: `RuncheckPillShape`.
- Padding: horizontal 12dp, vertical 4dp.
- Text style: `labelMedium`.
- Background/text colors from confidence color mapping.
- Scale animation:
  - Starts at 0.92.
  - Springs to 1.
  - Damping ratio: 0.8.
  - Stiffness: medium.
  - Reduced motion snaps to 1.

### 7.14 AnimatedFloatText

- `animateFloatAsState` target value.
- Default duration: 200ms.
- Reduced motion duration: 0ms.
- Uses `formatDecimal`.
- Default text style: `bodyLarge`.
- Default color: `onSurface`.

### 7.15 IconCircle

- Default size: 44dp.
- Default icon size: 22dp.
- Shape: `CircleShape`.
- Background: `MaterialTheme.iconCircleColor`.
- Default tint: `onSurfaceVariant`.
- Semantics cleared.

### 7.16 SectionHeader and CardSectionTitle

- Uppercase text.
- Style: `labelLarge`.
- Color: `onSurfaceVariant`.
- Heading semantics.

### 7.17 InfoIcon

- `IconButton`.
- Touch target: 48dp.
- Icon: auto-mirrored outlined help.
- Icon size: 16dp.
- Tint: `onSurfaceVariant`.

### 7.18 InfoBottomSheet

- `ModalBottomSheet`.
- `skipPartiallyExpanded = true`.
- Container: `surfaceContainer`.
- Shape: `BottomSheetShape`, top corners 28dp.
- Max content height: 60 percent of screen height.
- Content column:
  - `fillMaxWidth()`.
  - `heightIn(max = screenHeight * 0.6)`.
  - Vertical scroll.
  - Horizontal padding: 24dp.
  - Bottom padding: 32dp.
- Title: `titleMedium`.
- Explanation: `bodyMedium`.
- Normal range block:
  - Shape: small 8dp.
  - Background: `surfaceContainerHigh`.
  - Padding: 12dp.
- Detail expand/collapse:
  - `AnimatedVisibility`.
  - Expand/shrink vertically over 300ms.
  - No transition when reduced motion is true.
  - Detail top padding: 4dp.

### 7.19 InfoCard

- `AnimatedVisibility` for removal.
- Enter: none.
- Exit: fade out plus shrink vertically, 300ms.
- Reduced motion: no transition.
- Surface shape: large 16dp.
- Background: `surfaceContainerHigh`.
- Full width.
- Left accent line:
  - Width: 3dp.
  - Starts 16dp from top.
  - Ends 16dp before bottom.
  - Color: `primary`.
- Row padding: 16dp.
- Icon: 20dp.
- Icon/text gap: 12dp.
- Title: `titleSmall`.
- Body: `bodySmall`.
- Optional "learn more" area:
  - Minimum height: 48dp.
  - Vertical padding: 12dp.
- Close button: 48dp, icon 16dp.

### 7.20 CrossLinkButton

- `Surface` with click handler.
- Shape: large 16dp.
- Background: `surfaceContainerHigh`.
- Full width.
- Row padding: 16dp.
- Space between label and arrow.
- Label: `labelLarge`, primary.
- Arrow icon: 18dp, primary.

### 7.21 Pro Components

`ProBadgePill`:

- Shape: small 8dp.
- Background: `primary.copy(alpha = 0.12f)`.
- Padding: horizontal 8dp, vertical 3dp.
- Row gap: 4dp.
- Lock icon: 12dp.
- Text style: `labelMedium`, primary.

`ProFeatureCalloutCard`:

- Full-width card.
- Shape: large 16dp.
- Background: `surfaceContainer`.
- Elevation: 0dp.
- Padding: 16dp.
- Vertical gap: 8dp.
- Message: `bodyMedium`, `onSurfaceVariant`.
- Action: `OutlinedButton` with 1dp primary border and primary content.

`ProFeatureLockedState`:

- Full-size centered `Box`.
- Column width: full.
- Column padding: 24dp.
- Alignment: centered.
- Vertical gap: 12dp.
- Title: `titleLarge`.
- Message: `bodyMedium`.
- Action: `OutlinedButton` with 1dp primary border.

### 7.22 Connected Selection Rows

`EnumFilterChipRow`:

- Delegates to `ExpressiveSingleChoiceSelector`.
- Uses an equal-width Material 3 `SingleChoiceSegmentedButtonRow`.
- Long labels are single-line and ellipsized.

`HistoryPeriodFilterChipRow`:

- Same visual behavior.
- Omits `SINCE_UNPLUG` unless `includeSinceUnplug = true`.

Fullscreen chart controls:

- Two full-width connected selector rows.
- Period selector first, metric selector second.
- Gap: 4dp.

### 7.23 Expressive Wrappers

`ExpressiveComponents.kt` keeps reusable Material 3 Expressive surfaces behind
one component boundary:

- `ExpressiveDetailScaffold` combines a collapsible
  `LargeFlexibleTopAppBar`, constrained content, and 16dp horizontal content
  padding. It is the implemented shell for Battery, Network, Thermal, Storage,
  Speed Test, Charger Comparison, App Usage, Learn, Export, and Pro Upgrade,
  plus protected tool entry states.
- `ExpressiveSingleChoiceSelector<T>` renders connected, equal-width
  single-choice options when there are at most four choices. Longer sets use
  the same connected segmented shape in a horizontal scroll container, with
  104dp minimum option width, up to two label lines, and a 48dp minimum touch
  target.
- `RuncheckActionCard` is the 28dp hero action surface with a 1dp ActionCard
  outline, 24dp content padding, an outlined icon, and a 56dp primary CTA.
- `InfoBanner` delegates deterministic, dismissible information to the shared
  info-card behavior.
- `StatusPill` maps a text label and optional outlined icon to an opaque semantic
  status container/foreground pair. It never conveys status through color alone.
- `MeasuredHeroValue` pairs a hero value with its unit and the shared
  `ConfidenceBadge`. Raw Android platform telemetry is conservatively presented
  as estimated, or unavailable when the platform value is missing.
- `SecondaryActionLink` is the compact text-and-arrow action for a related
  destination that must not compete with the screen's primary action.
- `LearnTopicLink` is the shared full-width related-content link.
- `ExpressiveEmptyState` provides an icon, title, and supporting message; its
  icon minimum size comes from `UiTokens.touchTarget`.
- `RuncheckLoadingIndicator` owns the experimental morphing loading indicator.
  Reduced motion renders a fixed determinate shape instead of continuous morphing.
- `RuncheckWavyProgress` owns the circular wavy progress API. It uses the
  1200ms ring token and removes wave amplitude and speed under reduced motion.
- `AppDisplayName` trims a real app label and otherwise derives a readable
  package-tail fallback. Callers may preserve their existing text style; App
  Usage uses `bodyMedium`.
- `ChartTheme` owns the chart color composition local selected by
  `RuncheckTheme`; `TrendChart` consumes its line, fill, grid, axis,
  selected-point, and glow roles.

---

## 8. Chart System

### 8.1 TrendChart

`TrendChart` is the primary history chart.

Inputs:

- `data: List<Float>`
- `chartHeight`, default 200dp
- `lineColor`, default `MaterialTheme.chartColors.line`
- `fillColor`, default `MaterialTheme.chartColors.fill`
- Optional y-labels, x-labels, grid, quality zones, tooltip formatter,
  expand handler, presentation mode.

Animation constants:

| Constant | Value |
|---|---:|
| Grid fade | 200ms |
| Initial sweep | 1000ms |
| Transition sweep | 800ms |
| Initial scan fade delay | 700ms |
| Initial scan fade duration | 300ms |
| Transition scan fade delay | 560ms |
| Transition scan fade duration | 240ms |
| Emphasis | 200ms |
| Fade out | 300ms |
| Transition overlap | 200ms |
| Scan line start alpha | 0.5 |

Reduced motion:

- Grid, sweep, and emphasis snap to final.
- Scan line and fade-out alpha become 0.

Embedded chart style:

| Property | Value |
|---|---:|
| Chart padding | 8dp |
| Y-label gap | 6dp |
| X-label top padding | 4dp |
| Gesture edge guard | 24dp |
| Line stroke | 2dp |
| Grid stroke | 1dp |
| Tick length | 5dp |
| Point marker radius | 0dp |
| Selected outer point radius | 4dp |
| Selected inner point radius | 2dp |
| Axis style | `chartAxisTextStyle` |
| Tooltip style | `chartTooltipTextStyle` |

Fullscreen chart style:

| Property | Value |
|---|---:|
| Chart padding | 16dp |
| Y-label gap | 8dp |
| X-label top padding | 8dp |
| Gesture edge guard | 28dp |
| Line stroke | 3dp |
| Grid stroke | 1.5dp |
| Tick length | 8dp |
| Point marker radius | 3.5dp |
| Selected outer point radius | 6dp |
| Selected inner point radius | 3dp |

Drawing details:

- Grid color: `outlineVariant.copy(alpha = 0.2f)`.
- Tick color: grid color alpha multiplied by 0.75.
- Tooltip background: `surfaceContainer`.
- Tooltip cursor line: primary alpha 0.5.
- Quality zone fill: zone color alpha multiplied by grid alpha.
- Area fill top alpha: interpolates 0.08 to 0.30 based on average normalized Y.
- Area fill bottom alpha: 0.02.
- Sweep head: 1.5dp line with a 32dp draw-phase illumination trail.
- Last value emphasis:
  - Glow radius 6dp, color alpha `0.3 * emphasis`.
  - Dot radius 3dp, alpha `emphasis`.
  - Dashed horizontal line stroke 1dp, alpha `0.4 * emphasis`.
  - Dash pattern: 4dp on, 4dp off.
- Tooltip:
  - Horizontal padding 8dp.
  - Vertical padding 4dp.
  - Above-point offset 8dp when space allows.
  - Below-point offset 12dp otherwise.
  - Corner radius 6dp.
  - Border stroke 1dp, line color alpha 0.4.
- Expand button:
  - Aligns top end.
  - Outer padding 8dp.
  - Background `surfaceContainerHigh.copy(alpha = 0.9f)`.
  - Shape small 8dp.
  - Tonal/shadow elevation 0dp.
  - Row padding: horizontal 8dp, vertical 4dp.
  - Icon: outlined open-in-full, 16dp.
  - Icon/label gap: 4dp.
  - Text: `labelMedium`.

### 8.2 AreaChart

- Returns without drawing when data size is below 2.
- Fills available size.
- Optional content description with image role.
- Animation:
  - Sweep 0 to 1 over 800ms with `SweepEasing`.
  - Scan alpha 0.5 to 0 after 560ms over 200ms.
  - Reduced motion snaps sweep and hides scan.
- Stroke: 1.5dp, alpha 0.7.
- Vertical padding: 10 percent of chart height.
- Fill top alpha: interpolates 0.08 to 0.25.
- Fill bottom alpha: 0.02.
- Sweep head: 1.5dp line with the shared 32dp illumination trail.

### 8.3 LiveChart

- Header row:
  - Label left: `bodySmall`.
  - Current value right: `titleMedium` with JetBrains Mono.
- Header/chart gap: 4dp.
- Default chart height: 80dp.
- Default max points: 60.
- If data size is below 2, it shows an empty box.
- Y-range padded by 10 percent.
- Grid: three horizontal lines at 25, 50, and 75 percent.
- Grid stroke: 0.5dp, alpha 0.2.
- Data points are right-aligned.
- Area fill alpha: 0.20 to 0.02.
- Line stroke: 1.5dp, round cap/join.
- Current dot:
  - Glow radius settles from 11dp to 5dp.
  - Glow alpha settles from 0.40 to 0.12.
  - Duration 300ms.
  - Inner dot radius settles from 2.5dp to 3dp.
- Append scroll:
  - New point offset animates to 0 over 150ms with `MotionTokens.EaseOut`.
  - Reduced motion snaps to 0.
- Semantics role: image.

### 8.4 Chart Data Limits

Defined in `ChartModels.kt`.

| Constant | Value |
|---|---:|
| `MAX_HISTORY_CHART_POINTS` | 300 |
| `MAX_SESSION_CHART_POINTS` | 240 |
| `MAX_NETWORK_HISTORY_POINTS` | 300 |
| `MAX_THERMAL_HISTORY_POINTS` | 300 |
| `MAX_STORAGE_HISTORY_POINTS` | 300 |
| Fullscreen history max | 600 |
| Fullscreen session max | 480 |

### 8.5 Quality Zones

Defined in `ChartHelpers.kt`.

Battery level zones:

- 50..100 healthy, alpha 0.06.
- 20..50 fair, alpha 0.06.
- 0..20 critical, alpha 0.06.

Battery temperature zones:

- 0..35 healthy, alpha 0.06.
- 35..40 fair, alpha 0.06.
- 40..45 poor, alpha 0.06.
- 45..60 critical, alpha 0.06.

Signal zones:

- -50..0 healthy, alpha 0.07.
- -60..-50 healthy, alpha 0.05.
- -70..-60 fair, alpha 0.06.
- -80..-70 poor, alpha 0.06.
- -120..-80 critical, alpha 0.06.

Thermal zones:

- 0..35 healthy, alpha 0.06.
- 35..42 fair, alpha 0.06.
- 42..60 critical, alpha 0.06.

Storage used zones:

- 0..74.999 healthy, alpha 0.08.
- 75..84.999 fair, alpha 0.08.
- 85..94.999 poor, alpha 0.08.
- 95..100 critical, alpha 0.08.

### 8.6 Chart Stats Row

- Horizontal row.
- Full width.
- Top aligned.
- Gap: 16dp.
- Usually contains min/avg/max `MetricPill` items with equal weights.

### 8.7 Chart Accessibility Summary

`rememberChartAccessibilitySummary()` builds chart content descriptions from:

- Chart title.
- Optional time context.
- Minimum value.
- Maximum value.
- Latest value.
- Trend direction: increasing, decreasing, or stable.

Trend threshold:

- Data size below 2: stable.
- If range >= 10, minimum threshold is 1.
- If range >= 1, minimum threshold is 0.25.
- Otherwise minimum threshold is 0.05.
- Effective threshold is `max(range * 0.08f, minimumThreshold)`.

---

## 9. Screen Specifications

### 9.1 Home

Structure:

- Lightweight `PrimaryTopBar` with the lowercase app name and no Settings action.
- `ContentContainer`.
- Vertical scroll column.
- Horizontal padding: 16dp.
- Top spacer: 4dp.
- Section gap: 12dp.
- Bottom spacer: 32dp.
- Wide layout threshold: `screenWidthDp >= 600`.

First viewport:

- A stale-monitoring `InfoBanner` appears only while `monitoringStale` is true.
- `HomeHealthHero` uses the 28dp hero shape, hero surface color, and 24dp
  padding.
- The overall score is rendered inside a 148dp determinate
  `RuncheckWavyProgress`; the semantic description reports the score out of 100.
- A text `StatusPill` identifies Healthy, Fair, Poor, or Critical alongside the
  localized timestamp captured when the four live device-state flows emit.
- `ConfidenceBadge` shows the actual `batteryState.currentMa.confidence` level
  and is labelled as battery-current confidence rather than overall-score
  confidence.
- The hero does not repeat the four category breakdown rows.

Home grid:

- Wide layout: one row of four `GridCard`s with 8dp gaps.
- Compact layout: two rows; row gap 12dp, column gap 8dp.
- Grid cards use equal weights.
- Grid order is Battery, Network, Thermal, Storage.
- Each grid card pairs its semantic icon tint with text: charge/health,
  signal quality, temperature band, or storage-used percentage.

Home insights card:

- Home passes at most one ranked insight to the shared card and always exposes
  the Insights link.
- With no active insight, the section and View all action stay visible and the
  shared `ExpressiveEmptyState` replaces the row.
- The unseen count remains visible next to the section heading.
- Only `TrialHomeCard` for an active trial follows the insight. Home no longer
  owns quick tools, charger comparison, purchased-Pro, or expired-trial cards.

### 9.2 Battery Detail

Structure:

- `ExpressiveDetailScaffold` with collapsible large top app bar.
- `PullToRefreshWrapper`.
- Scroll column.
- Horizontal padding: 16dp, owned by the scaffold.
- Vertical item gap: 12dp.
- Top spacer: 8dp.
- Bottom spacer: 32dp.

Battery panel:

- Full-width card.
- Shape: large.
- Background: `surfaceContainer`.
- Elevation: 0dp.
- Column padding: 16dp.
- Vertical gap: 8dp.

Battery hero:

- Hero background: `BgCardDeep`.
- Padding: 16dp.
- Vertical gap: 8dp.
- Header: `SectionHeader`.
- Header/body gap: 16dp.
- Main row gap: 24dp.
- Ring size: 100dp.
- Ring stroke: 6dp.
- Ring track: `surfaceVariant.copy(alpha = 0.3f)`.
- Ring progress: status color for battery percent.
- Value: `numericHeroDisplayTextStyle`.
- Unit: `numericHeroDisplayUnitTextStyle`.
- Unit padding: start 2dp, bottom 12dp.
- Status text: `bodyMedium`.
- Metrics row gap: 16dp, usually three equal `MetricPill`s.

Battery history panel:

- Uses `BatteryPanel`.
- Contains card title, metric chips, period chips, load error if present,
  chart, and stats.
- Selection rows use connected `EnumFilterChipRow`.
- Chart label: `labelLarge`, primary.
- `TrendChart` uses embedded presentation with grid and battery quality zones.
- Expanded fullscreen action is available for history chart.
- Pro-locked state uses `ProFeatureLockedState`.

Battery history preview placeholder:

- Box height: 148dp.
- Shape: large.
- Background: `surfaceVariant.copy(alpha = 0.12f)`.
- Inner padding: top 16dp, bottom/start/end 8dp.
- Blur: RenderEffect radius 18f on API 31+, alpha fallback otherwise.
- AreaChart animation disabled.
- Pro badge overlays the preview.

Battery session graph panel:

- Uses `BatteryPanel`.
- Contains metric chip row and session window chip row.
- Uses `TrendChart` and `ChartStatsRow`.
- Fullscreen chart route supports battery session.

Other battery panels:

- Overview, charging, remaining time, footer, screen usage, sleep analysis,
  and statistics sections use the same `BatteryPanel` card grammar.
- Dividers use outlineVariant alpha 0.35.
- Live charts are used when enough session points exist.
- At most one eligible information banner is shown. Selection is deterministic:
  severity first, then catalog order; dismissed banners and the global tips
  preference are respected.
- Charger Comparison is a compact secondary action link, not a full-width
  primary panel or button.
- The footer links to the prefiltered Battery topic in Learn.

### 9.3 Network Detail

Structure:

- `ExpressiveDetailScaffold` with collapsible large top app bar.
- `PullToRefreshWrapper`.
- Scroll column.
- Horizontal padding: 16dp, owned by the scaffold.
- Vertical item gap: 12dp.
- Top spacer: 8dp.
- Bottom spacer: 32dp.

Network panel:

- Same visual grammar as BatteryPanel:
  - Large card.
  - `surfaceContainer`.
  - 0dp elevation.
  - Padding 16dp.
  - Vertical gap 8dp.

Network hero:

- Hero background: `BgCardDeep`.
- Padding: 16dp.
- Vertical gap: 8dp.
- Quality row uses `headlineLarge` with status color and `SignalBars`.
- SignalBars height in this context is 32dp.
- Numeric metrics row gap: 24dp.
- Signal dBm and latency ms use `numericMetricDisplayTextStyle` 48sp.
- Unit style: `numericHeroDisplayUnitTextStyle`.
- Both raw Android values use `MeasuredHeroValue` with an estimated or
  unavailable `ConfidenceBadge`; the UI does not invent an accurate rating.
- Optional signal `LiveChart`.
- Metrics row gap: 16dp, with bandwidth and band pills. Latency is not repeated
  as an unbadged raw value below the confidence-aware hero metric.

Signal history:

- `NetworkPanel`.
- `CardSectionTitle`.
- Metric chip row.
- History period chip row.
- Load error if present.
- Chart label: period and metric.
- `TrendChart` with signal quality zones.
- Expanded fullscreen action is available.
- Stats row below chart.

Speed test summary:

- `NetworkPanel`.
- Shows title and latest result metrics if available.
- Metrics use `MetricPill`.
- Empty state uses body text and action copy.

Network overview and details:

- `ConnectionDetailsCard` uses `NetworkPanel`.
- `MetricRow` rows show WiFi, cellular, bandwidth, IP, DNS, MTU, and general
  connection details.
- Copyable values include BSSID, IP addresses, DNS values, and IPv6 rows.
- Long IPv6 rows use one line with ellipsis and copy behavior.

Network tools:

- Pro signal history is shown when Pro is active.
- Otherwise `ProFeatureCalloutCard` is shown.
- Speed test summary and one prefiltered Network Learn topic link appear in the
  tools area.
- At most one eligible information banner is shown using the shared
  severity-then-catalog-order policy.

WiFi name help:

- Uses info/help card styling and muted body text.

### 9.4 Speed Test

Structure:

- `ExpressiveDetailScaffold` with collapsible large top app bar.
- Scroll column.
- Horizontal padding: 16dp, owned by the scaffold.
- Vertical gap: 12dp.
- Center horizontal alignment.
- Top spacer: 8dp.
- Bottom notice uses `labelSmall` and bottom 32dp spacing.

Network context panel:

- Card with 16dp padding.
- Uses `MetricPill` rows.

Speed test hero:

- Wrapper box:
  - Top padding 8dp.
  - Size 286dp.
- Main circular canvas size: 248dp.
- Background pulse circle size: 248dp.
- Idle pulse:
  - Scale 0.96 to 1.04.
  - Alpha 0.12 to 0.26.
  - Duration 1700ms.
  - Reverse repeat.
  - Easing `MotionTokens.EaseOut`.
  - Infinite transition exists only while the hero is idle.
  - Disabled when reduced motion is true.
- Non-idle background alpha: 0.08.
- Gauge stroke: 16dp.
- Outer stroke: 2dp.
- Track arc: start 135 degrees, sweep 270 degrees.
- Idle decorative sweep arc: start 130 degrees, sweep 220 degrees.
- Ping sweep:
  - Rotating arc start around 145 degrees.
  - Sweep 112 degrees.
  - Duration 1800ms linear.
  - Infinite transition exists only during the Ping phase.
- Failed arc: start 135 degrees, sweep 72 degrees, error color.
- Download/upload/completed arc:
  - Start 135 degrees.
  - Sweep equals 270 degrees multiplied by progress.
  - Progress animation: 700ms FastOutSlowIn unless reduced motion.
- Inner accent arc:
  - Inset 14dp.
  - Stroke 2dp.
  - Alpha 0.18.
- Center text:
  - Idle: `titleMedium`, SemiBold, primary.
  - Running/completed value: `numericSpeedHeroValueTextStyle` 40sp.
  - Unit: `labelMedium`.
  - Progress/status label under value.
- Semantics role: button.
- While a test is running, the action surface morphs toward a rounded rectangle,
  exposes "Stop speed test", and calls `NetworkViewModel.stopSpeedTest()`.
- Loading uses `RuncheckLoadingIndicator`.

Speed metrics card:

- Card padding: 24dp horizontal, 16dp vertical.
- Live region semantics.
- Vertical gap: 16dp.
- Two metric rows with 12dp horizontal gap.
- Divider between rows.
- Download/upload metrics use primary color.
- Ping/jitter use default text color.

Dialogs and state cards:

- Cellular warning uses `AlertDialog` with large shape.
- Failure card uses error container colors and 16dp text padding.
- Empty history card uses default card with 16dp text padding.

Latest result card:

- Padding: 24dp horizontal, 16dp vertical.
- Vertical gap: 12dp.
- Header has section title and timestamp.
- Connection type badge uses icon size 16dp and gap 6dp.
- Metrics row gap: 12dp.
- Optional server metric appears after divider.

History item:

- Card row padding: 16dp horizontal, 12dp vertical.
- Left side contains connection icon 14dp, label, and timestamp.
- Right value row gap: 16dp.
- Values use `titleSmall` with numeric font.
- Download/upload values use primary.

### 9.5 Thermal Detail

Structure:

- `ExpressiveDetailScaffold` with collapsible large top app bar.
- `PullToRefreshWrapper`.
- `LazyColumn`.
- Horizontal padding: 16dp, owned by the scaffold.
- Vertical gap: 12dp.
- Top spacer: 8dp.
- Bottom spacer: 32dp.

Thermal hero:

- Hero background: `BgCardDeep`.
- Padding: 24dp horizontal, 24dp vertical.
- Center aligned.
- Header: `SectionHeader`.
- Header/value gap: 24dp.
- Temperature value: `numericHeroDisplayTextStyle`.
- Unit: `numericHeroDisplayUnitTextStyle`.
- The raw Android battery temperature uses `MeasuredHeroValue` with an
  estimated or unavailable `ConfidenceBadge`.
- Thermal band uses `StatusPill` with the matching semantic tone.
- Optional session min/max: `bodySmall`, top gap 4dp.
- Segmented status bar after 24dp.
- Gap after status bar: 8dp.
- Thermal segments:
  - Cool/healthy: 0..35.
  - Normal/fair: 35..40.
  - Warm/poor: 40..45.
  - Critical: 45..60.

Thermal metrics card:

- Default card.
- Padding: 16dp.
- Vertical gap: 16dp.
- First row: CPU temperature and headroom, gap 12dp.
- Divider: outlineVariant alpha 0.35.
- Second row: thermal status and throttling, gap 12dp.
- Live charts follow behind dividers when data exists.

Thermal history card:

- Default card.
- Padding: 16dp.
- Vertical gap: 8dp.
- Title: `CardSectionTitle`.
- Metric chip row.
- Period chip row.
- Load error if present.
- `TrendChart` with thermal quality zones.
- `ChartStatsRow`.

Throttling section:

- Empty state card uses row padding 16dp and gap 12dp.
- Empty state uses healthy `StatusDot`.
- Event item card uses 16dp padding and 4dp vertical gap.
- Event detail row gap: 8dp.
- Pro-locked thermal logs use `ProFeatureCalloutCard`.
- At most one eligible information banner is shown using the shared
  severity-then-catalog-order policy.
- The footer links to the prefiltered Thermal topic in Learn.

### 9.6 Storage Detail

Structure:

- `ExpressiveDetailScaffold` with collapsible large top app bar.
- `PullToRefreshWrapper`.
- Scroll column.
- Horizontal padding: 16dp, owned by the scaffold.
- Vertical gap: 12dp.
- Top spacer: 8dp.
- Bottom spacer: 32dp.

Storage panel:

- Same panel grammar as network/battery:
  - Large card.
  - `surfaceContainer`.
  - 0dp elevation.
  - Padding 16dp.
  - Vertical gap 8dp.

Media permission card:

- Card padding: 16dp.
- Vertical gap: 8dp.
- Title: `titleMedium`.
- Body: `bodyMedium`.
- Action: `TextButton`.

Storage hero:

- Hero background: `BgCardDeep`.
- Padding: 16dp.
- Vertical gap: 8dp.
- Header: `SectionHeader`.
- Header/body gap: 16dp.
- Main row gap: 24dp.
- Ring size: 100dp.
- Ring stroke: 6dp.
- Ring track: `surfaceVariant.copy(alpha = 0.3f)`.
- Ring progress: status color for storage used percent.
- Usage percent: `numericHeroDisplayTextStyle`.
- Unit padding: start 2dp, bottom 12dp.
- Usage summary: `bodyMedium` with numeric font.
- Free/fill detail: `bodySmall`.
- Optional usage `LiveChart` after 16dp.
- Metrics row gap: 16dp.
- Cache storage is explicitly described as a read-only measurement: runcheck can
  measure it but cannot clear other apps' caches.
- A text action after the cache note opens the Storage Learn article that
  explains the Android sandbox boundary and system-owned cache controls.

Media breakdown:

- `StoragePanel`.
- Title.
- Gap before bar: 8dp.
- `SegmentedBar`.
- Gap before legend: 8dp.
- `SegmentedBarLegend`.
- Category colors are the exact mapping from section 2.3.

Storage history:

- Default card.
- Padding: 16dp.
- Vertical gap: 8dp.
- Metric chip row.
- History period chip row.
- Load error if present.
- `TrendChart` with storage quality zones.
- `ChartStatsRow`.

Cleanup tools:

- Section header.
- Tool list gap: 8dp.
- Uses `ActionCard`.
- Large files use poor status tint.
- Old downloads use primary.
- APK uses APK category color (`onSurfaceVariant`).
- Trash action appears only when API and data allow it.

Storage details and quick actions:

- Details card uses `MetricRow`.
- Dividers separate rows except the final row.
- Quick action card uses `ListRow`s with dividers.
- Quick actions open Android Storage settings and runcheck's App info screen;
  no in-app action claims to clear another app's cache.
- Removable-storage card uses the same panel grammar when present and shows `Unknown` when a detected volume's capacity cannot be read.
- At most one eligible information banner is shown using the shared
  severity-then-catalog-order policy.
- The footer links to the prefiltered Storage topic in Learn.

### 9.7 Cleanup Screens

Structure:

- `Scaffold`.
- `DetailTopBar`.
- `ContentContainer`.
- Body horizontal padding: 16dp.
- Optional filter chip row:
  - Top spacer 8dp.
  - Full-width connected single-choice selector.
  - Bottom spacer 8dp.

Loading/scanning/deleting:

- Centered `RuncheckLoadingIndicator`.
- Live region semantics for scanning/deleting.

Unsupported state:

- Top spacer 12dp.
- Card shape large.
- Background: `surfaceContainer`.
- Padding: 16dp.
- Vertical gap: 8dp.

Results list:

- `LazyColumn`.
- Item spacing: 0dp.
- Header label:
  - Text style `labelLarge`.
  - Color `onSurfaceVariant`.
  - Vertical padding 8dp.
- Group separators: outlineVariant alpha 0.35.
- Bottom spacer for action bar: 80dp.

Category group header:

- Full-width row.
- Clickable role button.
- Heading semantics and expand/collapse actions.
- Vertical padding: 8dp.
- Status dot: category color, 8dp.
- Dot/icon gap: 8dp.
- Expand icon: 20dp.
- Icon/label gap: 4dp.
- Label: `titleSmall`.
- Count: `bodySmall`, gap 2dp from label.
- Total size: `bodySmall` with numeric font.
- Checkbox uses primary checked color.

File list item:

- Full-width row.
- Clickable role checkbox.
- Merged semantics and toggleable state.
- Vertical padding: 8dp.
- Checkbox first.
- Checkbox/thumbnail gap: 4dp.
- Thumbnail/icon container:
  - Size 48dp.
  - Clip shape small 8dp.
  - Images use `ContentScale.Crop`.
  - Fallback `IconCircle` size 48dp, icon 24dp.
- Thumbnail/text gap: 8dp.
- Filename: `titleSmall`, one line, ellipsis.
- Subtitle: `bodySmall`, category plus relative date.
- Subtitle/bar gap: 4dp.
- Relative size `MiniBar`:
  - Height 3dp.
  - Fill: category color.
- Text/size gap: 8dp.
- File size: `bodyMedium` with numeric font.
- Item dividers after first item have start padding 56dp.

Cleanup bottom bar:

- `AnimatedVisibility`.
- Enter: slide in vertically from full height over 300ms.
- Exit: slide out vertically over 300ms.
- Reduced motion: no transition.
- Surface background: `surfaceContainer`.
- Tonal elevation: 0dp.
- Top divider: outlineVariant alpha 0.35.
- Content respects `WindowInsets.navigationBars`.
- Content padding: 16dp.
- Live region: polite.
- Projection text:
  - `bodySmall` with numeric font.
  - Color `onSurfaceVariant`.
  - Custom content description.
- Text/bar gap: 4dp.
- Projection MiniBar:
  - Height 4dp.
  - Fill: healthy status color.
- Bar/button gap: 8dp.
- The action is hosted by `HorizontalFloatingToolbar`.
- Delete button:
  - Full width.
  - Primary container and on-primary content.

Cleanup success overlay:

- `AnimatedVisibility`.
- Enter fade in over 300ms from alpha 0.
- Exit fade out over 300ms.
- Reduced motion: no transition.
- Full-screen box.
- Background: `surfaceContainer.copy(alpha = 0.95f)`.
- Live region: assertive.
- Center column.
- Success icon:
  - Outlined check circle.
  - Size 64dp.
  - Tint healthy.
  - Content description includes freed size.
- Icon/text gap: 16dp.
- Freed size text:
  - `titleLarge` with numeric font.
  - Color `onSurface`.

### 9.8 App Usage

Structure:

- `ExpressiveDetailScaffold`.
- Loading/error/locked states centered.
- Success content uses `LazyColumn`.
- Horizontal padding: 16dp, owned by the scaffold.
- Vertical item gap: 8dp.
- Top spacer: 8dp.
- Bottom: 12dp spacer then 32dp spacer.

Permission, error, and empty cards:

- Shape: large.
- Background: `surfaceContainer`.
- Elevation: 0dp.
- Padding: 16dp.
- Vertical gap: 8dp.
- Title: `titleMedium`.
- Body: `bodyMedium`.
- Action: `TextButton`.

App usage item:

- Card shape large.
- Background: `surfaceContainer`.
- Elevation: 0dp.
- Padding: 16dp.
- Vertical gap: 4dp.
- Header row uses space-between alignment.
- App icon:
  - Loaded bitmap: 40dp.
  - Fallback `IconCircle`: 40dp, icon 20dp, healthy tint.
- Icon/text gap: 8dp.
- App label: `bodyMedium`.
- `AppDisplayName` is the primary label and derives a readable package-tail
  fallback when the app label is missing.
- The package name remains visible only as a `labelSmall` technical detail.
- Time text: `bodySmall`, `onSurfaceVariant`.
- Label/time column gap: 2dp.
- Percent: `bodyMedium`, `onSurfaceVariant`.
- Progress bar:
  - Full width.
  - Height 4dp.
  - Track `surfaceVariant`.
  - Semantics includes content description and progress range.
- Per-app estimated mAh drain is not shown. Foreground duration and usage share
  do not support a defensible per-app drain claim.
- A connected Usage / Not used selector is present.
- Not used offers 30/60/90-day connected filter buttons and loads only after
  Pro access is confirmed.
- Each candidate keeps the app label as the primary line and package name as a
  technical secondary line. Missing labels fall back to the package name.
- Last-use copy distinguishes a recorded date from "No recorded use in N days";
  an absent UsageStats row is not presented as proof that the app was never used.
- Storage size is shown when available. Per-app size failures keep the remaining
  list visible and add a concise size-specific partial-results note; label-only
  failures instead explain that package names are used.
- Each candidate exposes a 48dp-minimum uninstall action that launches the
  system `ACTION_DELETE` flow. Returning to the screen forces a fresh query.

### 9.8a Weekly Report

- `ExpressiveDetailScaffold` owns the title, back action, and horizontal padding.
- Free users see the shared `ProFeatureLockedState`; the report use case is not
  invoked until Pro access is available.
- The hero card shows the previous completed local Monday-to-Monday interval,
  monitored-day/sample coverage, and Available/Estimated/Unavailable wording.
- Battery shows observed level changes, defensible discharge-rate estimates, and
  health change only when data exists. It does not claim per-app mAh drain.
- Storage, thermal, and speed sections state unavailable data explicitly.
- App ranking uses foreground duration only.
- Weekly notifications use the low-importance Reports channel and open the
  Tools parent before pushing Weekly Report.

### 9.9 Charger Comparison

Structure:

- `ExpressiveDetailScaffold`.
- Extended floating action button when profiles exist:
  - Container primary.
  - Content on-primary.
  - Icon: outlined add.
- `LazyColumn`.
- Horizontal padding: 16dp.
- Vertical gap: 12dp.
- Top spacer: 8dp.
- Bottom spacer: 32dp.

InfoCardContainer:

- Full-width card.
- Shape: large.
- Background: `surfaceContainer`.
- Elevation: 0dp.
- Padding: 16dp.

Selected charger card:

- Title: `titleSmall`.
- Selected charger name: `titleMedium`, SemiBold.
- Helper/status body: `bodyMedium`, `onSurfaceVariant`.
- Small internal spacers: 4dp and 8dp.
- Clear action: `TextButton`.

Empty state:

- Shared `ExpressiveEmptyState` with an outlined battery-charging and add icon
  illustration group.
- Inline full-width Add charger CTA.
- Charging sessions remain automatic; there is no manual Start session action.

Historical comparison:

- Title: `titleMedium`.
- Subtitle: `bodyMedium`, `onSurfaceVariant`.
- Rows sorted by comparison value descending.
- Label/value row uses space between.
- Charger name: `bodyMedium`.
- Comparison label: `bodySmall`, `onSurfaceVariant`.
- Bar track:
  - Height 8dp.
  - Background: `surfaceContainerHighest`.
  - Shape: `RuncheckPillShape`.
- Bar fill:
  - Height 8dp.
  - Color: primary.
  - Shape: `RuncheckPillShape`.
- Per-row bottom gap: 8dp.

Charger card:

- Full-width card.
- Shape: large.
- Background: `surfaceContainer`.
- Elevation: 0dp.
- Padding: 16dp.
- Header row space between.
- Name: `titleMedium`.
- Selected and active labels use the shared semantic `StatusPill`.
- Text column gap: 2dp.
- Delete icon button uses outlined delete, tint `onSurfaceVariant`.
- Spacers: 4dp after header, 4dp after sessions, 8dp before action.
- Session count: `bodyMedium`, `onSurfaceVariant`.
- Latest result: `bodyMedium`, `onSurface`.
- Last used: `bodySmall`, `onSurfaceVariant`.
- Action: full-width `OutlinedButton`.

Dialogs:

- Add charger dialog: `AlertDialog`, large shape, single-line
  `OutlinedTextField`.
- Delete charger dialog: `AlertDialog`, large shape.

### 9.10 Insights Screen

Structure:

- Top-level `PrimaryTopBar`; there is no back action.
- Loading state: centered `RuncheckLoadingIndicator` inside `ContentContainer`.
- Error state:
  - Centered column.
  - Padding 24dp.
  - Text: `bodyMedium`, `onSurfaceVariant`.
- Success state:
  - `ContentContainer`.
  - Vertical scroll column.
  - Horizontal padding 16dp.
  - Vertical gap 8dp.
  - Top spacer 8dp.
  - `SectionHeader` plus an equal-width `ExpressiveSingleChoiceSelector` for
    All and Important.
  - Important includes High and Medium priorities.
  - Count text follows the selected filter.
  - Empty results use `ExpressiveEmptyState`.
  - Insight rows use the shared `InsightRow`.
  - Bottom spacer 32dp.

Insight rows pair the priority color with a High/Medium/Low text
`StatusPill`, show the localized generated timestamp, and keep existing
dismissal and destination behavior. `LifecycleStartStopEffect` tells
`InsightsViewModel` when the top-level screen is started/stopped. Only while
started are visible unseen rows submitted as seen; insights arriving off-screen
remain unseen until the next visible transition, preserving the app-shell badge.

### 9.10a Tools

- Top-level `PrimaryTopBar`; there is no back action.
- A 28dp `RuncheckActionCard` dominates the first viewport with the outlined
  Speed icon, M-Lab NDT7 context, and a 56dp "Run speed test" CTA.
- The Device tools section is a two-by-two `GridCard` bento:
  Storage cleanup, Charger Comparison, App Battery Usage, and Weekly Report.
- Bento subtitles are prose and therefore use `bodyMedium`, not the numeric
  measurement subtitle style.
- All four Pro tools stay visible for Free users, expose the standard locked
  semantics and `PRO` badge, and open their existing protected destination.
- Storage cleanup opens the protected `cleanup/LARGE_FILES` destination
  directly. Before constructing `CleanupScreen`, the entry waits for Pro state
  and shows `ProFeatureLockedState` to Free users.
- Learn uses `LearnTopicLink`.
- Export is a secondary `ListRow`; Free users see the same `ProBadgePill` and
  reach the standard Export locked state.

### 9.11 Learn

Learn list:

- `ExpressiveDetailScaffold`.
- Connected topic selector with exactly Battery, Network, Thermal, Storage, and
  Privacy in that order.
- Privacy contains a dedicated data-and-privacy article. The existing general
  health score, software-versus-hardware, and background-monitoring articles
  are not relabeled as Privacy. They remain directly addressable and are also
  reachable from an always-visible `General` section below the selected topic.
- Detail-screen links open Learn with the matching topic preselected.
- `LazyColumn`.
- Horizontal padding: 16dp, owned by the scaffold.
- Vertical gap: 8dp.
- Top spacer: 4dp.
- Section headers use `CardSectionTitle`.
- Between sections: 4dp spacer.
- Bottom spacer: 24dp.

Learn article card:

- Card shape: large.
- Background: `surfaceContainer`.
- Elevation: 0dp.
- Full width.
- Clickable.
- Padding: 16dp.
- Title: `titleSmall`, `onSurface`.
- Title/preview gap: 4dp.
- Preview: `bodySmall`, `onSurfaceVariant`, max 2 lines, ellipsis.
- Preview/read-time gap: 8dp.
- Read time: `labelSmall`, `onSurfaceVariant`.

Learn article detail:

- `DetailTopBar` title is article title or Learn fallback.
- Missing article: text with 16dp padding.
- Article body uses `LazyColumn`.
- Content padding:
  - Start 16dp.
  - Top 8dp.
  - End 16dp.
  - Bottom 32dp.
- Vertical block gap: 8dp.
- Heading block: `titleSmall`, `onSurface`.
- Paragraph block: `bodyMedium`, `onSurface`.
- Bullet/numbered list:
  - Column gap: 4dp.
  - Marker and item are rendered in one text string.
- Optional cross-link uses `CrossLinkButton`.

Related articles:

- Column gap: 8dp.
- Header: `SectionHeader`.
- Each related item uses `CrossLinkButton`.

### 9.12 Settings

Structure:

- Top-level `PrimaryTopBar`; there is no back action.
- `ContentContainer`.
- Vertical scroll column.
- Horizontal padding: 16dp.
- Vertical section gap: 12dp.
- Top spacer: 8dp.
- Bottom spacer: 32dp.

SettingsCard:

- Shape: large.
- Background: `surfaceContainer`.
- Elevation: 0dp.
- Padding: 16dp.

SettingsDivider:

- `outlineVariant.copy(alpha = 0.35f)`.

SettingsRadioRow:

- Full width.
- Minimum height: 48dp.
- Disabled alpha: 0.38.
- Selectable role: radio button.
- Vertical padding: 4dp.
- Radio button followed by label.
- Label start padding: 8dp.
- Label style: `bodyMedium`.

SettingsToggle:

- Full width.
- Minimum height: 48dp.
- Disabled alpha: 0.38.
- Toggleable role: switch.
- Horizontal arrangement: space between.
- Title: `bodyMedium`, `onSurface`.
- Optional description: `bodySmall`, `onSurfaceVariant`.
- Switch at trailing edge.

SettingsSlider:

- Full-width column.
- Header row space between.
- Label: `titleSmall`, `onSurface`.
- Current value: `bodyMedium` with numeric font, primary.
- Slider:
  - Value range equals allowed value index range.
  - Steps equal `allowedValues.size - 2`.
  - Thumb: primary.
  - Active track: primary.
  - Inactive track: `surfaceVariant.copy(alpha = 0.5f)`.
  - Semantics content description contains label and value.

SettingsValueRow:

- Full width.
- Minimum height: 48dp.
- Vertical padding: 4dp.
- Space-between row.
- Label: `bodyMedium`, `onSurface`.
- Value: `bodyMedium`, default `onSurfaceVariant` or custom color.

SettingsNavigationRow:

- Full width.
- Minimum height: 48dp.
- Clickable role button.
- Merged semantics.
- Vertical padding: 8dp.
- Label: `bodyMedium`.
- Arrow: auto-mirrored outlined keyboard arrow, 18dp,
  `onSurfaceVariant`.

Sections:

- Display:
  - System, Light, and Dark use `ExpressiveSingleChoiceSelector`.
  - The selection updates DataStore immediately through `SettingsViewModel`.
  - Temperature unit and info-card controls remain below the theme selector.
- Monitoring:
  - `CardSectionTitle`.
  - Note text: `bodyMedium`, `onSurfaceVariant`.
  - 4dp spacers around note.
  - Radio rows separated by dividers.
  - Battery optimization text uses healthy or error status depending state.
  - Help row uses `SettingsNavigationRow`.
- Live notification:
  - Master `SettingsToggle`.
  - When enabled, divider, `titleSmall` label with top 4dp padding, 4dp
    spacer, then individual toggles.
- Notifications:
  - Master toggle.
  - Divider before child toggles.
  - Child toggles disabled with 0.38 alpha when master disabled.
  - Muted warning: top spacer 8dp, `bodySmall`, error color, horizontal 4dp
    padding, clickable.
- Alert thresholds:
  - Three `SettingsSlider`s separated by dividers.
  - Reset button appears only when thresholds differ from defaults.
- Data:
  - Description: `bodySmall`, `onSurfaceVariant`.
  - Retention rows separated by dividers.
  - Export is a navigation row to the protected Export destination.
  - Reset/clear actions use `SettingsNavigationRow`.
  - Clear all label uses error color.
- Widgets:
  - Pro users see an Available status.
  - Free users see a visible Pro badge and upgrade navigation.
- Pro:
  - Status row uses healthy color when active.
  - Active Pro shows thank-you body text.
  - Non-Pro billing state shows full-width primary `Button`.
  - Restore purchase uses `SettingsNavigationRow`.
- Measurement:
  - Device name: `titleMedium`.
  - Summary: `bodyMedium`, `onSurfaceVariant`.
  - Metric rows use `MetricPill` with 12dp gaps.
  - Reliable status uses healthy; unreliable uses error.
- About:
  - Version text: `bodyMedium`, `onSurfaceVariant`.
  - Rows separated by dividers.
  - Open-source licenses dialog text max height: 420dp, bodySmall,
    vertical scroll.
- Debug insights:
  - Appears after About and only when debug insight actions exist.
  - Uses `RuncheckLoadingIndicator` while an action is running.
  - Actions remain full-width outlined buttons and a text button.

The implemented section order is Display, Monitoring, Notifications (including
thresholds and live notification), Data, Widgets, Pro, Device Capabilities,
About, and debug-only insights.

Settings dialogs:

- Reset thresholds, reset tips, clear speed tests, notification permission
  denied, and clear-all-data dialogs use `AlertDialog`.
- Shape: large.
- Confirm action usually `Button`.
- Dismiss action usually `TextButton`.

### 9.13 Pro Upgrade and Trial

Pro upgrade screen:

- `ExpressiveDetailScaffold`.
- Upgrade content:
  - Vertical scroll.
  - Horizontal padding: 16dp, owned by the scaffold.
  - `navigationBarsPadding()`.
  - Center aligned.
  - Top spacer: 32dp.
  - Headline: `headlineMedium`, Bold, centered.
  - Subtitle: `bodyLarge`, `onSurfaceVariant`, centered.
  - Subtitle/features gap: 32dp.
  - Feature rows:
    - Full width.
    - Gap: 16dp.
    - Icon: 24dp, primary.
    - Text: `bodyLarge`, `onSurface`.
    - Gap after each row: 16dp.
  - Buy button:
    - Full width.
    - Height: 56dp.
    - Shape: large.
    - Container: primary.
    - Text: `titleMedium`, Bold.
  - Button/status gap: 8dp.
  - Pending purchase text: `bodySmall`, poor status, centered.
  - Error text: `bodySmall`, error color, centered.
  - One-time purchase note: `bodySmall`, `onSurfaceVariant`, centered.
  - Bottom spacer: 32dp.

Pro active content:

- Full-size centered column.
- Padding: 24dp.
- Check icon: 64dp, primary.
- Title: `headlineSmall`.
- Body: `bodyLarge`, `onSurfaceVariant`.
- Spacers: 16dp then 8dp.

Purchase thank-you content:

- Full-size centered column.
- Padding: 24dp.
- Check-circle icon: 80dp, primary.
- Icon/title gap: 24dp.
- Title: `headlineMedium`, Bold, centered.
- Title/body gap: 12dp.
- Body: `bodyLarge`, `onSurfaceVariant`, centered.
- Body/button gap: 32dp.
- Continue button:
  - Full width.
  - Height: 56dp.
  - Shape: large.
  - Primary container.
  - Text: `titleMedium`, Bold.

Trial welcome sheet:

- Modal bottom sheet.
- `skipPartiallyExpanded = true`.
- Container: `surfaceContainer`.
- Shape: `BottomSheetShape`.
- Content padding:
  - Horizontal 24dp.
  - Bottom 32dp.
- Center aligned.
- Title: `headlineSmall`, Bold.
- Subtitle: `bodyMedium`, `onSurfaceVariant`.
- Title/subtitle gap: 8dp.
- Subtitle/features gap: 24dp.
- Feature row gap: 12dp.
- Feature icon: outlined check circle, 20dp, primary.
- Feature text: `bodyMedium`.
- Gap after each feature: 12dp.
- Start button:
  - Full width.
  - Height: 52dp.
  - Shape: large.
  - Primary container.
  - Text: `titleMedium`, Bold.

Trial expiration modal:

- Full-screen `Dialog` with `usePlatformDefaultWidth = false`.
- Surface fills screen.
- Background: `background`.
- Pane title semantics.
- Content padding: 24dp.
- Center aligned.
- Lock icon: 64dp, `onSurfaceVariant`.
- Icon/title gap: 24dp.
- Title: `headlineMedium`, Bold, centered.
- Title/body gap: 16dp.
- Body: `bodyLarge`, `onSurfaceVariant`, centered.
- Body/button gap: 32dp.
- Purchase button:
  - Full width.
  - Height: 56dp.
  - Shape: large.
  - Primary container.
  - Text: `titleMedium`, Bold.
- Secondary action gap: 12dp.
- Continue-free action: `TextButton`, `onSurfaceVariant`.

### 9.14 Fullscreen Chart

Scaffold:

- Full-size `Scaffold`.
- Content insets: safe drawing horizontal and bottom.
- Top bar:
  - Window insets: safe drawing top and horizontal.
  - Padding: start 8dp, end 8dp, top 4dp, bottom 4dp.
  - Header row height: 48dp.
  - Close icon button at start.
  - Title: `titleMedium`, `onSurface`, weighted.
- Controls:
  - Appear below header when state has selections.
  - Gap above controls: 4dp.
  - Full-width connected period selector first.
  - Full-width connected metric selector second.
  - Gap: 4dp.
- Content:
  - Fills size.
  - Applies Scaffold inner padding.
  - Horizontal padding: 8dp.
  - Bottom padding: 8dp.

Chart content:

- `BoxWithConstraints`.
- Chart height equals available max height converted to dp.
- If height is infinite, fallback is 180dp.
- Height is coerced to at least 1dp.
- `TrendChart` fills width and uses fullscreen presentation.
- Quality zones:
  - Battery history: battery zones by selected metric and temperature unit.
  - Network history: signal zones by selected metric.
  - Battery session: no quality zones.
- Tooltip uses chart timestamps, unit, decimals, and selected skeleton.

Empty content:

- Centered box.
- Column max width: 420dp.
- Title: `titleMedium`, `onSurface`.
- Title/message gap: 4dp.
- Message: `bodyLarge`, `onSurfaceVariant`.

Locked content:

- Uses `ProFeatureLockedState`.

### 9.15 Learn, Insights, Settings, and Pro Cross-Navigation

- Learn cross-links use `CrossLinkButton`.
- Related article lists use `SectionHeader` plus `CrossLinkButton` rows.
- Home and full Insights rows resolve destination based on insight type and Pro
  status.
- Pro-gated destinations show `ProFeatureLockedState` or a Pro callout rather
  than custom locked UI.

### 9.16 App Shell Navigation

- The edge-to-edge root scaffold owns one Material 3 navigation bar with Home,
  Insights, Tools, and Settings in that order.
- Navigation items use outlined icons. The Insights item may show a badge for
  visible unseen Room-backed insights, including an accessible count label.
- The bar appears only at top-level roots. Pushed destinations keep their
  existing top bars. The root scaffold applies `safeDrawing` on the bottom when
  the bar is absent, consumes its content padding centrally, and avoids a
  second navigation-bar inset on top-level roots.
- Switching top-level destinations preserves their state. Reselecting the
  active destination returns to that destination's root.
- Weekly Report notifications rebuild the Tools root without restoring a saved
  Tools child stack, then push Weekly Report. Protected external routes wait
  for Pro status before routing so a cold start cannot expose gated content
  during billing initialization.
- Learn article cross-links to Home, Insights, Tools, or Settings use the
  top-level navigation policy; Back therefore returns through Home rather than
  the originating article stack.
- Export waits for Pro status. Free users see `ProFeatureLockedState`; Pro users
  open the dedicated `ExportScreen`.
- Export owns its own `ExportViewModel`, delegates CSV preparation to
  `ExportDataUseCase.prepareExportShare()`, shows `RuncheckLoadingIndicator`
  while preparing, and launches the existing Android share flow when the URIs
  are ready.

### 9.17 App Widgets

- Battery, Health Score, and Quick Glance share one Glance `ColorProviders`
  palette with explicit launcher/system day and night colors. The app's manual
  `ThemeMode` does not override widget colors.
- Every widget has responsive compact, standard, and expanded size policies;
  user-visible widget text is bounded with `maxLines` so narrow launchers
  truncate instead of overflowing.
- The Battery widget's declared minimum is 110×72dp. Its compact policy uses
  8dp outer padding and shows level plus temperature so the content and root
  action fit the minimum height at font scales 1.0, 1.3, and 2.0; wider
  policies add current and the title. Its Android 12 default is 2×2 cells so
  `targetCellHeight` agrees with the two-cell height budget.
- Quick Glance targets 4×2 and presents a 2×2 grid in this accessibility order:
  Health Score, Battery, Free storage, Temperature.
- Quick Glance selects explicit typography budgets for 1.0, 1.3, and 2.0 font
  scales. Compact cells use shorter metric labels and a conservative
  glyph-width estimate to select a fitting font; visible text stays on one line
  and may be ellipsized, while each cell keeps the complete label and value in
  its content description.
- The four Quick Glance cells open Home, Battery, Storage, and Thermal,
  respectively, through the same validated direct-route extra consumed by
  `MainActivity`.
- Widget states are Loading, Pro locked, Empty, Stale, Unavailable, and fresh
  content. State titles and supporting text expose status without relying on
  color.
- Health status text uses centralized day/night status providers whose normal
  text contrast against the corresponding widget background is at least 4.5:1.
- Free users are gated before Room snapshot subscriptions or health-score
  calculation. Quick Glance extends the Health widget's coherent snapshot with
  storage and thermal values rather than owning another score pipeline.
- Receiver declarations are non-exported, require
  `android.permission.BIND_APPWIDGET`, and provide XML metadata plus day/night
  preview layouts.

---

## 10. Accessibility and Semantics

Current accessibility behaviors in code:

- Shared touch target token is 48dp.
- Many rows use `defaultMinSize(minHeight = 48.dp)`.
- Quick Glance cells keep row-major TalkBack order and pair every value with a
  visible label; health also includes the textual Healthy/Fair/Poor/Critical
  status.
- Icon buttons use Material defaults or explicit 48dp sizes.
- Status is paired with text labels, icons, or semantic descriptions.
- Charts expose content descriptions summarizing min, max, latest, and trend.
- Progress visualizations expose progress semantics where implemented:
  - ProgressRing.
  - MiniBar when content description is provided.
  - App usage progress bars.
  - Trial progress.
  - Cleanup projection.
- Cleanup scanning/deleting states use polite live regions.
- Cleanup success overlay uses assertive live region.
- Speed metrics card uses live region semantics.
- Settings sliders expose a label/value content description.
- Cleanup group headers expose heading and expand/collapse semantics.
- Cleanup rows expose checkbox role and toggle state.
- Trial expiration modal exposes pane title semantics.
- Decorative icons often clear semantics or set `contentDescription = null`.

---

## 11. Current Icon Set

The current code uses outlined Material icons. Examples by area:

- Home/settings/navigation: outlined settings, close, keyboard arrows.
- Insights: outlined warning, close, arrow.
- Speed test: connection type icons and status icons.
- Storage cleanup: outlined image, video, audio file, description, download,
  Android, check circle.
- Pro: outlined bar chart, battery charging, data usage, widgets, file
  download, thermostat, delete, check/check circle, lock.
- Charger: outlined add and delete.
- Fullscreen chart: outlined close and open-in-full.

Auto-mirrored outlined icons are used for directional arrows where layout
direction matters.

---

## 12. Current Screen Inventory

Current navigation destinations:

- Home [top level].
- Insights [top level].
- Tools [top level].
- Battery Detail.
- Charger Comparison.
- Fullscreen Chart.
- Network Detail.
- Speed Test.
- Thermal Detail.
- Storage Detail.
- Cleanup by type.
- App Usage.
- Learn.
- Learn Article.
- Weekly Report.
- Export.
- Settings [top level].
- Pro Upgrade.

Current top-level destinations:

- Home.
- Insights.
- Tools.
- Settings.

Current nested destinations:

- Home -> Battery -> Fullscreen Chart.
- Home -> Network -> Fullscreen Chart.
- Home -> Thermal.
- Home -> Storage.
- Tools -> Speed Test.
- Tools -> Storage -> Cleanup by type.
- Tools -> Charger Comparison.
- Tools -> App Usage.
- Tools -> Learn -> Learn Article.
- Tools -> Weekly Report.
- Tools -> Export.
- Settings -> Pro Upgrade.

---

## 13. Visual Data Rules Present in Code

Current UI color rules as implemented:

- Large gauge tracks are neutral card/surface colors.
- Gauge/ring progress uses status or primary accent.
- Status colors are used for labels, dots, small bars, chart zones, badges, and
  progress indicators.
- Storage category colors are used in segmented bars, cleanup group dots,
  cleanup icons, and cleanup item mini bars.
- Text defaults to `onSurface` for values and `onSurfaceVariant` for labels.
- Numeric data frequently uses JetBrains Mono.
- Pro labels and badges use primary with low-alpha primary backgrounds.
- Error/destructive rows use `error`.
- Disabled settings rows use 0.38 alpha.

Current UI state rules as implemented:

- Home, Insights, Settings debug actions, Weekly Report and protected feature
  readiness, Battery, Network, Thermal, Storage, Speed Test, Cleanup, Charger,
  App Usage, Fullscreen Chart, and Export use `RuncheckLoadingIndicator` for
  their implemented loading paths.
- Error states use body text and retry buttons where available.
- Empty states use muted body text or card-based prompts.
- Pro-locked states navigate to Pro or show the shared locked/callout UI.
- Cleanup success temporarily overlays the whole screen.
- Pull-to-refresh wraps Battery, Network, Thermal, and Storage details.

---

## 14. Known Code-Level Exceptions and Specifics

These details are present in the current code and should be treated as existing
UI behavior:

- Top-level Home, Insights, Tools, and Settings use 16dp horizontal content padding.
- Pro upgrade content uses the scaffold's 16dp horizontal padding.
- Thermal hero uses 24dp horizontal/vertical padding.
- Home health hero uses 24dp horizontal/vertical padding and a 148dp wavy ring.
- Battery, Network, and Storage detail heroes use 16dp padding.
- Fullscreen chart top bar uses 8dp horizontal padding and 4dp vertical
  padding.
- Cleanup result item dividers start at 56dp to align after checkbox and
  thumbnail.
- Cleanup bottom spacer is 80dp to clear the bottom action bar.
- Open-source licenses dialog text is capped at 420dp height.
- Fullscreen empty content is capped at 420dp width.
- Home grid changes layout at 600dp screen width.
- `ContentContainer` caps content at 600dp even on wider screens.
- `BgCardDeep` exists and is used for hero cards, even though the older
  background table may not mention it.
- Storage document/APK category colors currently use `onSurfaceVariant`, not a
  bright accent.
- Download category color is `AccentBlue.copy(alpha = 0.6f)`.

---

## 15. Source of Truth

If this document conflicts with code, the code is the current truth. The
document should be updated from the source files listed at the top of this file.
