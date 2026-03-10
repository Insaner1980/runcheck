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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HeatStrip(
    temperatureC: Float,
    modifier: Modifier = Modifier,
    minTemp: Float = 15f,
    maxTemp: Float = 50f
) {
    val normalizedTemp = ((temperatureC - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)

    val isCritical = temperatureC > 42f

    val infiniteTransition = rememberInfiniteTransition(label = "heat_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (isCritical) 0.7f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heat_alpha"
    )

    val coolColor = Color(0xFF3B82F6) // Blue
    val warmColor = Color(0xFFFBBF24) // Yellow
    val hotColor = Color(0xFFEF4444)  // Red

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
            color = Color.White,
            radius = 8.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                indicatorX.coerceIn(8.dp.toPx(), size.width - 8.dp.toPx()),
                size.height / 2
            )
        )
    }
}
