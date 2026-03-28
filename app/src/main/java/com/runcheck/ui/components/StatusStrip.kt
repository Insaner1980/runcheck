package com.runcheck.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a vertical status strip on the left edge of the composable.
 * The strip uses rounded corners on the left side to follow card shapes.
 */
fun Modifier.statusStrip(
    color: Color,
    width: Dp = 4.dp,
    cornerRadius: Dp = 16.dp,
): Modifier =
    this.drawBehind {
        val stripWidth = width.toPx()
        val radius = cornerRadius.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(stripWidth, size.height),
            cornerRadius = CornerRadius(radius, radius),
        )
    }
