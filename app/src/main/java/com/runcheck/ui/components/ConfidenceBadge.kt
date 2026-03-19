package com.runcheck.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.snap
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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

    var targetScale by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { targetScale = 1f }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "badge_scale"
    )

    Box(
        modifier = modifier
            .scale(animatedScale)
            .semantics {
                contentDescription = label
                role = Role.Image
            }
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
