package com.runcheck.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.domain.model.Confidence
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.statusColors

@Composable
fun ConfidenceBadge(
    confidence: Confidence,
    modifier: Modifier = Modifier
) {
    val statusColors = MaterialTheme.statusColors
    val reducedMotion = MaterialTheme.reducedMotion

    val backgroundColor = when (confidence) {
        Confidence.HIGH -> statusColors.confidenceAccurateBg
        Confidence.LOW -> statusColors.confidenceEstimatedBg
        Confidence.UNAVAILABLE -> statusColors.confidenceUnavailableBg
    }

    val textColor = when (confidence) {
        Confidence.HIGH -> statusColors.confidenceAccurateText
        Confidence.LOW -> statusColors.confidenceEstimatedText
        Confidence.UNAVAILABLE -> statusColors.confidenceUnavailableText
    }

    val label = when (confidence) {
        Confidence.HIGH -> stringResource(R.string.confidence_accurate)
        Confidence.LOW -> stringResource(R.string.confidence_estimated)
        Confidence.UNAVAILABLE -> stringResource(R.string.confidence_unavailable)
    }

    var scale by remember(reducedMotion) { mutableFloatStateOf(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion) {
            scale = 1f
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = if (reducedMotion) 1f else 0.6f,
            stiffness = if (reducedMotion) Spring.StiffnessHigh else Spring.StiffnessMedium
        ),
        label = "badge_scale"
    )

    Box(
        modifier = modifier
            .scale(animatedScale)
            .background(backgroundColor, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}
