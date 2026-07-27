package com.runcheck.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import com.runcheck.domain.model.Confidence
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.theme.gaugeValueTextStyle
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.uiTokens
import kotlin.math.roundToInt

internal data class HeroGaugePresentation(
    val value: Float,
    val displayValue: String,
    val stateDescription: String,
)

internal fun heroGaugePresentation(
    value: Float,
    label: String,
    status: String,
): HeroGaugePresentation {
    val clampedValue = if (value.isFinite()) value.coerceIn(0f, 100f) else 0f
    val roundedValue = clampedValue.roundToInt().toFloat()
    val displayValue = roundedValue.roundToInt().toString()
    return HeroGaugePresentation(
        value = roundedValue,
        displayValue = displayValue,
        stateDescription = "$label, $status, $displayValue%",
    )
}

@Composable
fun HeroGauge(
    value: Float,
    label: String,
    status: String,
    accent: Color,
    contentDescription: String,
    animationKey: String,
    modifier: Modifier = Modifier,
    confidence: Confidence? = null,
) {
    val presentation = remember(value, label, status) { heroGaugePresentation(value, label, status) }
    val reducedMotion = MaterialTheme.reducedMotion
    val tokens = MaterialTheme.uiTokens
    val animatedValue =
        remember(animationKey) {
            Animatable(if (reducedMotion) presentation.value else 0f)
        }

    LaunchedEffect(animationKey, presentation.value, reducedMotion) {
        if (reducedMotion) {
            animatedValue.snapTo(presentation.value)
        } else {
            animatedValue.animateTo(
                targetValue = presentation.value,
                animationSpec = MotionTokens.gaugeSpring(),
            )
        }
    }

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clearAndSetSemantics {
                    this.contentDescription = contentDescription
                    stateDescription = presentation.stateDescription
                    progressBarRangeInfo = ProgressBarRangeInfo(presentation.value, 0f..100f)
                },
        contentAlignment = Alignment.Center,
    ) {
        val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .clearAndSetSemantics {},
        ) {
            val strokeWidth = tokens.heroGaugeStroke.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val inset = strokeWidth / 2f
            val arcSize = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth)

            drawArc(
                color = trackColor,
                startAngle = tokens.heroGaugeStartAngle,
                sweepAngle = tokens.heroGaugeSweepAngle,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = accent,
                startAngle = tokens.heroGaugeStartAngle,
                sweepAngle = tokens.heroGaugeSweepAngle * (animatedValue.value / 100f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
        }

        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.cardInternal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            Text(
                text = presentation.displayValue,
                style = MaterialTheme.gaugeValueTextStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatusPill(label = status, tone = StatusTone.NEUTRAL)
            if (confidence != null) {
                ConfidenceBadge(confidence = confidence)
            }
        }
    }
}
