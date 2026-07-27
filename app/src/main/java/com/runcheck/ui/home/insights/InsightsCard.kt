package com.runcheck.ui.home.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import com.runcheck.R
import com.runcheck.domain.insights.model.Insight
import com.runcheck.ui.components.RuncheckEmptyState
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.theme.BadgeShape
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.uiTokens

@Composable
fun InsightsCard(
    state: InsightsCardState,
    navigationHandlers: InsightNavigationHandlers,
    onNavigateToInsights: () -> Unit,
    onDismissInsight: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val insights = state.insights

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
            TextButton(onClick = onNavigateToInsights) {
                Text(text = stringResource(R.string.home_insights_view_all))
            }
        }

        if (homeInsightsSectionShowsEmptyState(insights)) {
            RuncheckEmptyState(
                title = stringResource(R.string.home_insights_empty_title),
                message = stringResource(R.string.home_insights_empty_body),
            )
        } else {
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
}

internal fun homeInsightsSectionShowsEmptyState(insights: List<Insight>): Boolean = insights.isEmpty()

data class InsightsCardState(
    val insights: List<Insight>,
    val unseenInsightCount: Int,
    val isPro: Boolean,
)

@Composable
private fun UnseenInsightsBadge(unseenInsightCount: Int) {
    val tokens = MaterialTheme.uiTokens
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
                    shape = BadgeShape,
                ).padding(
                    horizontal = tokens.proBadgeHorizontalPadding,
                    vertical = tokens.badgeVerticalPadding,
                ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}
