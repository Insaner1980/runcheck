package com.runcheck.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.runcheck.ui.theme.iconCircleColor
import com.runcheck.ui.theme.reducedMotion

@Composable
fun MiniBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color = MaterialTheme.iconCircleColor,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    animationDuration: Int = 800,
    contentDescription: String? = null,
) {
    val isReducedMotion = MaterialTheme.reducedMotion
    val pillShape = MaterialTheme.shapes.extraLarge

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec =
            if (isReducedMotion) {
                tween(durationMillis = 0)
            } else {
                tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
            },
        label = "miniBar",
    )

    val semanticsModifier =
        if (contentDescription != null) {
            Modifier.clearAndSetSemantics {
                this.contentDescription = contentDescription
            }
        } else {
            Modifier.clearAndSetSemantics {}
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .then(semanticsModifier)
                .clip(pillShape)
                .background(trackColor),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .height(height)
                    .background(fillColor, shape = pillShape),
        )
    }
}
