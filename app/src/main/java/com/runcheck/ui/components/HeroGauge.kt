package com.runcheck.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.runcheck.R
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
    val semantics: HeroGaugeSemantics,
)

internal data class HeroGaugeSemantics(
    val label: String,
    val valuePercent: String,
    val status: String,
    val confidence: String?,
)

internal fun heroGaugePresentation(
    value: Float,
    semanticLabel: String,
    status: String,
    confidenceLabel: String?,
): HeroGaugePresentation {
    val clampedValue = if (value.isFinite()) value.coerceIn(0f, 100f) else 0f
    val roundedValue = clampedValue.roundToInt().toFloat()
    val displayValue = roundedValue.roundToInt().toString()
    return HeroGaugePresentation(
        value = roundedValue,
        displayValue = displayValue,
        semantics =
            HeroGaugeSemantics(
                label = semanticLabel,
                valuePercent = "$displayValue%",
                status = status,
                confidence = confidenceLabel,
            ),
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
    val confidenceLabel = confidence?.let { stringResource(confidenceLabelResource(it)) }
    val presentation =
        remember(value, contentDescription, status, confidenceLabel) {
            heroGaugePresentation(
                value = value,
                semanticLabel = contentDescription,
                status = status,
                confidenceLabel = confidenceLabel,
            )
        }
    val semanticDescription =
        if (presentation.semantics.confidence == null) {
            stringResource(
                R.string.hero_gauge_semantics,
                presentation.semantics.label,
                presentation.displayValue,
                presentation.semantics.status,
            )
        } else {
            stringResource(
                R.string.hero_gauge_semantics_with_confidence,
                presentation.semantics.label,
                presentation.displayValue,
                presentation.semantics.status,
                presentation.semantics.confidence,
            )
        }
    val reducedMotion = MaterialTheme.reducedMotion
    val tokens = MaterialTheme.uiTokens
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
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
                .drawWithCache {
                    val strokeWidth = tokens.heroGaugeStroke.toPx()
                    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    val inset = strokeWidth / 2f
                    val arcSize =
                        size.copy(
                            width = size.width - strokeWidth,
                            height = size.height - strokeWidth,
                        )

                    onDrawBehind {
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
                }.clearAndSetSemantics {
                    this.contentDescription = semanticDescription
                },
        contentAlignment = Alignment.Center,
    ) {
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
