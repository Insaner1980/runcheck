package com.runcheck.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


data class StatusSegment(
    val label: String,
    val color: Color,
    val rangeStart: Float,
    val rangeEnd: Float
)

/**
 * Horizontal segmented bar showing a value's position on a labeled scale.
 * Active segment is highlighted; others are dimmed.
 */
@Composable
fun SegmentedStatusBar(
    segments: List<StatusSegment>,
    currentValue: Float,
    modifier: Modifier = Modifier,
    barHeight: Dp = 6.dp,
    gap: Dp = 3.dp,
    inactiveAlpha: Float = 0.2f,
    accessibilityDescription: String? = null
) {
    val activeIndex = segments.indexOfLast { currentValue >= it.rangeStart }
        .coerceAtLeast(0)

    Column(modifier = modifier.then(
        if (accessibilityDescription != null) {
            Modifier.semantics { contentDescription = accessibilityDescription }
        } else Modifier
    )) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
        ) {
            val totalGap = gap.toPx() * (segments.size - 1)
            val segmentWidth = (size.width - totalGap) / segments.size
            val cornerRad = CornerRadius(barHeight.toPx() / 2f)

            segments.forEachIndexed { index, segment ->
                val x = index * (segmentWidth + gap.toPx())
                val isActive = index == activeIndex
                val alpha = if (isActive) 1f else inactiveAlpha

                drawRoundRect(
                    color = segment.color.copy(alpha = alpha),
                    topLeft = Offset(x, 0f),
                    size = Size(segmentWidth, size.height),
                    cornerRadius = cornerRad
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            segments.forEachIndexed { index, segment ->
                val isActive = index == activeIndex
                Text(
                    text = segment.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) {
                        segment.color
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }
        }
    }
}
