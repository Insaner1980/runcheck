package com.runcheck.ui.chart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.components.ChartQualityZone
import com.runcheck.ui.components.TrendChart

@Composable
fun HistoryChartContent(
    accessibilityTitle: String,
    label: String,
    periodLabel: String,
    chartModel: ChartRenderModel,
    qualityZones: List<ChartQualityZone>?,
    modifier: Modifier = Modifier,
    onExpandClick: (() -> Unit)? = null,
    emptyContent: @Composable () -> Unit = { DefaultHistoryEmptyContent() },
) {
    Column(modifier = modifier) {
        if (chartModel.chartData.size < 2) {
            emptyContent()
        } else {
            val accessibilitySummary =
                rememberChartAccessibilitySummary(
                    title = accessibilityTitle,
                    chartData = chartModel.chartData,
                    unit = chartModel.unit,
                    decimals = chartModel.tooltipDecimals,
                    timeContext = stringResource(R.string.a11y_chart_context_history, periodLabel),
                )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            TrendChart(
                data = chartModel.chartData,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = accessibilitySummary,
                yLabels = chartModel.yLabels.ifEmpty { null },
                xLabels = chartModel.xLabels.ifEmpty { null },
                showGrid = true,
                qualityZones = qualityZones,
                tooltipFormatter = { index -> formatChartTooltip(chartModel, index) },
                onExpandClick = onExpandClick,
            )
            ChartStatsRow(chartModel = chartModel)
        }
    }
}

@Composable
private fun DefaultHistoryEmptyContent() {
    Text(
        text = stringResource(R.string.network_history_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
