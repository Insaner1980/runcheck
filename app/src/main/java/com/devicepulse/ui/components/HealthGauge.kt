package com.devicepulse.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devicepulse.ui.theme.reducedMotion

@Composable
fun HealthGauge(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 12.dp
) {
    val reducedMotion = MaterialTheme.reducedMotion

    val targetSweep = (score / 100f) * 360f
    var animTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(score) {
        animTarget = targetSweep
    }

    val sweepAngle by animateFloatAsState(
        targetValue = if (reducedMotion) targetSweep else animTarget,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 0)
        } else {
            tween(
                durationMillis = 850,
                easing = FastOutSlowInEasing
            )
        },
        label = "gauge_sweep"
    )

    val accentColor = MaterialTheme.colorScheme.primary
    val trackColor = accentColor.copy(alpha = 0.28f)
    val innerColor = accentColor.copy(alpha = 0.16f)
    val progressBrush = rememberGaugeSweepBrush(accentColor)
    val innerBrush = rememberGaugeSweepBrush(innerColor)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val progressStroke = strokeWidth.toPx()
            val outerStroke = progressStroke * 0.25f

            val progressInset = progressStroke / 2
            val progressArcSize = Size(
                this.size.width - progressInset * 2,
                this.size.height - progressInset * 2
            )
            val progressTopLeft = Offset(progressInset, progressInset)

            val innerInset = progressStroke + outerStroke / 2
            val innerArcSize = Size(
                this.size.width - innerInset * 2,
                this.size.height - innerInset * 2
            )
            val innerTopLeft = Offset(innerInset, innerInset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = progressTopLeft,
                size = progressArcSize,
                style = Stroke(width = progressStroke, cap = StrokeCap.Butt)
            )

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerArcSize,
                style = Stroke(width = outerStroke, cap = StrokeCap.Butt)
            )

            drawArc(
                brush = progressBrush,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = progressTopLeft,
                size = progressArcSize,
                style = Stroke(width = progressStroke, cap = StrokeCap.Round)
            )

            drawArc(
                brush = innerBrush,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerArcSize,
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedIntText(
                value = score,
                suffix = "%",
                style = MaterialTheme.typography.displayLarge,
            )
        }
    }
}

private fun rememberGaugeSweepBrush(baseColor: Color): Brush {
    val seamStart = lerp(baseColor, Color.White, 0.02f)
    val highlight = lerp(baseColor, Color.White, 0.05f)
    val middle = baseColor
    val shadow = lerp(baseColor, Color.Black, 0.03f)
    val seamEnd = lerp(baseColor, Color.White, 0.018f)
    return Brush.sweepGradient(
        0.0f to seamStart,
        0.18f to highlight,
        0.52f to middle,
        0.82f to shadow,
        1.0f to seamEnd
    )
}
