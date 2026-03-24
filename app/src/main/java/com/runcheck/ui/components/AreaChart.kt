package com.runcheck.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.runcheck.ui.theme.reducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AreaChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    if (data.size < 2) return

    val reducedMotion = MaterialTheme.reducedMotion

    // Sweep animation state
    val sweepProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val scanLineAlpha = remember { Animatable(if (reducedMotion) 0f else 0.5f) }

    LaunchedEffect(data, reducedMotion) {
        if (reducedMotion) {
            sweepProgress.snapTo(1f)
            scanLineAlpha.snapTo(0f)
            return@LaunchedEffect
        }
        sweepProgress.snapTo(0f)
        scanLineAlpha.snapTo(0.5f)
        launch {
            delay(560) // 70% of 800ms
            scanLineAlpha.animateTo(0f, tween(240))
        }
        sweepProgress.animateTo(
            1f,
            tween(800, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f))
        )
    }

    val minVal = data.min()
    val maxVal = data.max()
    val range = (maxVal - minVal).coerceAtLeast(1f)

    val linePath = remember { Path() }
    val stripPath = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (contentDescription == null) Modifier
                else Modifier.semantics {
                    this.contentDescription = contentDescription
                    this.role = Role.Image
                }
            )
    ) {
        val verticalPadding = size.height * 0.1f
        val chartHeight = size.height - verticalPadding * 2
        val stepX = size.width / (data.size - 1)

        // Build the full line path for all data points
        linePath.reset()
        for (i in data.indices) {
            val x = i * stepX
            val y = verticalPadding + chartHeight - ((data[i] - minVal) / range * chartHeight)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        val sweepX = sweepProgress.value * size.width

        // Clip-based sweep rendering
        clipRect(left = 0f, top = 0f, right = sweepX, bottom = size.height) {
            // Draw strip-based gradient fill
            for (i in 0 until data.size - 1) {
                val x1 = i * stepX
                val x2 = (i + 1) * stepX
                val y1 = verticalPadding + chartHeight - ((data[i] - minVal) / range * chartHeight)
                val y2 = verticalPadding + chartHeight - ((data[i + 1] - minVal) / range * chartHeight)
                val avgNormalizedY = ((data[i] - minVal) / range + (data[i + 1] - minVal) / range) / 2f
                val topAlpha = lerp(0.08f, 0.25f, avgNormalizedY) // Slightly lower max than TrendChart

                stripPath.reset()
                stripPath.moveTo(x1, y1)
                stripPath.lineTo(x2, y2)
                stripPath.lineTo(x2, size.height)
                stripPath.lineTo(x1, size.height)
                stripPath.close()

                drawPath(
                    path = stripPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = topAlpha),
                            lineColor.copy(alpha = 0.02f)
                        ),
                        startY = minOf(y1, y2),
                        endY = size.height
                    )
                )
            }

            // Draw line stroke
            drawPath(
                path = linePath,
                color = lineColor.copy(alpha = 0.7f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )
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
    }
}
