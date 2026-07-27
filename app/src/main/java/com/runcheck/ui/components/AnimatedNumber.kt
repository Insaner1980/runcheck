package com.runcheck.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.theme.reducedMotion
import kotlin.math.roundToInt

internal fun <T> formatCounterText(
    value: T,
    formatter: (T) -> String,
    prefix: String,
    suffix: String,
): String = "$prefix${formatter(value)}$suffix"

@Composable
fun AnimatedCounter(
    value: Int,
    modifier: Modifier = Modifier,
    initialValue: Int = 0,
    formatter: (Int) -> String = Int::toString,
    prefix: String = "",
    suffix: String = "",
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    AnimatedCounterText(
        value = value.toFloat(),
        initialValue = initialValue.toFloat(),
        finalText =
            formatCounterText(
                value = value,
                formatter = formatter,
                prefix = prefix,
                suffix = suffix,
            ),
        visualFormatter = { animatedValue ->
            formatCounterText(
                value = animatedValue.roundToInt(),
                formatter = formatter,
                prefix = prefix,
                suffix = suffix,
            )
        },
        modifier = modifier,
        style = style,
        color = color,
    )
}

@Composable
fun AnimatedCounter(
    value: Float,
    modifier: Modifier = Modifier,
    initialValue: Float = 0f,
    formatter: (Float) -> String = { animatedValue -> formatDecimal(animatedValue, 1) },
    prefix: String = "",
    suffix: String = "",
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    AnimatedCounterText(
        value = value,
        initialValue = initialValue,
        finalText =
            formatCounterText(
                value = value,
                formatter = formatter,
                prefix = prefix,
                suffix = suffix,
            ),
        visualFormatter = { animatedValue ->
            formatCounterText(
                value = animatedValue,
                formatter = formatter,
                prefix = prefix,
                suffix = suffix,
            )
        },
        modifier = modifier,
        style = style,
        color = color,
    )
}

@Composable
private fun AnimatedCounterText(
    value: Float,
    initialValue: Float,
    finalText: String,
    visualFormatter: (Float) -> String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = MaterialTheme.reducedMotion
    val animatedValue = remember(initialValue) { Animatable(initialValue) }

    LaunchedEffect(value, reducedMotion) {
        if (reducedMotion) {
            animatedValue.snapTo(value)
        } else {
            animatedValue.animateTo(
                targetValue = value,
                animationSpec = MotionTokens.counterTween(),
            )
        }
    }

    Text(
        text = visualFormatter(animatedValue.value),
        style = style,
        color = color,
        modifier =
            modifier.clearAndSetSemantics {
                text = AnnotatedString(finalText)
            },
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
    AnimatedCounter(
        value = value,
        modifier = modifier,
        formatter = { animatedValue -> formatDecimal(animatedValue, decimalPlaces) },
        suffix = suffix,
        style = style,
    )
}
