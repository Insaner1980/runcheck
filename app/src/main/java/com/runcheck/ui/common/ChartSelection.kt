package com.runcheck.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.components.ExpressiveSingleChoiceSelector
import com.runcheck.ui.fullscreen.parseFullscreenChartSource

@Composable
fun <T> EnumFilterChipRow(
    values: Iterable<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelFor: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    ExpressiveSingleChoiceSelector(
        options = values.toList(),
        selected = selected,
        labelFor = labelFor,
        onSelect = onSelect,
        modifier = modifier,
    )
}

@Composable
fun ApplyFullscreenChartSelectionResult(
    rawMetric: String?,
    rawPeriod: String?,
    onConsume: () -> Unit,
    applySelection: (source: FullscreenChartSource, metric: String, period: String) -> Unit,
    rawSource: String? = null,
    defaultSource: FullscreenChartSource? = null,
) {
    val currentApplySelection = rememberUpdatedState(applySelection)
    val currentOnConsume = rememberUpdatedState(onConsume)

    LaunchedEffect(rawSource, defaultSource, rawMetric, rawPeriod) {
        val source = rawSource?.let(::parseFullscreenChartSource) ?: defaultSource
        if (source != null && rawMetric != null && rawPeriod != null) {
            currentApplySelection.value(source, rawMetric, rawPeriod)
            currentOnConsume.value()
        }
    }
}
