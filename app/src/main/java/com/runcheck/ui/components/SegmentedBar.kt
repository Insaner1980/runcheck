package com.runcheck.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.runcheck.ui.theme.reducedMotion

data class SegmentData(
    val label: String,
    val value: Long,
    val formattedValue: String,
    val color: Color
)

@Composable
fun SegmentedBar(
    segments: List<SegmentData>,
    modifier: Modifier = Modifier
) {
    val total = segments.sumOf { it.value }.coerceAtLeast(1L)
    val reducedMotion = MaterialTheme.reducedMotion
    val gapPx = with(LocalDensity.current) { 2.dp.toPx() }
    val cornerPx = with(LocalDensity.current) { 6.dp.toPx() }
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    // Animate the overall progress from 0 to 1
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reducedMotion) tween(0) else tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "segBarProgress"
    )

    val a11yDesc = segments.joinToString(", ") { "${it.label}: ${it.formattedValue}" }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .semantics {
                contentDescription = a11yDesc
                this.role = Role.Image
            }
    ) {
        val totalWidth = size.width
        val barHeight = size.height

        // Draw background track
        drawRoundRect(
            color = bgColor,
            size = Size(totalWidth, barHeight),
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )

        // Draw segments
        val nonZeroSegments = segments.filter { it.value > 0 }
        if (nonZeroSegments.isEmpty()) return@Canvas

        val minSegmentWidth = 4.dp.toPx()
        val totalGapWidth = (nonZeroSegments.size - 1).coerceAtLeast(0) * gapPx
        val availableWidth = (totalWidth - totalGapWidth) * animatedProgress

        // Calculate proportional widths
        val rawWidths = nonZeroSegments.map { (it.value.toFloat() / total) * availableWidth }
        val adjustedWidths = rawWidths.map { it.coerceAtLeast(minSegmentWidth) }
        val widthScale = availableWidth / adjustedWidths.sum().coerceAtLeast(1f)
        val finalWidths = adjustedWidths.map { it * widthScale }

        var x = 0f
        finalWidths.forEachIndexed { index, segWidth ->
            val segment = nonZeroSegments[index]
            drawSegment(
                color = segment.color,
                x = x,
                width = segWidth,
                height = barHeight,
                cornerPx = cornerPx,
                isFirst = index == 0,
                isLast = index == finalWidths.lastIndex
            )
            x += segWidth + gapPx
        }
    }
}

private fun DrawScope.drawSegment(
    color: Color,
    x: Float,
    width: Float,
    height: Float,
    cornerPx: Float,
    isFirst: Boolean,
    isLast: Boolean
) {
    // For first and last segments, use rounded corners; middle segments are flat
    if (isFirst && isLast) {
        drawRoundRect(
            color = color,
            topLeft = Offset(x, 0f),
            size = Size(width, height),
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )
    } else if (isFirst) {
        // Round left corners only - draw round rect clipped
        drawRoundRect(
            color = color,
            topLeft = Offset(x, 0f),
            size = Size(width + cornerPx, height),
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )
        // Cover right corners with a flat rect
        drawRect(
            color = color,
            topLeft = Offset(x + width - cornerPx, 0f),
            size = Size(cornerPx, height)
        )
    } else if (isLast) {
        drawRoundRect(
            color = color,
            topLeft = Offset(x - cornerPx, 0f),
            size = Size(width + cornerPx, height),
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )
        drawRect(
            color = color,
            topLeft = Offset(x, 0f),
            size = Size(cornerPx, height)
        )
    } else {
        drawRect(
            color = color,
            topLeft = Offset(x, 0f),
            size = Size(width, height)
        )
    }
}

@Composable
fun SegmentedBarLegend(
    segments: List<SegmentData>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        segments.filter { it.value > 0 }.forEach { segment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(color = segment.color)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = segment.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = segment.formattedValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
