package com.runcheck.ui.home.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.domain.insights.model.Insight
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.theme.spacing

@Composable
fun InsightsCard(
    state: InsightsCardState,
    navigationHandlers: InsightNavigationHandlers,
    onNavigateToInsights: () -> Unit,
    onDismissInsight: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val insights = state.insights
    if (insights.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            SectionHeader(text = stringResource(R.string.home_insights_section_title))
            if (state.unseenInsightCount > 0) {
                UnseenInsightsBadge(unseenInsightCount = state.unseenInsightCount)
            }
            if (state.totalInsightCount > insights.size) {
                TextButton(onClick = onNavigateToInsights) {
                    Text(text = stringResource(R.string.home_insights_view_all))
                }
            }
        }

        insights.forEach { insight ->
            val navigationAction =
                resolveInsightNavigationAction(
                    insight = insight,
                    isPro = state.isPro,
                    navigationHandlers = navigationHandlers,
                )

            InsightRow(
                insight = insight,
                onClick = navigationAction.onClick,
                onDismiss = { onDismissInsight(insight.id) },
            )
        }
    }
}

data class InsightsCardState(
    val insights: List<Insight>,
    val totalInsightCount: Int,
    val unseenInsightCount: Int,
    val isPro: Boolean,
)

@Composable
private fun UnseenInsightsBadge(unseenInsightCount: Int) {
    val label =
        pluralStringResource(
            id = R.plurals.home_insights_unseen_count,
            count = unseenInsightCount,
            unseenInsightCount,
        )

    Text(
        text = label,
        modifier =
            Modifier
                .clearAndSetSemantics {
                    contentDescription = label
                }.background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(999.dp),
                ).padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}
