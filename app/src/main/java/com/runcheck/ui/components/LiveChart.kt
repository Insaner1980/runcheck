package com.runcheck.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.spacing
import kotlinx.coroutines.launch

/**
 * Real-time scrolling sparkline chart.
 *
 * Displays the most recent [maxPoints] values as a smooth line with gradient fill.
 * New data points enter from the right edge; older points scroll left.
 *
 * @param data Recent values to display. The chart shows up to [maxPoints] entries.
 * @param currentValueLabel Formatted string of the latest value shown at right (e.g., "-476 mA").
 * @param modifier Layout modifier.
 * @param lineColor Color for the line stroke and fill gradient.
 * @param chartHeight Height of the chart canvas.
 * @param maxPoints Maximum visible data points (controls time window).
 * @param yMin Optional fixed minimum for Y axis. Auto-scaled from data if null.
 * @param yMax Optional fixed maximum for Y axis. Auto-scaled from data if null.
 * @param label Optional label shown top-left (e.g., "Current" or "Power").
 * @param accessibilityDescription Content description for screen readers.
 */
@Composable
fun LiveChart(
    data: List<Float>,
    currentValueLabel: String,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    chartHeight: Dp = 80.dp,
    maxPoints: Int = 60,
    yMin: Float? = null,
    yMax: Float? = null,
    label: String? = null,
    accessibilityDescription: String? = null,
) {
    val visibleData =
        remember(data, maxPoints) {
            if (data.size > maxPoints) data.takeLast(maxPoints) else data
        }

    val reducedMotion = MaterialTheme.reducedMotion

    Column(modifier = modifier) {
        // Header row: label + current value
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            Text(
                text = currentValueLabel,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontFamily = MaterialTheme.numericFontFamily,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

        // Chart canvas
        LiveChartCanvas(
            data = visibleData,
            lineColor = lineColor,
            maxPoints = maxPoints,
            yMin = yMin,
            yMax = yMax,
            accessibilityDescription = accessibilityDescription,
            reducedMotion = reducedMotion,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
        )
    }
}

@Composable
private fun LiveChartCanvas(
    data: List<Float>,
    lineColor: Color,
    maxPoints: Int,
    yMin: Float?,
    yMax: Float?,
    accessibilityDescription: String?,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
) {
    if (data.size < 2) {
        // Not enough data — show empty placeholder
        Box(modifier = modifier)
        return
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)

    // Compute Y range
    val computedMin = yMin ?: data.min()
    val computedMax = yMax ?: data.max()
    val range = (computedMax - computedMin).coerceAtLeast(0.1f)
    // Add 10% padding
    val paddedMin = computedMin - range * 0.1f
    val paddedMax = computedMax + range * 0.1f
    val paddedRange = paddedMax - paddedMin

    val linePath = remember { Path() }
    val fillPath = remember { Path() }

    // Smooth scroll animation state
    val animatedScrollOffset = remember { Animatable(0f) }
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var previousData by remember { mutableStateOf<List<Float>>(emptyList()) }

    // Glow pulse animation state
    val glowPulseAlpha = remember { Animatable(0.3f) }
    val glowPulseRadius = remember { Animatable(5f) }

    LaunchedEffect(data, canvasWidth, reducedMotion, maxPoints) {
        val hadVisibleChart = previousData.size >= 2
        val hasVisibleChart = data.size >= 2
        val dataChanged = previousData != data
        val newPointCount = appendedPointCount(previousData, data)

        if (reducedMotion || !hadVisibleChart || !hasVisibleChart || !dataChanged) {
            animatedScrollOffset.snapTo(0f)
            glowPulseAlpha.snapTo(0.3f)
            glowPulseRadius.snapTo(5f)
            previousData = data.toList()
            return@LaunchedEffect
        }

        if (newPointCount > 0 && canvasWidth > 0f) {
            val stepX = if (maxPoints > 1) canvasWidth / (maxPoints - 1) else canvasWidth
            animatedScrollOffset.stop()
            animatedScrollOffset.snapTo(animatedScrollOffset.value + (newPointCount * stepX))
            launch {
                animatedScrollOffset.animateTo(0f, tween(MotionTokens.SCROLL, easing = LinearEasing))
            }
        } else {
            animatedScrollOffset.snapTo(0f)
        }

        glowPulseAlpha.stop()
        glowPulseRadius.stop()
        glowPulseAlpha.snapTo(0.5f)
        glowPulseRadius.snapTo(8f)
        launch { glowPulseAlpha.animateTo(0.3f, tween(MotionTokens.MEDIUM)) }
        glowPulseRadius.animateTo(5f, tween(MotionTokens.MEDIUM))

        previousData = data.toList()
    }

    Canvas(
        modifier =
            modifier
                .onSizeChanged { canvasWidth = it.width.toFloat() }
                .then(
                    if (accessibilityDescription == null) {
                        Modifier
                    } else {
                        Modifier.semantics {
                            this.contentDescription = accessibilityDescription
                            this.role = Role.Image
                        }
                    },
                ),
    ) {
        val chartLeft = 0f
        val chartRight = size.width
        val chartTop = 0f
        val chartBottom = size.height
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        // Draw horizontal grid lines (3 lines: 25%, 50%, 75%)
        for (fraction in listOf(0.25f, 0.5f, 0.75f)) {
            val y = chartBottom - chartHeight * fraction
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 0.5f.dp.toPx(),
            )
        }

        // Build line path
        // Points are evenly spaced, right-aligned: newest at right edge
        val stepX = if (maxPoints > 1) chartWidth / (maxPoints - 1) else chartWidth
        val baseOffsetX = (maxPoints - data.size) * stepX
        val totalOffsetX = baseOffsetX + animatedScrollOffset.value

        linePath.reset()
        for (i in data.indices) {
            val x = totalOffsetX + i * stepX
            val normalized = (data[i] - paddedMin) / paddedRange
            val y = chartBottom - normalized * chartHeight
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        // Build fill path (line + bottom)
        fillPath.reset()
        fillPath.addPath(linePath)
        val lastX = totalOffsetX + (data.size - 1) * stepX
        val firstX = totalOffsetX
        fillPath.lineTo(lastX, chartBottom)
        fillPath.lineTo(firstX, chartBottom)
        fillPath.close()

        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            lineColor.copy(alpha = 0.20f),
                            lineColor.copy(alpha = 0.02f),
                        ),
                    startY = chartTop,
                    endY = chartBottom,
                ),
            style = Fill,
        )

        // Draw line
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 1.5f.dp.toPx(), cap = StrokeCap.Round),
        )

        // Draw current value dot at the end
        if (data.isNotEmpty()) {
            val lastValue = data.last()
            val dotX = totalOffsetX + (data.size - 1) * stepX
            val dotNorm = (lastValue - paddedMin) / paddedRange
            val dotY = chartBottom - dotNorm * chartHeight

            // Animated pulse glow (replaces static outer glow)
            drawCircle(
                color = lineColor.copy(alpha = glowPulseAlpha.value),
                radius = glowPulseRadius.value.dp.toPx(),
                center = Offset(dotX, dotY),
            )
            // Inner dot
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(dotX, dotY),
            )
        }
    }
}

private fun appendedPointCount(
    previous: List<Float>,
    current: List<Float>,
): Int {
    if (previous.isEmpty() || current.isEmpty() || previous == current) return 0

    val maxOverlap = minOf(previous.size, current.size)
    for (overlap in maxOverlap downTo 1) {
        if (previous.takeLast(overlap) == current.take(overlap)) {
            return current.size - overlap
        }
    }

    return 0
}
