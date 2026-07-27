package com.runcheck.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a vertical status strip on the left edge of the composable.
 * The parent card owns clipping and corner geometry.
 */
fun Modifier.statusStrip(
    color: Color,
    width: Dp = 4.dp,
): Modifier =
    this.drawBehind {
        val stripWidth = width.toPx()
        drawRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(stripWidth, size.height),
        )
    }
