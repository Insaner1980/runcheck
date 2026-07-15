package com.runcheck.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawSweepHead(
    x: Float,
    top: Float,
    bottom: Float,
    color: Color,
    alpha: Float,
    leftBound: Float = 0f,
) {
    if (alpha <= 0f || bottom <= top) return

    val headX = x.coerceIn(leftBound, size.width)
    val trailWidth = 32.dp.toPx()
    val trailLeft = (headX - trailWidth).coerceAtLeast(leftBound)
    val visibleTrailWidth = (headX - trailLeft).coerceAtLeast(0f)

    if (visibleTrailWidth > 0f) {
        drawRect(
            brush =
                Brush.horizontalGradient(
                    colors =
                        listOf(
                            Color.Transparent,
                            color.copy(alpha = alpha * 0.08f),
                            color.copy(alpha = alpha * 0.22f),
                        ),
                    startX = trailLeft,
                    endX = headX,
                ),
            topLeft = Offset(trailLeft, top),
            size = Size(visibleTrailWidth, bottom - top),
        )
    }
    drawLine(
        color = color.copy(alpha = alpha),
        start = Offset(headX, top),
        end = Offset(headX, bottom),
        strokeWidth = 1.5.dp.toPx(),
    )
}
