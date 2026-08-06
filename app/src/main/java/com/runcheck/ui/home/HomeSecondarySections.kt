package com.runcheck.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.ui.components.IconCircle
import com.runcheck.ui.components.ListRow
import com.runcheck.ui.components.ProBadgePill
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors

@Composable
internal fun HomeQuickToolsSection(
    isPro: Boolean,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
    onNavigateToLearn: () -> Unit,
) {
    Column {
        SectionHeader(stringResource(R.string.home_quick_tools))

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        Card(
            shape = MaterialTheme.shapes.large,
            colors = runcheckCardColors(),
            elevation = runcheckCardElevation(),
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = MaterialTheme.spacing.base,
                        vertical = MaterialTheme.spacing.xs,
                    ),
            ) {
                ListRow(
                    label = stringResource(R.string.home_speed_test),
                    icon = Icons.Outlined.Speed,
                    onClick = onNavigateToSpeedTest,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                HomeAppUsageQuickToolRow(
                    isPro = isPro,
                    onNavigateToAppUsage = onNavigateToAppUsage,
                    onNavigateToProUpgrade = onNavigateToProUpgrade,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ListRow(
                    label = stringResource(R.string.home_learn),
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    onClick = onNavigateToLearn,
                )
            }
        }
    }
}

@Composable
private fun HomeAppUsageQuickToolRow(
    isPro: Boolean,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        ListRow(
            label = stringResource(R.string.home_app_usage_card),
            icon = Icons.Outlined.DataUsage,
            onClick = if (isPro) onNavigateToAppUsage else onNavigateToProUpgrade,
            trailing = if (!isPro) ({ ProBadgePill() }) else null,
        )

        if (!isPro) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.14f)),
            )
        }
    }
}

@Composable
internal fun HomeProStatusSection(visible: Boolean) {
    if (visible) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = runcheckCardColors(),
            elevation = runcheckCardElevation(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconCircle(
                    icon = Icons.Outlined.Star,
                    tint = MaterialTheme.statusColors.healthy,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_insights_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.home_insights_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
