package com.devicepulse.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.devicepulse.ui.theme.reducedMotion
import com.devicepulse.ui.theme.statusColors

@Composable
fun HeatStrip(
    temperatureC: Float,
    modifier: Modifier = Modifier,
    minTemp: Float = 15f,
    maxTemp: Float = 50f
) {
    val normalizedTemp = ((temperatureC - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
    val isCritical = temperatureC > 42f
    val reducedMotion = MaterialTheme.reducedMotion
    val pulseAlpha = if (reducedMotion) {
        1f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "heat_pulse")
        infiniteTransition.animateFloat(
            initialValue = if (isCritical) 0.7f else 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "heat_alpha"
        ).value
    }

    val coolColor = MaterialTheme.statusColors.healthy
    val warmColor = MaterialTheme.statusColors.fair
    val hotColor = MaterialTheme.statusColors.critical
    val indicatorColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Background gradient
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(coolColor, warmColor, hotColor)
            ),
            alpha = if (isCritical) pulseAlpha else 1f
        )

        // Indicator position
        val indicatorX = normalizedTemp * size.width
        drawCircle(
            color = indicatorColor,
            radius = 8.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                indicatorX.coerceIn(8.dp.toPx(), size.width - 8.dp.toPx()),
                size.height / 2
            )
        )
    }
}
