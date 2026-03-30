package com.runcheck.ui.home.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
    insights: List<Insight>,
    totalInsightCount: Int,
    unseenInsightCount: Int,
    isPro: Boolean,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
    onDismissInsight: (Long) -> Unit,
) {
    if (insights.isEmpty()) return

    androidx.compose.foundation.layout.Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            SectionHeader(text = stringResource(R.string.home_insights_section_title))
            if (unseenInsightCount > 0) {
                UnseenInsightsBadge(unseenInsightCount = unseenInsightCount)
            }
            if (totalInsightCount > insights.size) {
                TextButton(onClick = onNavigateToInsights) {
                    Text(text = stringResource(R.string.home_insights_view_all))
                }
            }
        }

        insights.forEach { insight ->
            val navigationAction =
                resolveInsightNavigationAction(
                    insight = insight,
                    isPro = isPro,
                    onNavigateToBattery = onNavigateToBattery,
                    onNavigateToNetwork = onNavigateToNetwork,
                    onNavigateToThermal = onNavigateToThermal,
                    onNavigateToStorage = onNavigateToStorage,
                    onNavigateToCharger = onNavigateToCharger,
                    onNavigateToAppUsage = onNavigateToAppUsage,
                    onNavigateToProUpgrade = onNavigateToProUpgrade,
                )

            InsightRow(
                insight = insight,
                onClick = navigationAction.onClick,
                onDismiss = { onDismissInsight(insight.id) },
            )
        }
    }
}

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
