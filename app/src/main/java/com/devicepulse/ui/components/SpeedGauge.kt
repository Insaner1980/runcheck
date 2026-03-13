package com.devicepulse.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devicepulse.R
import com.devicepulse.ui.common.formatDecimal
import com.devicepulse.ui.theme.reducedMotion

@Composable
fun SpeedGauge(
    value: Double,
    maxValue: Double,
    label: String,
    unit: String,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    strokeWidth: Dp = 10.dp
) {
    val reducedMotion = MaterialTheme.reducedMotion
    val fraction = if (maxValue > 0) (value / maxValue).toFloat().coerceIn(0f, 1f) else 0f
    val targetSweep = fraction * 270f
    val spokenValue = if (value > 0) formatDecimal(value, 1) else stringResource(R.string.not_available)
    val gaugeContentDescription = stringResource(R.string.a11y_speed_gauge, label, spokenValue, unit)

    val sweepAngle by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 0)
        } else {
            tween(
                durationMillis = 700,
                easing = FastOutSlowInEasing
            )
        },
        label = "speed_gauge_sweep"
    )

    val arcColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                contentDescription = gaugeContentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(
                this.size.width - stroke,
                this.size.height - stroke
            )
            val topLeft = Offset(stroke / 2, stroke / 2)

            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (value > 0) {
                AnimatedFloatText(
                    value = value.toFloat(),
                    style = MaterialTheme.typography.bodyLarge,
                    decimalPlaces = 1,
                    modifier = Modifier,
                    suffix = ""
                )
            } else {
                Text(
                    text = stringResource(R.string.placeholder_dash),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
