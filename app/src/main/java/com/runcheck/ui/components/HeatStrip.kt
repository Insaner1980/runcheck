package com.runcheck.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.runcheck.R
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.ui.common.formatTemperature
import com.runcheck.ui.common.temperatureBandLabel
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.statusColors

@Composable
fun HeatStrip(
    temperatureC: Float,
    modifier: Modifier = Modifier,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    minTempC: Float = 15f,
    maxTempC: Float = 50f,
) {
    val normalizedTemp = ((temperatureC - minTempC) / (maxTempC - minTempC)).coerceIn(0f, 1f)
    val isCritical = temperatureC > 42f
    val reducedMotion = MaterialTheme.reducedMotion
    val stripContentDescription =
        stringResource(
            R.string.a11y_heat_strip,
            formatTemperature(temperatureC, temperatureUnit),
            temperatureBandLabel(temperatureC),
        )

    val pulseProgress =
        if (isCritical && !reducedMotion) {
            val infiniteTransition = rememberInfiniteTransition(label = "heat_pulse")
            infiniteTransition
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(durationMillis = MotionTokens.CONTINUOUS, easing = MotionTokens.EaseOut),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "heat_alpha",
                ).value
        } else {
            1f
        }

    val healthyColor = MaterialTheme.statusColors.healthy
    val fairColor = MaterialTheme.statusColors.fair
    val poorColor = MaterialTheme.statusColors.poor
    val criticalColor = MaterialTheme.statusColors.critical
    val indicatorColor = MaterialTheme.colorScheme.onSurface

    // Color stops aligned with statusColorForTemperature thresholds (35/40/45°C)
    // with ±1°C soft transition zones for smooth blending
    val rangeC = maxTempC - minTempC

    fun tempToStop(tempC: Float) = ((tempC - minTempC) / rangeC).coerceIn(0f, 1f)

    val transitionHalf = 1f / rangeC // ~1°C in normalized units

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(MaterialTheme.shapes.large)
                .semantics {
                    contentDescription = stripContentDescription
                },
    ) {
        drawRect(
            brush =
                Brush.horizontalGradient(
                    colorStops =
                        arrayOf(
                            0f to healthyColor,
                            (tempToStop(35f) - transitionHalf) to healthyColor,
                            (tempToStop(35f) + transitionHalf) to fairColor,
                            (tempToStop(40f) - transitionHalf) to fairColor,
                            (tempToStop(40f) + transitionHalf) to poorColor,
                            (tempToStop(45f) - transitionHalf) to poorColor,
                            (tempToStop(45f) + transitionHalf) to criticalColor,
                            1f to criticalColor,
                        ),
                ),
        )

        val indicatorX = normalizedTemp * size.width
        val indicatorCenter =
            androidx.compose.ui.geometry.Offset(
                indicatorX.coerceIn(8.dp.toPx(), size.width - 8.dp.toPx()),
                size.height / 2,
            )
        if (isCritical && !reducedMotion) {
            drawCircle(
                color = criticalColor.copy(alpha = lerp(0.26f, 0.08f, pulseProgress)),
                radius = lerp(9f, 13f, pulseProgress).dp.toPx(),
                center = indicatorCenter,
            )
        }
        drawCircle(
            color = indicatorColor,
            radius = 8.dp.toPx(),
            center = indicatorCenter,
        )
    }
}
