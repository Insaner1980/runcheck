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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devicepulse.ui.theme.reducedMotion

@Composable
fun SparklineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    if (data.size < 2) return

    val reducedMotion = MaterialTheme.reducedMotion
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(data) { progress = 1f }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 600),
        label = "sparkline_draw"
    )

    val minVal = data.min()
    val maxVal = data.max()
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .then(
                if (contentDescription == null) Modifier
                else Modifier.semantics { this.contentDescription = contentDescription }
            )
    ) {
        val stepX = size.width / (data.size - 1)
        val visibleCount = (data.size * animatedProgress).toInt().coerceAtLeast(2)

        val path = Path()
        for (i in 0 until visibleCount) {
            val x = i * stepX
            val y = size.height - ((data[i] - minVal) / range * size.height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
