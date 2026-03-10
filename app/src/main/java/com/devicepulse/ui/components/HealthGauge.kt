package com.devicepulse.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devicepulse.domain.model.HealthStatus
import com.devicepulse.ui.theme.reducedMotion
import com.devicepulse.ui.theme.statusColors

@Composable
fun HealthGauge(
    score: Int,
    status: HealthStatus,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 12.dp
) {
    val reducedMotion = MaterialTheme.reducedMotion
    val statusColors = MaterialTheme.statusColors

    val targetSweep = (score / 100f) * 270f
    var animTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(score) {
        animTarget = targetSweep
    }

    val sweepAngle by animateFloatAsState(
        targetValue = if (reducedMotion) targetSweep else animTarget,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 0)
        } else {
            spring(dampingRatio = 0.7f, stiffness = 200f)
        },
        label = "gauge_sweep"
    )

    val arcColor by androidx.compose.animation.animateColorAsState(
        targetValue = when (status) {
            HealthStatus.HEALTHY -> statusColors.healthy
            HealthStatus.FAIR -> statusColors.fair
            HealthStatus.POOR -> statusColors.poor
            HealthStatus.CRITICAL -> statusColors.critical
        },
        animationSpec = tween(durationMillis = 300),
        label = "gauge_color"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(
                this.size.width - stroke,
                this.size.height - stroke
            )
            val topLeft = Offset(stroke / 2, stroke / 2)

            // Track arc
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Value arc
            drawArc(
                color = arcColor,
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Text(
            text = score.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
