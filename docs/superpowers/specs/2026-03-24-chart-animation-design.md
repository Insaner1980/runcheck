# Chart Animation Design — "Instrument Sweep"

## Overview

Upgrade chart animations from generic tween reveals to a cohesive "instrument sweep" animation language that reinforces the app's diagnostic tool identity. The style is technical and precise with minimalist restraint.

## Scope

Three chart components only:
- **TrendChart** — history charts on all detail screens
- **AreaChart** — blurred Pro preview charts
- **LiveChart** — real-time battery/power monitoring

Out of scope: ProgressRing, MiniBar, SegmentedBar, HeatStrip, tooltip animations.

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Animation personality | Technical & precise + minimalist | Fits diagnostic tool brand, builds trust in data accuracy |
| Entry animation | Oscilloscope sweep with scan line | Distinctive "instrument" metaphor, not generic |
| Data transition | Fade out old + sweep in new | Reliable, consistent with entry animation |
| LiveChart updates | Smooth interpolation + glow pulse | Continuous feel like real oscilloscope, pulse signals new reading |
| Visual polish | Status gradient line, improved fill, grid-first entry, last value emphasis | Adds information density and depth without clutter |
| Rejected: line glow | No neon glow effect | Would push toward SciFi aesthetic, away from clean precision |
| Rejected: tooltip animation | Keep current tooltip | Current tooltip works fine, avoids scope creep |
| Rejected: path morphing for transitions | Fade + re-sweep instead | Morph has edge cases with very different datasets; sweep is 100% reliable |

## Entry Animation Sequence (TrendChart / AreaChart)

All timings respect `MaterialTheme.reducedMotion` — instant (0ms) when enabled.

### Phase 1: Grid Materialization (0–200ms)
- Grid lines and axis labels fade in together: `alpha 0 → 1`, 200ms ease-out
- Quality zone background bands (if present) fade in simultaneously
- Creates the "instrument calibrating" feeling before data arrives

### Phase 2: Oscilloscope Sweep (200–1200ms)
- A thin vertical scan line (1.5dp, line color, 50% alpha) moves left to right across the chart area
- The data line is drawn progressively behind the scan line using a clip rect that follows the scan line's X position
- Gradient fill area appears behind the line with the same clip, alpha ramping up
- Easing: `CubicBezier(0.25, 0.1, 0.25, 1.0)` — fast start, smooth deceleration
- Scan line fades from 50% to 0% alpha over the final 30% of its travel

### Phase 3: Final Touches (1200–1400ms)
- Last data point: small circle (3dp) fades in with subtle glow (6dp, 30% alpha, line color)
- Dashed line (1dp, `DashPathEffect [4dp, 4dp]`) draws from last point horizontally to Y-axis, 200ms fade-in
- Status gradient line color is fully visible by this point

### Total Duration: ~1400ms first appearance

## Data Transition Animation (Period / Metric Change)

When user switches history period (Day → Week → Month) or metric (Level → Temperature):

### Phase 1: Fade Out (0–300ms)
- Current line + fill + last-value indicator fade to `alpha 0`, 300ms ease-in
- Grid and axes remain visible (no flicker)
- Y-axis labels crossfade if scale changed

### Phase 2: Sweep In (300–1100ms)
- Same oscilloscope sweep as entry, but 800ms duration (faster — user is already "calibrated")
- New data line, fill, and status gradient colors
- New last-value emphasis appears at end

### Total Duration: ~1100ms per transition

## LiveChart Real-Time Updates

### Smooth Scroll Interpolation
- When new data point arrives, animate the entire path shift leftward over ~150ms
- Use `Animatable<Float>` for scroll offset, `tween(150ms, LinearEasing)`
- New point position interpolates from previous last point to new position
- Prevents the current "jump" between frames

### New Point Glow Pulse
- Newest data point gets a glow pulse on arrival:
  - Outer circle: 8dp radius, line color at 40% alpha → 15% alpha over 300ms
  - Inner circle: 3dp radius, line color at 100% (persistent, same as current)
- Pulse happens once per new point, does not repeat
- The persistent dot at the rightmost point remains as current implementation

### Current Value Dot (Unchanged)
- Existing outer glow (5dp, 30% alpha) + inner dot (3dp) pattern is kept
- The arrival pulse is additive — briefly brighter, then settles to normal

## Status Gradient Line

Applies to metrics that have quality zones defined. Zone thresholds and colors are derived from the existing `ChartQualityZone` data structures (`batteryQualityZones()`, `signalQualityZones()`, `thermalQualityZones()` in ChartHelpers.kt) — not hardcoded separately. The gradient line reuses the same boundaries and status colors that the quality zone background bands already use.

For storage charts: a new `storageQualityZones()` function must be created in ChartHelpers.kt (zones do not exist yet).

Example zone mappings (illustrative — actual values come from existing zone functions):
- Battery Level: Teal (healthy) → Amber (fair) → Red (critical)
- Battery Temperature: Teal (normal) → Amber (warm) → Red (hot)
- Signal Strength: uses existing 5-zone scale from `signalQualityZones()`
- Thermal: uses existing zones from `thermalQualityZones()`

### Implementation
- Use `Brush.horizontalGradient` with color stops calculated from data point Y-values mapped to existing `ChartQualityZone` thresholds
- Each line segment between two data points gets the color interpolated from the zone boundaries
- For metrics without quality zones (Current, Voltage, Power, Latency): single solid color as today

### Fallback
- If quality zones are not provided to TrendChart, line renders in the existing single color — no behavioral change for charts without zones

## Improved Gradient Fill

### Current
Simple `Brush.verticalGradient` from line color (25% alpha) at top to transparent (2%) at bottom, uniform across the chart.

### New
- Fill alpha at each X position is proportional to the Y-value's height in the chart
- At peaks (high values, top of chart): gradient starts at 30% alpha
- At valleys (low values, bottom of chart): gradient starts at 8% alpha
- Creates a natural "depth" effect — areas with more data prominence are visually heavier
- Implementation: for each vertical strip, calculate `topAlpha = lerp(0.08, 0.30, normalizedY)` where `normalizedY` is the data point's position in the Y range

## Last Value Emphasis (TrendChart only)

- Small glowing dot at the rightmost data point: 3dp solid + 6dp glow at 30% alpha
- Thin dashed horizontal line from the dot to the Y-axis area: 1dp stroke, `DashPathEffect [4dp, 4dp]`, line color at 40% alpha
- Both elements appear in Phase 3 of entry animation and persist
- On data transition: old emphasis fades with old data, new one appears with new data

## Grid-First Entry (All Charts)

- Grid lines currently appear as part of the static background
- Change: grid lines are invisible initially, fade in during Phase 1 (200ms) before data sweep begins
- X-axis labels and Y-axis labels fade in with the grid
- Quality zone bands (colored background rectangles) also fade in with the grid
- This creates a sequential "power on → calibrate → measure" narrative

## Reduced Motion

All new animations check `MaterialTheme.reducedMotion`:
- When true: all durations become 0ms, elements appear instantly
- Scan line is not shown
- Glow pulse is not shown
- Grid, data, and emphasis all appear in final state immediately

## Performance Considerations

- Scan line: single `drawLine()` per frame during animation — negligible
- Status gradient: `Brush.horizontalGradient` computed once per data change, cached in `remember` — no per-frame cost
- Improved fill: calculated once during path building, not per frame
- LiveChart interpolation: single `Animatable<Float>` — standard Compose animation overhead
- All animations use hardware-accelerated Canvas operations

## Components Modified

| File | Changes |
|------|---------|
| `TrendChart.kt` | Entry sequence (grid fade → sweep → emphasis), transition animation, status gradient line, improved fill, last value emphasis |
| `AreaChart.kt` | Entry sequence (grid fade → sweep), improved fill |
| `LiveChart.kt` | Smooth scroll interpolation, new point glow pulse |
| `ChartHelpers.kt` | Helper function for mapping quality zones to gradient color stops |

## Not Changed

| File | Reason |
|------|--------|
| `ChartModels.kt` | No model changes needed |
| `ChartRenderModel.kt` | No model changes needed |
| `HeatStrip.kt` | Out of scope — current animation is good |
| `SegmentedBar.kt` | Out of scope — different visual metaphor |
| `ProgressRing.kt` | Out of scope — already effective |
| `MiniBar.kt` | Out of scope — simple, works well |
