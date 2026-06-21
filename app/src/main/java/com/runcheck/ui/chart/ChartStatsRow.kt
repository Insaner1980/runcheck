package com.runcheck.ui.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.theme.spacing

@Composable
fun ChartStatsRow(
    chartModel: ChartRenderModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
        verticalAlignment = Alignment.Top,
    ) {
        val decimals = chartModel.tooltipDecimals
        chartModel.minValue?.let {
            MetricPill(
                label = stringResource(R.string.chart_stat_min),
                value = "${formatDecimal(it, decimals)}${chartModel.unit}",
                modifier = Modifier.weight(1f),
            )
        }
        chartModel.averageValue?.let {
            MetricPill(
                label = stringResource(R.string.chart_stat_avg),
                value = "${formatDecimal(it, decimals)}${chartModel.unit}",
                modifier = Modifier.weight(1f),
            )
        }
        chartModel.maxValue?.let {
            MetricPill(
                label = stringResource(R.string.chart_stat_max),
                value = "${formatDecimal(it, decimals)}${chartModel.unit}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}
