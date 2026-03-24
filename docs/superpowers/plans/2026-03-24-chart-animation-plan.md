# Chart Animation "Instrument Sweep" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace generic 800ms tween chart animations with a cohesive "instrument sweep" animation language across TrendChart, AreaChart, and LiveChart.

**Architecture:** All changes are Canvas-level drawing logic within existing composables. No new files, no architecture changes. Quality zone color mapping added to ChartHelpers. Each chart component gains a multi-phase animation state machine (grid → sweep → emphasis) replacing the single `animatedProgress` float.

**Tech Stack:** Kotlin, Jetpack Compose Canvas API, `Animatable<Float>`, `animateFloatAsState`, `Brush.horizontalGradient`

**Spec:** `docs/superpowers/specs/2026-03-24-chart-animation-design.md`

**Note:** Do not run Gradle builds — user builds from a separate terminal. Verify changes by reading code.

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `app/src/main/java/com/runcheck/ui/components/TrendChart.kt` | Modify | Multi-phase entry animation, scan line, status gradient line, improved fill, last value emphasis, data transition |
| `app/src/main/java/com/runcheck/ui/components/AreaChart.kt` | Modify | Multi-phase entry animation (grid fade + sweep), improved fill |
| `app/src/main/java/com/runcheck/ui/components/LiveChart.kt` | Modify | Smooth scroll interpolation, new point glow pulse |
| `app/src/main/java/com/runcheck/ui/chart/ChartHelpers.kt` | Modify | `storageQualityZones()` function, `qualityZoneColorForValue()` helper |

---

### Task 1: Add quality zone color mapping helper to ChartHelpers

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/chart/ChartHelpers.kt`

This helper maps a data value to a color by interpolating through the quality zone thresholds. Used by TrendChart to color line segments based on data values.

- [ ] **Step 1: Add `storageQualityZones()` function**

Add after the existing `thermalQualityZones()` function (around line 429). Follow the same pattern as other zone functions:

```kotlin
@Composable
fun storageQualityZones(metric: StorageHistoryMetric): List<ChartQualityZone>? {
    val statusColors = MaterialTheme.statusColors
    return when (metric) {
        StorageHistoryMetric.USED_SPACE -> listOf(
            ChartQualityZone(0f, 70f, statusColors.healthy.copy(alpha = 0.08f)),
            ChartQualityZone(70f, 90f, statusColors.fair.copy(alpha = 0.08f)),
            ChartQualityZone(90f, 100f, statusColors.critical.copy(alpha = 0.08f))
        )
        StorageHistoryMetric.AVAILABLE_SPACE -> null
    }
}
```

- [ ] **Step 2: Add `qualityZoneColorForValue()` helper**

Add a non-composable utility function that maps a Float value to a Color given a list of quality zones. This will be called per data point inside Canvas drawing:

```kotlin
fun qualityZoneColorForValue(
    value: Float,
    zones: List<ChartQualityZone>,
    defaultColor: Color
): Color {
    for (zone in zones) {
        if (value >= zone.minValue && value < zone.maxValue) {
            return zone.color.copy(alpha = 1f) // Full alpha — zone.color has 0.08 alpha for backgrounds
        }
    }
    return defaultColor
}
```

Note: The zone colors stored in `ChartQualityZone` have low alpha (0.08f) for background rendering. This helper needs the base status colors at full alpha. Adjust approach: accept a separate `List<Pair<ClosedFloatingPointRange<Float>, Color>>` for line colors, or derive from `MaterialTheme.statusColors` directly. Read the actual zone color values to decide the cleanest approach.

- [ ] **Step 3: Commit**

```
git add app/src/main/java/com/runcheck/ui/chart/ChartHelpers.kt
git commit -m "feat: lisää storageQualityZones ja qualityZoneColorForValue-apufunktio"
```

---

### Task 2: TrendChart — Multi-phase entry animation with grid fade and oscilloscope sweep

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/components/TrendChart.kt`

Replace the single `animatedProgress` (lines 110–123) with a three-phase animation: grid fade-in → oscilloscope sweep → final emphasis.

- [ ] **Step 1: Replace animation state with multi-phase system**

Replace the current animation state (lines 110–123) with:

```kotlin
// Phase 1: Grid + axes fade in (0→1 over 200ms)
val gridAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
// Phase 2: Oscilloscope sweep progress (0→1 over 1000ms)
val sweepProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
// Phase 3: Last value emphasis fade in (0→1 over 200ms)
val emphasisAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
// Scan line opacity (fades out during final 30% of sweep)
val scanLineAlpha = remember { Animatable(if (reducedMotion) 0f else 0.5f) }

LaunchedEffect(data, reducedMotion) {
    if (reducedMotion) return@LaunchedEffect
    // Reset all phases
    gridAlpha.snapTo(0f)
    sweepProgress.snapTo(0f)
    emphasisAlpha.snapTo(0f)
    scanLineAlpha.snapTo(0.5f)

    // Phase 1: Grid materialization
    gridAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
    // Phase 2: Oscilloscope sweep
    launch {
        // Fade scan line during final 30% of sweep
        delay(700) // 70% of 1000ms
        scanLineAlpha.animateTo(0f, tween(300))
    }
    sweepProgress.animateTo(
        1f,
        tween(1000, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))
    )
    // Phase 3: Emphasis
    emphasisAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
}
```

Key: the `remember` keys must include `data` so animation replays when data changes (period/metric switch triggers re-entry).

- [ ] **Step 2: Apply grid alpha to grid, axes, and quality zone drawing**

In the Canvas block, wrap the grid lines (around lines 310–323), Y-axis labels (325–339), X-axis labels (341–361), and quality zone bands (295–308) with `alpha = gridAlpha.value`:

For quality zones:
```kotlin
qualityZones?.forEach { zone ->
    // ... existing position calculation ...
    drawRect(
        color = zone.color.copy(alpha = zone.color.alpha * gridAlpha.value),
        topLeft = ..., size = ...
    )
}
```

For grid lines:
```kotlin
drawLine(
    color = gridColor.copy(alpha = gridColor.alpha * gridAlpha.value),
    ...
)
```

Same pattern for Y-axis label text and X-axis labels — use `alpha = gridAlpha.value` on the text color.

- [ ] **Step 3: Replace line/fill drawing with sweep clip + scan line**

Replace the current line and fill drawing (around lines 363–393) with clip-based sweep rendering:

```kotlin
// Calculate sweep X position
val sweepX = chartLeft + chartWidth * sweepProgress.value

// Clip rect for sweep reveal
clipRect(
    left = chartLeft,
    top = chartTop,
    right = sweepX,
    bottom = chartTop + chartHeight
) {
    // Draw fill (existing gradient code)
    drawPath(path = fillPath, brush = ..., style = Fill)
    // Draw line (existing stroke code)
    drawPath(path = linePath, color = lineColor, style = Stroke(...))
}

// Draw scan line
if (scanLineAlpha.value > 0f) {
    drawLine(
        color = lineColor.copy(alpha = scanLineAlpha.value),
        start = Offset(sweepX, chartTop),
        end = Offset(sweepX, chartTop + chartHeight),
        strokeWidth = 1.5.dp.toPx()
    )
}
```

Remove the old `animatedProgress`-based `visibleCount` logic.

- [ ] **Step 4: Verify existing tooltip and gesture handling still works**

The tooltip (lines 413–472) uses `selectedIndex` which depends on data point positions, not animation state. Verify that:
- Tooltip still appears when tapping after animation completes
- The `detectTapGestures` and `detectHorizontalDragGestures` still calculate correct indices
- No interaction is possible during sweep (sweepProgress < 1.0) — add guard if needed

Read the gesture handling code and confirm compatibility.

- [ ] **Step 5: Commit**

```
git add app/src/main/java/com/runcheck/ui/components/TrendChart.kt
git commit -m "feat: TrendChart oskilloskooppi-pyyhkäisy ja grid-first sisääntulo"
```

---

### Task 3: TrendChart — Status gradient line

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/components/TrendChart.kt`
- Read: `app/src/main/java/com/runcheck/ui/chart/ChartHelpers.kt` (for zone data)

Color the data line based on quality zone status values — green for healthy, amber for fair, red for critical.

- [ ] **Step 1: Add `qualityZones` to gradient color stop calculation**

Before the Canvas block, compute a list of color stops from the quality zones and data values. This is done once per recomposition (when data changes), not per frame:

```kotlin
val lineGradientColors = remember(data, qualityZones, lineColor) {
    if (qualityZones.isNullOrEmpty() || data.isEmpty()) null
    else {
        // Map each data point's value to a status color
        // Build list of color stops at fractional X positions
        data.mapIndexed { index, value ->
            val fraction = if (data.size <= 1) 0f else index.toFloat() / (data.size - 1)
            val color = qualityZoneColorForValue(value, qualityZones, lineColor)
            fraction to color
        }
    }
}
```

- [ ] **Step 2: Replace solid line color with gradient brush when available**

In the Canvas line drawing section, use `Brush.horizontalGradient` when gradient colors are available:

```kotlin
if (lineGradientColors != null) {
    drawPath(
        path = linePath,
        brush = Brush.horizontalGradient(
            colorStops = lineGradientColors.toTypedArray()
        ),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
} else {
    // Existing single-color line
    drawPath(path = linePath, color = lineColor, style = Stroke(...))
}
```

- [ ] **Step 3: Verify `qualityZoneColorForValue` handles zone alpha correctly**

The quality zone colors in `ChartQualityZone` use `alpha = 0.08f` for background bands. The line gradient needs full-alpha versions. Read the actual zone definitions in ChartHelpers.kt and verify that `qualityZoneColorForValue` returns properly visible colors. If zones store low-alpha colors, extract the base hue and use it at full alpha for the line.

- [ ] **Step 4: Commit**

```
git add app/src/main/java/com/runcheck/ui/components/TrendChart.kt
git commit -m "feat: TrendChart status gradient -viiva quality zone -väreillä"
```

---

### Task 4: TrendChart — Improved gradient fill

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/components/TrendChart.kt`

Make fill alpha proportional to data value height — peaks get stronger fill, valleys get weaker.

- [ ] **Step 1: Replace uniform vertical gradient with height-aware fill**

The current fill uses a single `Brush.verticalGradient`. Replace with per-column rendering that varies alpha based on the data point's Y position. The most practical approach in Canvas:

Instead of drawing one gradient fill for the entire path, draw the fill in vertical strips where each strip's top alpha depends on the data value at that X position:

```kotlin
// For each pair of adjacent data points, draw a trapezoid fill
// with alpha based on average height of the two points
for (i in 0 until visibleCount - 1) {
    val x1 = chartLeft + i * stepX
    val x2 = chartLeft + (i + 1) * stepX
    val y1 = /* y position of data[i] */
    val y2 = /* y position of data[i+1] */
    val avgNormalizedY = ((data[i] - minVal) / range + (data[i + 1] - minVal) / range) / 2f
    val topAlpha = lerp(0.08f, 0.30f, avgNormalizedY)

    val stripPath = Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y2)
        lineTo(x2, chartTop + chartHeight)
        lineTo(x1, chartTop + chartHeight)
        close()
    }
    drawPath(
        path = stripPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                fillColor.copy(alpha = topAlpha),
                fillColor.copy(alpha = 0.02f)
            ),
            startY = minOf(y1, y2),
            endY = chartTop + chartHeight
        )
    )
}
```

**Performance:** Reuse a single `Path` with `reset()` between strips (do NOT create `Path()` per strip — with 300 data points that's 300 allocations per frame during animation):

```kotlin
val stripPath = remember { Path() }
// Inside Canvas draw loop:
for (i in 0 until data.size - 1) {
    stripPath.reset()
    stripPath.moveTo(x1, y1)
    // ... build strip ...
    drawPath(path = stripPath, brush = ..., style = Fill)
}
```

- [ ] **Step 2: Apply sweep clip to improved fill**

Ensure the strip-based fill is also clipped by the sweep progress, same as the line:

```kotlin
clipRect(left = chartLeft, top = chartTop, right = sweepX, bottom = chartTop + chartHeight) {
    // Draw all fill strips here
    // Draw line here
}
```

- [ ] **Step 3: Commit**

```
git add app/src/main/java/com/runcheck/ui/components/TrendChart.kt
git commit -m "feat: TrendChart parannettu gradient-täyttö datan korkeuden mukaan"
```

---

### Task 5: TrendChart — Last value emphasis

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/components/TrendChart.kt`

Add a glowing dot at the last data point + dashed horizontal line to the Y-axis.

- [ ] **Step 1: Draw last value emphasis after sweep completes**

After the line/fill drawing, add emphasis elements controlled by `emphasisAlpha`:

```kotlin
if (data.isNotEmpty() && emphasisAlpha.value > 0f) {
    val lastIndex = data.size - 1
    val lastX = chartLeft + lastIndex * stepX
    val lastY = chartTop + chartHeight - ((data[lastIndex] - minVal) / range * chartHeight)

    // Glow circle (outer)
    drawCircle(
        color = lineColor.copy(alpha = 0.3f * emphasisAlpha.value),
        radius = 6.dp.toPx(),
        center = Offset(lastX, lastY)
    )
    // Solid dot (inner)
    drawCircle(
        color = lineColor.copy(alpha = emphasisAlpha.value),
        radius = 3.dp.toPx(),
        center = Offset(lastX, lastY)
    )
    // Dashed horizontal line to Y-axis
    drawLine(
        color = lineColor.copy(alpha = 0.4f * emphasisAlpha.value),
        start = Offset(lastX, lastY),
        end = Offset(chartLeft, lastY),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(4.dp.toPx(), 4.dp.toPx())
        )
    )
}
```

- [ ] **Step 2: Ensure emphasis is inside sweep clip or drawn after clip**

The emphasis should appear only after the sweep is complete (Phase 3). Since `emphasisAlpha` starts animating after `sweepProgress` reaches 1.0, drawing it outside the clip rect is fine — the last point is at the right edge which is fully revealed.

- [ ] **Step 3: Commit**

```
git add app/src/main/java/com/runcheck/ui/components/TrendChart.kt
git commit -m "feat: TrendChart viimeisen arvon korostus (glow-piste + katkoviiva)"
```

---

### Task 6: TrendChart — Data transition animation (period/metric change)

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/components/TrendChart.kt`

When `data` changes (period or metric switch), the current implementation just replays the entry animation because `LaunchedEffect(data)` resets everything. This is close to what we want (fade out + sweep in), but we need to add the fade-out of old data before the new sweep.

- [ ] **Step 1: Store previous data points for fade-out**

Store the previous data list (not paths — paths are populated inside Canvas and would be stale in LaunchedEffect). Reconstruct the fade-out path in the Canvas block from stored data points:

```kotlin
var previousData by remember { mutableStateOf<List<Float>>(emptyList()) }
var previousMinVal by remember { mutableFloatStateOf(0f) }
var previousRange by remember { mutableFloatStateOf(1f) }
val fadeOutAlpha = remember { Animatable(0f) }
```

In `LaunchedEffect(data)`, before resetting sweep, snapshot the current data:
```kotlin
LaunchedEffect(data, reducedMotion) {
    if (reducedMotion) return@LaunchedEffect

    // Save current data for fade-out (only if we have previous data displayed)
    if (sweepProgress.value > 0f && data.isNotEmpty()) {
        // Store the data that was just showing — Canvas will rebuild paths from this
        // previousData, previousMinVal, previousRange are set in the Canvas block
        // on each draw, so they reflect the last rendered state
        fadeOutAlpha.snapTo(1f)
        launch { fadeOutAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
        delay(200) // Overlap: start sweep before fade completes
    }

    // Reset sweep phases
    sweepProgress.snapTo(0f)
    emphasisAlpha.snapTo(0f)
    scanLineAlpha.snapTo(0.5f)
    // Grid stays visible (don't reset gridAlpha on data change)

    // Phase 2: Sweep (800ms on transition, faster than initial 1000ms)
    launch {
        delay(560) // 70% of 800ms
        scanLineAlpha.animateTo(0f, tween(240))
    }
    sweepProgress.animateTo(
        1f,
        tween(800, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))
    )
    // Phase 3: Emphasis
    emphasisAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
}
```

Update `previousData`/`previousMinVal`/`previousRange` at the END of each Canvas draw when sweep is complete:
```kotlin
// At end of Canvas block:
if (sweepProgress.value >= 1f) {
    previousData = data
    previousMinVal = minVal
    previousRange = range
}
```

- [ ] **Step 2: Draw fading previous data in Canvas**

Before drawing the new data, rebuild and draw the previous path from `previousData`:

```kotlin
if (previousData.isNotEmpty() && fadeOutAlpha.value > 0f) {
    val prevPath = Path()
    val prevFillPath = Path()
    val prevStepX = if (previousData.size > 1) chartWidth / (previousData.size - 1) else chartWidth

    previousData.forEachIndexed { i, value ->
        val x = chartLeft + i * prevStepX
        val y = chartTop + chartHeight - ((value - previousMinVal) / previousRange * chartHeight)
        if (i == 0) { prevPath.moveTo(x, y); prevFillPath.moveTo(x, y) }
        else { prevPath.lineTo(x, y); prevFillPath.lineTo(x, y) }
    }
    prevFillPath.lineTo(chartLeft + (previousData.size - 1) * prevStepX, chartTop + chartHeight)
    prevFillPath.lineTo(chartLeft, chartTop + chartHeight)
    prevFillPath.close()

    drawPath(path = prevFillPath, brush = Brush.verticalGradient(...), alpha = fadeOutAlpha.value * 0.5f)
    drawPath(path = prevPath, color = lineColor.copy(alpha = fadeOutAlpha.value), style = Stroke(...))
}
```

- [ ] **Step 3: Differentiate initial entry from data transition**

Use a flag to track whether this is the first appearance or a data change:

```kotlin
var isInitialEntry by remember { mutableStateOf(true) }

LaunchedEffect(data, reducedMotion) {
    if (isInitialEntry) {
        // Full sequence: grid fade (200ms) → sweep (1000ms) → emphasis (200ms)
        gridAlpha.snapTo(0f)
        gridAlpha.animateTo(1f, tween(200))
        // ... 1000ms sweep ...
        isInitialEntry = false
    } else {
        // Transition: fade out (300ms, overlapped) → sweep (800ms) → emphasis (200ms)
        // Grid stays visible
        // ... 800ms sweep ...
    }
}
```

- [ ] **Step 4: Commit**

```
git add app/src/main/java/com/runcheck/ui/components/TrendChart.kt
git commit -m "feat: TrendChart datanvaihtosiirtymä (fade-out + uusi pyyhkäisy)"
```

---

### Task 7: AreaChart — Multi-phase entry animation

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/components/AreaChart.kt`

AreaChart is simpler — no axes, no quality zones, no tooltip. Apply the oscilloscope sweep and improved fill.

- [ ] **Step 1: Replace animation state with sweep + scan line**

Replace the current animation (lines 38–51) with:

```kotlin
val sweepProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
val scanLineAlpha = remember { Animatable(if (reducedMotion) 0f else 0.5f) }

LaunchedEffect(data, reducedMotion) {
    if (reducedMotion) return@LaunchedEffect
    sweepProgress.snapTo(0f)
    scanLineAlpha.snapTo(0.5f)
    launch {
        delay(560)
        scanLineAlpha.animateTo(0f, tween(240))
    }
    sweepProgress.animateTo(
        1f,
        tween(800, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))
    )
}
```

- [ ] **Step 2: Apply clip-based sweep rendering and scan line**

Replace the current `visibleCount`-based drawing with clip rect approach:

```kotlin
val sweepX = sweepProgress.value * size.width

clipRect(left = 0f, top = 0f, right = sweepX, bottom = size.height) {
    drawPath(path = fillPath, brush = ..., style = Fill)
    drawPath(path = linePath, color = lineColor.copy(alpha = 0.7f), style = Stroke(...))
}

// Scan line
if (scanLineAlpha.value > 0f) {
    drawLine(
        color = lineColor.copy(alpha = scanLineAlpha.value),
        start = Offset(sweepX, 0f),
        end = Offset(sweepX, size.height),
        strokeWidth = 1.5.dp.toPx()
    )
}
```

Build the full path for all data points (not `visibleCount`), then let the clip rect control visibility.

- [ ] **Step 3: Apply improved gradient fill**

Same height-aware fill as TrendChart but simpler (no quality zones in AreaChart):

```kotlin
for (i in 0 until data.size - 1) {
    val avgNormalizedY = ((data[i] - minVal) / range + (data[i + 1] - minVal) / range) / 2f
    val topAlpha = lerp(0.08f, 0.25f, avgNormalizedY) // Slightly lower max than TrendChart
    // ... strip fill drawing ...
}
```

- [ ] **Step 4: Commit**

```
git add app/src/main/java/com/runcheck/ui/components/AreaChart.kt
git commit -m "feat: AreaChart oskilloskooppi-pyyhkäisy ja parannettu täyttö"
```

---

### Task 8: LiveChart — Smooth scroll interpolation and glow pulse

**Files:**
- Modify: `app/src/main/java/com/runcheck/ui/components/LiveChart.kt`

Add smooth scrolling when new data arrives + glow pulse on newest point.

- [ ] **Step 1: Pass `reducedMotion` into LiveChartCanvas**

LiveChartCanvas does NOT currently have access to `MaterialTheme.reducedMotion` (confirmed by code inspection). Add it as a parameter from the parent `LiveChart` composable, which is `@Composable` and can read the theme:

```kotlin
// In LiveChart composable (parent):
val reducedMotion = MaterialTheme.reducedMotion

// Pass to LiveChartCanvas:
LiveChartCanvas(
    data = visibleData,
    // ... existing params ...
    reducedMotion = reducedMotion
)

// In LiveChartCanvas signature, add parameter:
@Composable
private fun LiveChartCanvas(
    // ... existing params ...
    reducedMotion: Boolean = false
)
```

This must be done FIRST because Steps 2 and 3 reference `reducedMotion`.

- [ ] **Step 2: Add animated scroll offset**

Currently `offsetX = (maxPoints - data.size) * stepX` jumps instantly. Animate this:

```kotlin
// In LiveChartCanvas:
val animatedScrollOffset = remember { Animatable(0f) }
val previousDataSize = remember { mutableIntStateOf(data.size) }

LaunchedEffect(data.size) {
    if (!reducedMotion && data.size > previousDataSize.intValue && data.size > 1) {
        // New point arrived — animate one step left
        val stepX = /* calculate based on canvas width and maxPoints */
        animatedScrollOffset.snapTo(stepX) // Start offset to the right
        animatedScrollOffset.animateTo(0f, tween(150, easing = LinearEasing))
    }
    previousDataSize.intValue = data.size
}
```

Then in Canvas, add `animatedScrollOffset.value` to each point's X position.

**Challenge:** `stepX` depends on Canvas size which isn't known until drawing. Solution: calculate `stepX` from a remembered canvas width that updates on each draw, or use `onSizeChanged` modifier.

- [ ] **Step 3: Add glow pulse on newest point**

Add a pulse animation that triggers when a new data point arrives:

```kotlin
val glowPulseAlpha = remember { Animatable(0f) }
val glowPulseRadius = remember { Animatable(3.dp.value) }

LaunchedEffect(data.size) {
    if (!reducedMotion && data.size > 1) {
        // Pulse: expand + fade
        glowPulseAlpha.snapTo(0.4f)
        glowPulseRadius.snapTo(8.dp.value)
        launch { glowPulseAlpha.animateTo(0.15f, tween(300)) }
        glowPulseRadius.animateTo(3.dp.value, tween(300))
    }
}
```

In the Canvas, replace the static outer glow circle with the animated version:

```kotlin
// Animated pulse glow (replaces static outer glow)
drawCircle(
    color = lineColor.copy(alpha = glowPulseAlpha.value),
    radius = with(density) { glowPulseRadius.value.dp.toPx() },
    center = Offset(dotX, dotY)
)
// Inner dot (unchanged)
drawCircle(
    color = lineColor,
    radius = 3.dp.toPx(),
    center = Offset(dotX, dotY)
)
```

- [ ] **Step 4: Commit**

```
git add app/src/main/java/com/runcheck/ui/components/LiveChart.kt
git commit -m "feat: LiveChart pehmeä liuku ja glow-pulssi uusille datapisteille"
```

---

### Task 9: Integration verification and cleanup

**Files:**
- Read: All modified files
- Read: Detail screen files that use these charts

- [ ] **Step 1: Verify TrendChart usage sites pass correct parameters**

Read these files and confirm TrendChart calls don't break:
- `app/src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/thermal/ThermalDetailScreen.kt`
- `app/src/main/java/com/runcheck/ui/storage/StorageDetailScreen.kt`

TrendChart signature must remain backward-compatible — all new parameters have defaults.

- [ ] **Step 2: Verify AreaChart usage**

AreaChart is used in blurred Pro preview states on BatteryDetailScreen. Confirm the signature change is compatible.

- [ ] **Step 3: Verify LiveChart usage**

LiveChart is used on BatteryDetailScreen for real-time current/power. Confirm no parameter changes break existing calls.

- [ ] **Step 4: Add `storageQualityZones` to StorageDetailScreen**

If StorageDetailScreen doesn't already pass quality zones to its TrendChart, add the call:

```kotlin
val storageQualityZones = storageQualityZones(selectedMetric)
// Pass to TrendChart:
TrendChart(
    // ... existing params ...
    qualityZones = storageQualityZones
)
```

- [ ] **Step 5: Verify `reducedMotion` is respected everywhere**

Read all modified files and confirm:
- Every `Animatable` starts at final value when `reducedMotion` is true
- Scan line is never shown when `reducedMotion` is true
- Glow pulse is not shown when `reducedMotion` is true

- [ ] **Step 6: Final commit**

```
git add -A
git commit -m "feat: integroi chart-animaatiot kaikkiin detail-näkymiin"
```
