package com.runcheck.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.theme.iconCircleColor
import com.runcheck.ui.theme.reducedMotion

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = MaterialTheme.iconCircleColor,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    animationDuration: Int = MotionTokens.RING,
    contentDescription: String? = null,
    content: @Composable () -> Unit = {},
) {
    val isReducedMotion = MaterialTheme.reducedMotion

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec =
            if (isReducedMotion) {
                tween(durationMillis = 0)
            } else {
                tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
            },
        label = "progressRing",
    )

    Box(
        modifier =
            if (contentDescription == null) {
                modifier
            } else {
                modifier.semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                    progressBarRangeInfo = ProgressBarRangeInfo(animatedProgress, 0f..1f)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke =
                Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round,
                )
            val padding = strokeWidth.toPx() / 2f

            // Track (full circle)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                topLeft =
                    androidx.compose.ui.geometry
                        .Offset(padding, padding),
                size =
                    androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth.toPx(),
                        size.height - strokeWidth.toPx(),
                    ),
            )

            // Progress arc (from top, clockwise)
            drawArc(
                color = progressColor,
                startAngle = 270f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = stroke,
                topLeft =
                    androidx.compose.ui.geometry
                        .Offset(padding, padding),
                size =
                    androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth.toPx(),
                        size.height - strokeWidth.toPx(),
                    ),
            )
        }
        content()
    }
}
