package com.runcheck.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runcheck.ui.theme.chartAxisTextStyle
import com.runcheck.ui.theme.chartTooltipTextStyle
import com.runcheck.ui.theme.numericFontFamily
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
    tooltipFormatter: ((index: Int) -> String)? = null
) {
    if (data.size < 2) return

    val reducedMotion = MaterialTheme.reducedMotion
    var progress by remember(data) { mutableFloatStateOf(0f) }
    LaunchedEffect(data) { progress = 1f }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 800),
        label = "trend_draw"
    )

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
    val labelStyle = MaterialTheme.chartAxisTextStyle.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val tooltipLabelStyle = MaterialTheme.chartTooltipTextStyle.copy(
        color = MaterialTheme.colorScheme.onSurface
    )

    // Calculate left padding for Y-axis labels
    val yLabelWidth = if (hasYLabels) {
        val maxWidth = yLabels.maxOf { textMeasurer.measure(it.label, labelStyle).size.width }
        with(LocalDensity.current) { (maxWidth + 6.dp.toPx()).toDp() }
    } else 0.dp

    val xLabelHeight = if (hasXLabels) 16.dp else 0.dp

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    val tooltipBgColor = MaterialTheme.colorScheme.surfaceContainer
    val tooltipLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight + xLabelHeight)
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
                        .pointerInput(data) {
                            detectTapGestures { offset ->
                                val leftPad = yLabelWidth.toPx()
                                val chartPad = 8.dp.toPx()
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
                        .pointerInput(data) {
                            detectHorizontalDragGestures(
                                onDragEnd = { /* keep selection visible */ },
                                onDragCancel = { selectedIndex = -1 }
                            ) { change, _ ->
                                change.consume()
                                val leftPad = yLabelWidth.toPx()
                                val chartPad = 8.dp.toPx()
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
        val chartPad = 8.dp.toPx()
        val chartLeft = yLabelWidthPx + chartPad
        val chartTop = chartPad
        val chartWidth = size.width - chartLeft - chartPad
        val chartHeight = size.height - chartTop - chartPad - xLabelHeightPx
        val stepX = chartWidth / (data.size - 1)
        val visibleCount = (data.size * animatedProgress).toInt().coerceAtLeast(2)

        // ── Quality zone bands ─────────────────────────────────────────
        qualityZones?.forEach { zone ->
            val yBottom = chartTop + chartHeight - ((zone.minValue - minVal) / range * chartHeight)
            val yTop = chartTop + chartHeight - ((zone.maxValue - minVal) / range * chartHeight)
            val clampedTop = yTop.coerceIn(chartTop, chartTop + chartHeight)
            val clampedBottom = yBottom.coerceIn(chartTop, chartTop + chartHeight)
            if (clampedBottom > clampedTop) {
                drawRect(
                    color = zone.color,
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
                        color = gridColor,
                        start = Offset(chartLeft, y),
                        end = Offset(chartLeft + chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        // ── Y-axis labels ──────────────────────────────────────────────
        if (yLabels != null) {
            for (yLabel in yLabels) {
                val y = chartTop + chartHeight - ((yLabel.value - minVal) / range * chartHeight)
                if (y in chartTop - chartPad..chartTop + chartHeight + chartPad) {
                    val measured = textMeasurer.measure(yLabel.label, labelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            yLabelWidthPx - measured.size.width - 4.dp.toPx(),
                            y - measured.size.height / 2f
                        )
                    )
                }
            }
        }

        // ── X-axis labels ──────────────────────────────────────────────
        if (xLabels != null) {
            for (xLabel in xLabels) {
                val x = chartLeft + xLabel.position * chartWidth
                val measured = textMeasurer.measure(xLabel.label, labelStyle)
                val labelX = (x - measured.size.width / 2f)
                    .coerceIn(chartLeft, chartLeft + chartWidth - measured.size.width)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(labelX, chartTop + chartHeight + 4.dp.toPx())
                )
            }
        }

        // ── Data line ──────────────────────────────────────────────────
        linePath.reset()
        for (i in 0 until visibleCount) {
            val x = chartLeft + i * stepX
            val y = chartTop + chartHeight - ((data[i] - minVal) / range * chartHeight)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        fillPath.reset()
        fillPath.addPath(linePath)
        val lastX = chartLeft + (visibleCount - 1) * stepX
        fillPath.lineTo(lastX, chartTop + chartHeight)
        fillPath.lineTo(chartLeft, chartTop + chartHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = chartTop,
                endY = chartTop + chartHeight
            ),
            style = Fill,
            alpha = animatedProgress
        )

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

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
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(sx, sy))
            drawCircle(
                color = tooltipBgColor,
                radius = 2.dp.toPx(),
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
