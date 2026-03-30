package com.runcheck.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.fullscreen.parseFullscreenChartSource
import com.runcheck.ui.theme.spacing

@Composable
fun <T> EnumFilterChipRow(
    values: Iterable<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelFor: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        values.forEach { value ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(labelFor(value)) },
            )
        }
    }
}

@Composable
fun ApplyFullscreenChartSelectionResult(
    rawMetric: String?,
    rawPeriod: String?,
    onConsumed: () -> Unit,
    applySelection: (source: FullscreenChartSource, metric: String, period: String) -> Unit,
    rawSource: String? = null,
    defaultSource: FullscreenChartSource? = null,
) {
    val currentApplySelection = rememberUpdatedState(applySelection)
    val currentOnConsumed = rememberUpdatedState(onConsumed)

    LaunchedEffect(rawSource, defaultSource, rawMetric, rawPeriod) {
        val source = rawSource?.let(::parseFullscreenChartSource) ?: defaultSource
        if (source != null && rawMetric != null && rawPeriod != null) {
            currentApplySelection.value(source, rawMetric, rawPeriod)
            currentOnConsumed.value()
        }
    }
}
