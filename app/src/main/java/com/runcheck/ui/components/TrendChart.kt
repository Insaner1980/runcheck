package com.runcheck.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.runcheck.ui.chart.qualityZoneColorForValue
import com.runcheck.ui.theme.chartAxisTextStyle
import com.runcheck.ui.theme.chartTooltipTextStyle
import com.runcheck.ui.theme.reducedMotion

/**
 * Quality zone band drawn as a subtle background behind the chart.
 */
data class ChartQualityZone(
    val minValue: Float,
    val maxValue: Float,
    val color: Color
)

/**
 * Label for the X-axis positioned by normalized fraction (0f = left, 1f = right).
 */
data class ChartXLabel(
    val position: Float,
    val label: String
)

/**
 * Label for the Y-axis positioned by value in data units.
 */
data class ChartYLabel(
    val value: Float,
    val label: String
)

enum class TrendChartPresentation {
    Embedded,
    Fullscreen
}

private data class TrendChartStyle(
    val chartPadding: Dp,
    val yLabelGap: Dp,
    val xLabelTopPadding: Dp,
    val gestureEdgeGuard: Dp,
    val lineStrokeWidth: Dp,
    val gridStrokeWidth: Dp,
    val tickLength: Dp,
    val pointMarkerRadius: Dp,
    val selectedPointOuterRadius: Dp,
    val selectedPointInnerRadius: Dp,
    val axisTextStyle: TextStyle,
    val tooltipTextStyle: TextStyle
)

private const val GRID_FADE_DURATION_MS = 200
private const val INITIAL_SWEEP_DURATION_MS = 1000
private const val TRANSITION_SWEEP_DURATION_MS = 800
private const val SWEEP_SCAN_FADE_DELAY_MS = 700
private const val SWEEP_SCAN_FADE_DURATION_MS = 300
private const val TRANSITION_SCAN_FADE_DELAY_MS = 560
private const val TRANSITION_SCAN_FADE_DURATION_MS = 240
private const val EMPHASIS_DURATION_MS = 200
private const val FADE_OUT_DURATION_MS = 300
private const val TRANSITION_OVERLAP_MS = 200
private const val SCAN_LINE_START_ALPHA = 0.5f

private val SweepEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

private fun buildTrendLineGradientStops(
    data: List<Float>,
    qualityZones: List<ChartQualityZone>?,
    defaultColor: Color
): Array<Pair<Float, Color>>? {
    if (qualityZones.isNullOrEmpty() || data.isEmpty()) return null

    val colors = data.map { value ->
        qualityZoneColorForValue(value, qualityZones, defaultColor)
    }
    val uniqueColors = colors.distinct()
    if (uniqueColors.size == 1) {
        val solidColor = uniqueColors.first()
        return arrayOf(0f to solidColor, 1f to solidColor)
    }

    val lastIndex = data.lastIndex.coerceAtLeast(1)
    return Array(data.size) { index ->
        (index.toFloat() / lastIndex).coerceIn(0f, 1f) to colors[index]
    }
}

@Composable
fun TrendChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 200.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    contentDescription: String? = null,
    // Axis labels
    yLabels: List<ChartYLabel>? = null,
    xLabels: List<ChartXLabel>? = null,
    // Grid
    showGrid: Boolean = false,
    // Quality zone bands
    qualityZones: List<ChartQualityZone>? = null,
    // Tooltip — called with data index when user taps/drags
    tooltipFormatter: ((index: Int) -> String)? = null,
    presentation: TrendChartPresentation = TrendChartPresentation.Embedded
) {
    val reducedMotion = MaterialTheme.reducedMotion

    // Phase 1: Grid + axes fade in (0→1 over 200ms)
    val gridAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    // Phase 2: Oscilloscope sweep progress (0→1 over 1000ms)
    val sweepProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    // Phase 3: Last value emphasis fade in (0→1 over 200ms)
    val emphasisAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    // Scan line opacity (fades out during final 30% of sweep)
    val scanLineAlpha = remember { Animatable(if (reducedMotion) 0f else 0.5f) }

    // Tooltip state: -1 means no selection (declared early for use in LaunchedEffect)
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Previous data for fade-out during data transitions
    var previousData by remember { mutableStateOf<List<Float>>(emptyList()) }
    var previousMinVal by remember { mutableFloatStateOf(0f) }
    var previousRange by remember { mutableFloatStateOf(1f) }
    val fadeOutAlpha = remember { Animatable(0f) }

    var settledData by remember { mutableStateOf<List<Float>>(emptyList()) }
    var emphasisData by remember { mutableStateOf<List<Float>>(emptyList()) }

    // Tracks whether the initial chart entry has started at least once.
    var hasStartedEntry by remember { mutableStateOf(false) }

    LaunchedEffect(data, reducedMotion) {
        selectedIndex = -1
        emphasisData = emptyList()

        if (reducedMotion) {
            previousData = emptyList()
            gridAlpha.snapTo(1f)
            sweepProgress.snapTo(1f)
            emphasisAlpha.snapTo(1f)
            scanLineAlpha.snapTo(0f)
            fadeOutAlpha.snapTo(0f)
            settledData = data
            emphasisData = data
            hasStartedEntry = true
            return@LaunchedEffect
        }

        if (!hasStartedEntry) {
            hasStartedEntry = true
            previousData = emptyList()
            fadeOutAlpha.snapTo(0f)
            gridAlpha.snapTo(0f)
            sweepProgress.snapTo(0f)
            emphasisAlpha.snapTo(0f)
            scanLineAlpha.snapTo(if (data.isNotEmpty()) SCAN_LINE_START_ALPHA else 0f)

            gridAlpha.animateTo(1f, tween(GRID_FADE_DURATION_MS, easing = FastOutSlowInEasing))
            if (data.isEmpty()) {
                sweepProgress.snapTo(1f)
                scanLineAlpha.snapTo(0f)
                return@LaunchedEffect
            }

            launch {
                delay(SWEEP_SCAN_FADE_DELAY_MS.toLong())
                scanLineAlpha.animateTo(0f, tween(SWEEP_SCAN_FADE_DURATION_MS))
            }
            sweepProgress.animateTo(1f, tween(INITIAL_SWEEP_DURATION_MS, easing = SweepEasing))
            scanLineAlpha.snapTo(0f)
            emphasisData = data
            emphasisAlpha.animateTo(1f, tween(EMPHASIS_DURATION_MS, easing = FastOutSlowInEasing))
            settledData = data
            return@LaunchedEffect
        }

        val canFadeOutSettledData = settledData.size >= 2 &&
            sweepProgress.value >= 1f &&
            emphasisAlpha.value > 0f
        val fadeSource = if (canFadeOutSettledData) settledData else emptyList()

        previousData = fadeSource
        previousMinVal = fadeSource.minOrNull() ?: 0f
        previousRange = fadeSource
            .let { source -> (source.maxOrNull() ?: 1f) - (source.minOrNull() ?: 0f) }
            .coerceAtLeast(1f)

        gridAlpha.snapTo(1f)
        sweepProgress.snapTo(0f)
        emphasisAlpha.snapTo(0f)
        scanLineAlpha.snapTo(if (data.isNotEmpty()) SCAN_LINE_START_ALPHA else 0f)

        if (fadeSource.isNotEmpty()) {
            fadeOutAlpha.snapTo(1f)
            launch {
                fadeOutAlpha.animateTo(
                    0f,
                    tween(FADE_OUT_DURATION_MS, easing = FastOutSlowInEasing)
                )
            }
            delay(TRANSITION_OVERLAP_MS.toLong())
        } else {
            fadeOutAlpha.snapTo(0f)
        }

        if (data.isEmpty()) {
            scanLineAlpha.snapTo(0f)
            sweepProgress.snapTo(1f)
            settledData = emptyList()
            return@LaunchedEffect
        }

        launch {
            delay(TRANSITION_SCAN_FADE_DELAY_MS.toLong())
            scanLineAlpha.animateTo(0f, tween(TRANSITION_SCAN_FADE_DURATION_MS))
        }
        sweepProgress.animateTo(1f, tween(TRANSITION_SWEEP_DURATION_MS, easing = SweepEasing))
        scanLineAlpha.snapTo(0f)
        emphasisData = data
        emphasisAlpha.animateTo(1f, tween(EMPHASIS_DURATION_MS, easing = FastOutSlowInEasing))
        settledData = data
    }

    val scaleValues = remember(data, yLabels, qualityZones) {
        buildList {
            addAll(data)
            yLabels?.forEach { add(it.value) }
            qualityZones?.forEach { zone ->
                add(zone.minValue)
                add(zone.maxValue)
            }
        }
    }
    val minVal = scaleValues.minOrNull() ?: 0f
    val maxVal = scaleValues.maxOrNull() ?: (minVal + 1f)
    val range = (maxVal - minVal).coerceAtLeast(1f)

    val linePath = remember { Path() }
    val previousLinePath = remember { Path() }
    val stripPath = remember { Path() }

    val hasYLabels = yLabels != null && yLabels.isNotEmpty()
    val hasXLabels = xLabels != null && xLabels.isNotEmpty()

    val textMeasurer = rememberTextMeasurer()
    val chartStyle = when (presentation) {
        TrendChartPresentation.Embedded -> TrendChartStyle(
            chartPadding = 8.dp,
            yLabelGap = 6.dp,
            xLabelTopPadding = 4.dp,
            gestureEdgeGuard = 24.dp,
            lineStrokeWidth = 2.dp,
            gridStrokeWidth = 1.dp,
            tickLength = 5.dp,
            pointMarkerRadius = 0.dp,
            selectedPointOuterRadius = 4.dp,
            selectedPointInnerRadius = 2.dp,
            axisTextStyle = MaterialTheme.chartAxisTextStyle,
            tooltipTextStyle = MaterialTheme.chartTooltipTextStyle
        )
        TrendChartPresentation.Fullscreen -> TrendChartStyle(
            chartPadding = 16.dp,
            yLabelGap = 10.dp,
            xLabelTopPadding = 8.dp,
            gestureEdgeGuard = 28.dp,
            lineStrokeWidth = 3.dp,
            gridStrokeWidth = 1.5.dp,
            tickLength = 8.dp,
            pointMarkerRadius = 3.5.dp,
            selectedPointOuterRadius = 6.dp,
            selectedPointInnerRadius = 3.dp,
            axisTextStyle = MaterialTheme.chartAxisTextStyle.copy(
                fontSize = 12.sp,
                lineHeight = 14.sp
            ),
            tooltipTextStyle = MaterialTheme.chartTooltipTextStyle.copy(
                fontSize = 13.sp,
                lineHeight = 16.sp
            )
        )
    }
    val labelStyle = chartStyle.axisTextStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val tooltipLabelStyle = chartStyle.tooltipTextStyle.copy(color = MaterialTheme.colorScheme.onSurface)

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    val tooltipBgColor = MaterialTheme.colorScheme.surfaceContainer
    val tooltipLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val measuredYLabels = remember(yLabels, labelStyle, textMeasurer) {
        yLabels?.map { it to textMeasurer.measure(it.label, labelStyle) }.orEmpty()
    }
    val measuredXLabels = remember(xLabels, labelStyle, textMeasurer) {
        xLabels?.map { it to textMeasurer.measure(it.label, labelStyle) }.orEmpty()
    }
    val density = LocalDensity.current
    val yLabelWidth = if (hasYLabels) {
        val maxWidth = measuredYLabels.maxOf { it.second.size.width }
        with(density) { (maxWidth + chartStyle.yLabelGap.toPx()).toDp() }
    } else 0.dp
    val xLabelHeight = if (hasXLabels) {
        val maxHeight = measuredXLabels.maxOf { it.second.size.height }
        with(density) {
            (
                maxHeight.toFloat() +
                    chartStyle.xLabelTopPadding.toPx() +
                    chartStyle.chartPadding.toPx() / 2f
                ).toDp()
        }
    } else 0.dp
    val gestureEdgeGuardPx = with(density) { chartStyle.gestureEdgeGuard.toPx() }

    // Compute per-point gradient color stops from quality zones
    val lineGradientColors = remember(data, qualityZones, lineColor) {
        buildTrendLineGradientStops(data, qualityZones, lineColor)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .then(
                if (contentDescription == null) Modifier
                else Modifier.semantics {
                    this.contentDescription = contentDescription
                    this.role = Role.Image
                }
            )
            .then(
                if (tooltipFormatter != null) {
                    Modifier
                        .pointerInput(data, yLabelWidth, gestureEdgeGuardPx) {
                            detectTapGestures { offset ->
                                if (sweepProgress.value < 1f) return@detectTapGestures
                                if (offset.x <= gestureEdgeGuardPx || offset.x >= size.width - gestureEdgeGuardPx) {
                                    return@detectTapGestures
                                }
                                val leftPad = yLabelWidth.toPx()
                                val chartPad = chartStyle.chartPadding.toPx()
                                val chartLeft = leftPad + chartPad
                                val chartWidth = size.width - chartLeft - chartPad
                                if (chartWidth > 0 && data.isNotEmpty()) {
                                    val fraction =
                                        ((offset.x - chartLeft) / chartWidth).coerceIn(0f, 1f)
                                    val index =
                                        (fraction * (data.size - 1)).toInt().coerceIn(0, data.lastIndex)
                                    selectedIndex = if (selectedIndex == index) -1 else index
                                }
                            }
                        }
                        .pointerInput(data, yLabelWidth, gestureEdgeGuardPx) {
                            var allowTooltipDrag = false
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    allowTooltipDrag = sweepProgress.value >= 1f &&
                                        offset.x > gestureEdgeGuardPx &&
                                        offset.x < size.width - gestureEdgeGuardPx
                                    if (allowTooltipDrag) {
                                        val leftPad = yLabelWidth.toPx()
                                        val chartPad = chartStyle.chartPadding.toPx()
                                        val chartLeft = leftPad + chartPad
                                        val chartWidth = size.width - chartLeft - chartPad
                                        if (chartWidth > 0 && data.isNotEmpty()) {
                                            val fraction =
                                                ((offset.x - chartLeft) / chartWidth).coerceIn(0f, 1f)
                                            selectedIndex = (fraction * (data.size - 1)).toInt()
                                                .coerceIn(0, data.lastIndex)
                                        }
                                    }
                                },
                                onDragEnd = { allowTooltipDrag = false },
                                onDragCancel = {
                                    allowTooltipDrag = false
                                    selectedIndex = -1
                                }
                            ) { change, _ ->
                                if (!allowTooltipDrag) return@detectHorizontalDragGestures
                                change.consume()
                                val leftPad = yLabelWidth.toPx()
                                val chartPad = chartStyle.chartPadding.toPx()
                                val chartLeft = leftPad + chartPad
                                val chartWidth = size.width - chartLeft - chartPad
                                if (chartWidth > 0 && data.isNotEmpty()) {
                                    val fraction =
                                        ((change.position.x - chartLeft) / chartWidth).coerceIn(
                                            0f,
                                            1f
                                        )
                                    selectedIndex = (fraction * (data.size - 1)).toInt()
                                        .coerceIn(0, data.lastIndex)
                                }
                            }
                        }
                } else Modifier
            )
    ) {
        val yLabelWidthPx = yLabelWidth.toPx()
        val xLabelHeightPx = xLabelHeight.toPx()
        val chartPad = chartStyle.chartPadding.toPx()
        val chartLeft = yLabelWidthPx + chartPad
        val chartTop = chartPad
        val chartWidth = size.width - chartLeft - chartPad
        val chartHeight = size.height - chartTop - chartPad - xLabelHeightPx
        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas
        val stepX = if (data.size > 1) chartWidth / (data.size - 1) else 0f
        val dataRight = when {
            data.isEmpty() -> chartLeft
            data.size == 1 -> chartLeft
            else -> chartLeft + stepX * (data.size - 1)
        }
        val sweepX = (chartLeft + chartWidth * sweepProgress.value).coerceIn(chartLeft, chartLeft + chartWidth)
        val visibleSweepRight = sweepX.coerceAtMost(dataRight)
        val tickColor = gridColor.copy(alpha = 0.75f)
        val shouldDrawPointMarkers = chartStyle.pointMarkerRadius > 0.dp &&
            stepX >= chartStyle.pointMarkerRadius.toPx() * 3f

        // ── Quality zone bands ─────────────────────────────────────────
        qualityZones?.forEach { zone ->
            val yBottom = chartTop + chartHeight - ((zone.minValue - minVal) / range * chartHeight)
            val yTop = chartTop + chartHeight - ((zone.maxValue - minVal) / range * chartHeight)
            val clampedTop = yTop.coerceIn(chartTop, chartTop + chartHeight)
            val clampedBottom = yBottom.coerceIn(chartTop, chartTop + chartHeight)
            if (clampedBottom > clampedTop) {
                drawRect(
                    color = zone.color.copy(alpha = zone.color.alpha * gridAlpha.value),
                    topLeft = Offset(chartLeft, clampedTop),
                    size = Size(chartWidth, clampedBottom - clampedTop)
                )
            }
        }

        // ── Grid lines ─────────────────────────────────────────────────
        if (showGrid && yLabels != null) {
            for (yLabel in yLabels) {
                val y = chartTop + chartHeight - ((yLabel.value - minVal) / range * chartHeight)
                if (y in chartTop..chartTop + chartHeight) {
                    drawLine(
                        color = gridColor.copy(alpha = gridColor.alpha * gridAlpha.value),
                        start = Offset(chartLeft, y),
                        end = Offset(chartLeft + chartWidth, y),
                        strokeWidth = chartStyle.gridStrokeWidth.toPx()
                    )
                }
            }
        }

        // ── Y-axis labels ──────────────────────────────────────────────
        if (yLabels != null) {
            for ((yLabel, measured) in measuredYLabels) {
                val y = chartTop + chartHeight - ((yLabel.value - minVal) / range * chartHeight)
                if (y in chartTop - chartPad..chartTop + chartHeight + chartPad) {
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            yLabelWidthPx - measured.size.width - 4.dp.toPx(),
                            y - measured.size.height / 2f
                        ),
                        alpha = gridAlpha.value
                    )
                }
            }
        }

        // ── X-axis labels ──────────────────────────────────────────────
        if (xLabels != null) {
            for ((xLabel, measured) in measuredXLabels) {
                val x = chartLeft + xLabel.position * chartWidth
                drawLine(
                    color = tickColor.copy(alpha = tickColor.alpha * gridAlpha.value),
                    start = Offset(x, chartTop + chartHeight),
                    end = Offset(x, chartTop + chartHeight + chartStyle.tickLength.toPx()),
                    strokeWidth = chartStyle.gridStrokeWidth.toPx()
                )
                val labelX = (x - measured.size.width / 2f)
                    .coerceIn(chartLeft, chartLeft + chartWidth - measured.size.width)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        labelX,
                        chartTop + chartHeight + chartStyle.xLabelTopPadding.toPx()
                    ),
                    alpha = gridAlpha.value
                )
            }
        }

        // ── Fading previous data (during data transitions) ──────────
        if (previousData.isNotEmpty() && fadeOutAlpha.value > 0f) {
            val prevStepX = if (previousData.size > 1) chartWidth / (previousData.size - 1) else chartWidth
            previousLinePath.reset()

            previousData.forEachIndexed { i, value ->
                val x = chartLeft + i * prevStepX
                val y = chartTop + chartHeight - ((value - previousMinVal) / previousRange * chartHeight)
                if (i == 0) previousLinePath.moveTo(x, y) else previousLinePath.lineTo(x, y)
            }

            drawPath(
                path = previousLinePath,
                color = lineColor.copy(alpha = fadeOutAlpha.value),
                style = Stroke(width = chartStyle.lineStrokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        // ── Data line ──────────────────────────────────────────────────
        linePath.reset()
        if (data.size >= 2 && visibleSweepRight > chartLeft) {
            for (i in 0 until data.lastIndex) {
                val x1 = chartLeft + i * stepX
                val x2 = chartLeft + (i + 1) * stepX
                val y1 = chartTop + chartHeight - ((data[i] - minVal) / range * chartHeight)
                val y2 = chartTop + chartHeight - ((data[i + 1] - minVal) / range * chartHeight)

                if (i == 0) linePath.moveTo(x1, y1)
                if (visibleSweepRight <= x1) break

                if (visibleSweepRight < x2) {
                    val fraction = ((visibleSweepRight - x1) / (x2 - x1)).coerceIn(0f, 1f)
                    linePath.lineTo(
                        visibleSweepRight,
                        lerp(y1, y2, fraction)
                    )
                    break
                }

                linePath.lineTo(x2, y2)
            }
        }

        // Clip rect for oscilloscope sweep reveal
        clipRect(
            left = chartLeft,
            top = chartTop,
            right = visibleSweepRight,
            bottom = chartTop + chartHeight
        ) {
            // Strip-based gradient fill — alpha proportional to data value height
            for (i in 0 until data.size - 1) {
                val x1 = chartLeft + i * stepX
                val x2 = chartLeft + (i + 1) * stepX
                if (visibleSweepRight <= x1) break
                val y1 = chartTop + chartHeight - ((data[i] - minVal) / range * chartHeight)
                val y2 = chartTop + chartHeight - ((data[i + 1] - minVal) / range * chartHeight)
                val visibleX2 = visibleSweepRight.coerceAtMost(x2)
                val segmentFraction = if (x2 > x1) {
                    ((visibleX2 - x1) / (x2 - x1)).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val visibleY2 = lerp(y1, y2, segmentFraction)
                val avgNormalizedY = ((data[i] - minVal) / range + (data[i + 1] - minVal) / range) / 2f
                val topAlpha = lerp(0.08f, 0.30f, avgNormalizedY)

                stripPath.reset()
                stripPath.moveTo(x1, y1)
                stripPath.lineTo(visibleX2, visibleY2)
                stripPath.lineTo(visibleX2, chartTop + chartHeight)
                stripPath.lineTo(x1, chartTop + chartHeight)
                stripPath.close()

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

            if (lineGradientColors != null) {
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(
                        colorStops = lineGradientColors,
                        startX = chartLeft,
                        endX = dataRight.coerceAtLeast(chartLeft + 1f)
                    ),
                    style = Stroke(width = chartStyle.lineStrokeWidth.toPx(), cap = StrokeCap.Round)
                )
            } else if (!linePath.isEmpty) {
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = chartStyle.lineStrokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }

            if (shouldDrawPointMarkers) {
                val innerMarkerRadius = (chartStyle.pointMarkerRadius - 1.5.dp).coerceAtLeast(1.5.dp)
                for (i in data.indices) {
                    val x = chartLeft + i * stepX
                    if (x > visibleSweepRight) break
                    val y = chartTop + chartHeight - ((data[i] - minVal) / range * chartHeight)
                    drawCircle(
                        color = lineColor.copy(alpha = 0.95f),
                        radius = chartStyle.pointMarkerRadius.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = tooltipBgColor,
                        radius = innerMarkerRadius.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

        }

        if (data.size == 1 && sweepProgress.value > 0f) {
            val y = chartTop + chartHeight - ((data.first() - minVal) / range * chartHeight)
            drawCircle(
                color = lineColor.copy(alpha = 0.95f),
                radius = chartStyle.selectedPointInnerRadius.toPx(),
                center = Offset(chartLeft, y)
            )
        }

        // Draw scan line
        if (scanLineAlpha.value > 0f && data.isNotEmpty() && sweepProgress.value < 1f) {
            drawLine(
                color = lineColor.copy(alpha = scanLineAlpha.value),
                start = Offset(sweepX.coerceAtMost(chartLeft + chartWidth), chartTop),
                end = Offset(sweepX.coerceAtMost(chartLeft + chartWidth), chartTop + chartHeight),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // ── Last value emphasis ──────────────────────────────────────────
        if (emphasisData == data && data.isNotEmpty() && emphasisAlpha.value > 0f) {
            val lastIndex = data.lastIndex
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

        // ── Tooltip cursor ─────────────────────────────────────────────
        if (selectedIndex in 0..data.lastIndex && tooltipFormatter != null) {
            val sx = chartLeft + selectedIndex * stepX
            val sy = chartTop + chartHeight - ((data[selectedIndex] - minVal) / range * chartHeight)

            // Vertical cursor line
            drawLine(
                color = tooltipLineColor,
                start = Offset(sx, chartTop),
                end = Offset(sx, chartTop + chartHeight),
                strokeWidth = 1.dp.toPx()
            )

            // Data point dot
            drawCircle(
                color = lineColor,
                radius = chartStyle.selectedPointOuterRadius.toPx(),
                center = Offset(sx, sy)
            )
            drawCircle(
                color = tooltipBgColor,
                radius = chartStyle.selectedPointInnerRadius.toPx(),
                center = Offset(sx, sy)
            )

            // Tooltip label
            val tooltipText = tooltipFormatter(selectedIndex)
            val tooltipMeasured = textMeasurer.measure(tooltipText, tooltipLabelStyle)
            val tooltipPadH = 8.dp.toPx()
            val tooltipPadV = 4.dp.toPx()
            val tooltipW = tooltipMeasured.size.width + tooltipPadH * 2
            val tooltipH = tooltipMeasured.size.height + tooltipPadV * 2

            // Position tooltip above point, flip if near top
            val tooltipX = (sx - tooltipW / 2)
                .coerceIn(chartLeft, chartLeft + chartWidth - tooltipW)
            val tooltipY = if (sy - tooltipH - 8.dp.toPx() > chartTop) {
                sy - tooltipH - 8.dp.toPx()
            } else {
                sy + 12.dp.toPx()
            }

            drawRoundRect(
                color = tooltipBgColor,
                topLeft = Offset(tooltipX, tooltipY),
                size = Size(tooltipW, tooltipH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
            )
            drawRoundRect(
                color = lineColor.copy(alpha = 0.4f),
                topLeft = Offset(tooltipX, tooltipY),
                size = Size(tooltipW, tooltipH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawText(
                textLayoutResult = tooltipMeasured,
                topLeft = Offset(tooltipX + tooltipPadH, tooltipY + tooltipPadV)
            )
        }
    }
}
