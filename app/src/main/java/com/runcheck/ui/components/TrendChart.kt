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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
    if (data.size < 2) return

    val reducedMotion = MaterialTheme.reducedMotion

    // Phase 1: Grid + axes fade in (0→1 over 200ms)
    val gridAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    // Phase 2: Oscilloscope sweep progress (0→1 over 1000ms)
    val sweepProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    // Phase 3: Last value emphasis fade in (0→1 over 200ms)
    val emphasisAlpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    // Scan line opacity (fades out during final 30% of sweep)
    val scanLineAlpha = remember { Animatable(if (reducedMotion) 0f else 0.5f) }

    LaunchedEffect(data, reducedMotion) {
        if (reducedMotion) {
            gridAlpha.snapTo(1f)
            sweepProgress.snapTo(1f)
            emphasisAlpha.snapTo(1f)
            scanLineAlpha.snapTo(0f)
            return@LaunchedEffect
        }
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

    val minVal = data.min()
    val maxVal = data.max()
    val range = (maxVal - minVal).coerceAtLeast(1f)

    val linePath = remember { Path() }
    val fillPath = remember { Path() }

    // Tooltip state: -1 means no selection
    var selectedIndex by remember { mutableIntStateOf(-1) }

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
        if (qualityZones.isNullOrEmpty() || data.isEmpty()) null
        else {
            data.mapIndexed { index, value ->
                val fraction = if (data.size <= 1) 0f else index.toFloat() / (data.size - 1)
                val color = qualityZoneColorForValue(value, qualityZones, lineColor)
                fraction to color
            }
        }
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
        val stepX = chartWidth / (data.size - 1)
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

        // ── Data line ──────────────────────────────────────────────────
        // Build full paths for all data points — sweep clip controls visibility
        linePath.reset()
        for (i in data.indices) {
            val x = chartLeft + i * stepX
            val y = chartTop + chartHeight - ((data[i] - minVal) / range * chartHeight)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        fillPath.reset()
        fillPath.addPath(linePath)
        val lastX = chartLeft + (data.size - 1) * stepX
        fillPath.lineTo(lastX, chartTop + chartHeight)
        fillPath.lineTo(chartLeft, chartTop + chartHeight)
        fillPath.close()

        // Calculate sweep X position
        val sweepX = chartLeft + chartWidth * sweepProgress.value

        // Clip rect for oscilloscope sweep reveal
        clipRect(
            left = chartLeft,
            top = chartTop,
            right = sweepX,
            bottom = chartTop + chartHeight
        ) {
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillColor, Color.Transparent),
                    startY = chartTop,
                    endY = chartTop + chartHeight
                ),
                style = Fill
            )

            if (lineGradientColors != null) {
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(
                        colorStops = lineGradientColors.toTypedArray()
                    ),
                    style = Stroke(width = chartStyle.lineStrokeWidth.toPx(), cap = StrokeCap.Round)
                )
            } else {
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

        // Draw scan line
        if (scanLineAlpha.value > 0f) {
            drawLine(
                color = lineColor.copy(alpha = scanLineAlpha.value),
                start = Offset(sweepX, chartTop),
                end = Offset(sweepX, chartTop + chartHeight),
                strokeWidth = 1.5.dp.toPx()
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
