package com.runcheck.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.runcheck.ui.theme.reducedMotion

@Composable
fun AreaChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    if (data.size < 2) return

    val reducedMotion = MaterialTheme.reducedMotion
    var progress by remember(data) { mutableFloatStateOf(0f) }
    LaunchedEffect(data) { progress = 1f }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 800),
        label = "area_chart_draw"
    )

    val minVal = data.min()
    val maxVal = data.max()
    val range = (maxVal - minVal).coerceAtLeast(1f)

    val linePath = remember { Path() }
    val fillPath = remember { Path() }

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
        val visibleCount = (data.size * animatedProgress).toInt().coerceAtLeast(2)

        linePath.reset()
        for (i in 0 until visibleCount) {
            val x = i * stepX
            val y = verticalPadding + chartHeight - ((data[i] - minVal) / range * chartHeight)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        fillPath.reset()
        fillPath.addPath(linePath)
        val lastX = (visibleCount - 1) * stepX
        fillPath.lineTo(lastX, size.height)
        fillPath.lineTo(0f, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.25f),
                    lineColor.copy(alpha = 0.02f)
                ),
                startY = 0f,
                endY = size.height
            ),
            style = Fill
        )

        drawPath(
            path = linePath,
            color = lineColor.copy(alpha = 0.7f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
