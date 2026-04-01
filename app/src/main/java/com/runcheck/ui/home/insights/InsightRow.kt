package com.runcheck.ui.home.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.ui.components.IconCircle
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import com.runcheck.ui.theme.uiTokens

@Composable
fun InsightRow(
    insight: Insight,
    onClick: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val tokens = MaterialTheme.uiTokens
    val priorityTint =
        when (insight.priority) {
            InsightPriority.HIGH -> MaterialTheme.statusColors.critical
            InsightPriority.MEDIUM -> MaterialTheme.statusColors.poor
            InsightPriority.LOW -> MaterialTheme.statusColors.fair
        }

    val content: @Composable () -> Unit = {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            IconCircle(
                icon = Icons.Outlined.WarningAmber,
                tint = priorityTint,
            )
            Column(modifier = with(this) { Modifier.weight(1f) }) {
                Text(
                    text = resolveInsightTitle(insight),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = resolveInsightBody(insight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(tokens.touchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.a11y_dismiss_card),
                        modifier = Modifier.size(tokens.iconMedium),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.a11y_open_insight_destination),
                        modifier = Modifier.size(tokens.iconLarge),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = MaterialTheme.shapes.large,
            colors = runcheckCardColors(),
            elevation = runcheckCardElevation(),
        ) {
            content()
        }
    } else {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = runcheckCardColors(),
            elevation = runcheckCardElevation(),
        ) {
            content()
        }
    }
}
