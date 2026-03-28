package com.runcheck.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.theme.reducedMotion
import kotlin.math.roundToInt

@Composable
fun AnimatedIntText(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    suffix: String = "",
) {
    val reducedMotion = MaterialTheme.reducedMotion

    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else MotionTokens.SHORT),
        label = "number_anim",
    )

    Text(
        text = "${animatedValue.roundToInt()}$suffix",
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
fun AnimatedFloatText(
    value: Float,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    decimalPlaces: Int = 1,
    suffix: String = "",
) {
    val reducedMotion = MaterialTheme.reducedMotion

    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else MotionTokens.SHORT),
        label = "float_anim",
    )

    Text(
        text = "${formatDecimal(animatedValue, decimalPlaces)}$suffix",
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}
