package com.runcheck.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

internal enum class HomeTileEdge { BATTERY, THERMAL, LEARN, SPEED }

internal val HOME_TILE_EDGE_BEND = 20.dp

/** Complementary curved edges keep a real gap between independently clickable tiles. */
@Immutable
internal data class HomeTileShape(
    val edge: HomeTileEdge,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        val r = with(density) { HOME_TILE_EDGE_BEND.toPx() }.coerceAtMost(minOf(w, h) / 3f)
        val maxBend = (w / 3f).coerceAtLeast(0f)
        val bend = with(density) { HOME_TILE_EDGE_BEND.toPx() }.coerceIn(0f, maxBend)
        val leftTop = if (edge == HomeTileEdge.THERMAL) bend else 0f
        val leftBottom = if (edge == HomeTileEdge.SPEED) bend else 0f
        val rightTop = if (edge == HomeTileEdge.LEARN) w - bend else w
        val rightBottom = if (edge == HomeTileEdge.BATTERY) w - bend else w
        return Outline.Generic(
            Path().apply {
                moveTo(leftTop + r, 0f)
                lineTo(rightTop - r, 0f)
                quadraticTo(rightTop, 0f, rightTop, r)
                cubicTo(rightTop, h * 0.52f, rightBottom, h * 0.48f, rightBottom, h - r)
                quadraticTo(rightBottom, h, rightBottom - r, h)
                lineTo(leftBottom + r, h)
                quadraticTo(leftBottom, h, leftBottom, h - r)
                if (edge == HomeTileEdge.SPEED) {
                    cubicTo(leftBottom, h * 0.78f, leftTop, h * 0.78f, leftTop, h * 0.55f)
                    lineTo(leftTop, r)
                } else {
                    cubicTo(leftBottom, h * 0.48f, leftTop, h * 0.52f, leftTop, r)
                }
                quadraticTo(leftTop, 0f, leftTop + r, 0f)
                close()
            },
        )
    }
}
