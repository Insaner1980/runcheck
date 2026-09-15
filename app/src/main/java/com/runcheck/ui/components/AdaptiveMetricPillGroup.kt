package com.runcheck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.runcheck.ui.theme.LARGE_CONTENT_FONT_SCALE

@Composable
internal fun AdaptiveMetricPillGroup(
    items: List<MetricPillItem>,
    onInfoClick: (String) -> Unit,
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    modifier: Modifier = Modifier,
    stackedItemModifier: Modifier = Modifier,
) {
    if (shouldStackMetricPills(LocalDensity.current.fontScale)) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        ) {
            MetricPillItems(
                items = items,
                onInfoClick = onInfoClick,
                modifier = stackedItemModifier,
            )
        }
    } else {
        MetricPillRow(modifier = modifier, spacing = horizontalSpacing) {
            MetricPillItems(
                items = items,
                onInfoClick = onInfoClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

internal fun shouldStackMetricPills(fontScale: Float): Boolean = fontScale >= LARGE_CONTENT_FONT_SCALE
