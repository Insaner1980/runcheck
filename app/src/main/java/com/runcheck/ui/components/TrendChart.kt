package com.devicepulse.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devicepulse.ui.theme.reducedMotion

@Composable
fun TrendChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    contentDescription: String? = null
) {
    if (data.size < 2) return

    val reducedMotion = MaterialTheme.reducedMotion
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(data) { progress = 1f }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 800),
        label = "trend_draw"
    )

    val minVal = data.min()
    val maxVal = data.max()
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .then(
                if (contentDescription == null) Modifier
                else Modifier.semantics { this.contentDescription = contentDescription }
            )
    ) {
        val padding = 8.dp.toPx()
        val chartWidth = size.width - padding * 2
        val chartHeight = size.height - padding * 2
        val stepX = chartWidth / (data.size - 1)
        val visibleCount = (data.size * animatedProgress).toInt().coerceAtLeast(2)

        // Line path
        val linePath = Path()
        for (i in 0 until visibleCount) {
            val x = padding + i * stepX
            val y = padding + chartHeight - ((data[i] - minVal) / range * chartHeight)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        // Fill path
        val fillPath = Path().apply {
            addPath(linePath)
            val lastX = padding + (visibleCount - 1) * stepX
            lineTo(lastX, padding + chartHeight)
            lineTo(padding, padding + chartHeight)
            close()
        }

        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = size.height
            ),
            style = Fill,
            alpha = animatedProgress
        )

        // Draw line
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
