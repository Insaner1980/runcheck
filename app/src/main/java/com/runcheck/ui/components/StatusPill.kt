package com.runcheck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import com.runcheck.ui.theme.uiTokens

enum class StatusTone {
    HEALTHY,
    FAIR,
    POOR,
    CRITICAL,
    NEUTRAL,
    UNAVAILABLE,
}

@Composable
fun StatusPill(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.statusColors
    val (container, foreground) =
        when (tone) {
            StatusTone.HEALTHY -> colors.healthyContainer to colors.onHealthyContainer
            StatusTone.FAIR -> colors.fairContainer to colors.onFairContainer
            StatusTone.POOR -> colors.poorContainer to colors.onPoorContainer
            StatusTone.CRITICAL -> colors.criticalContainer to colors.onCriticalContainer
            StatusTone.NEUTRAL -> colors.neutralContainer to colors.onNeutralContainer
            StatusTone.UNAVAILABLE -> colors.unavailableContainer to colors.onUnavailableContainer
        }
    val tokens = MaterialTheme.uiTokens

    Surface(
        modifier =
            modifier
                .defaultMinSize(
                    minWidth = tokens.statusPillMinWidth,
                    minHeight = tokens.statusPillMinHeight,
                ).semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = foreground,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = tokens.badgeHorizontalPadding,
                    vertical = tokens.badgeVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(tokens.iconSmall),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
