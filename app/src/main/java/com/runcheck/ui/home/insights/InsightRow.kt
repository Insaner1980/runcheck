package com.runcheck.ui.home.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.ui.components.IconCircle
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.statusColors

@Composable
fun InsightRow(
    insight: Insight,
    onClick: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
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
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.a11y_dismiss_card),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.a11y_open_insight_destination),
                        modifier = Modifier.size(20.dp),
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
