package com.runcheck.ui.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.common.formatDecimal
import kotlin.math.abs
import kotlin.math.max

internal enum class ChartTrendDirection {
    INCREASING,
    DECREASING,
    STABLE,
}

internal data class ChartAccessibilitySnapshot(
    val minimumValue: String,
    val maximumValue: String,
    val latestValue: String,
    val trendDirection: ChartTrendDirection,
)

internal fun buildChartAccessibilitySnapshot(
    chartData: List<Float>,
    unit: String,
    decimals: Int,
): ChartAccessibilitySnapshot? {
    if (chartData.isEmpty()) return null

    val minValue = chartData.minOrNull() ?: return null
    val maxValue = chartData.maxOrNull() ?: return null
    val latestValue = chartData.last()

    return ChartAccessibilitySnapshot(
        minimumValue = "${formatDecimal(minValue, decimals)}$unit",
        maximumValue = "${formatDecimal(maxValue, decimals)}$unit",
        latestValue = "${formatDecimal(latestValue, decimals)}$unit",
        trendDirection =
            resolveChartTrendDirection(
                chartData = chartData,
                minValue = minValue,
                maxValue = maxValue,
            ),
    )
}

@Composable
fun rememberChartAccessibilitySummary(
    title: String,
    chartData: List<Float>,
    unit: String,
    decimals: Int,
    timeContext: String? = null,
): String {
    val snapshot =
        remember(chartData, unit, decimals) {
            buildChartAccessibilitySnapshot(
                chartData = chartData,
                unit = unit,
                decimals = decimals,
            )
        } ?: return title

    val trendLabel =
        stringResource(
            when (snapshot.trendDirection) {
                ChartTrendDirection.INCREASING -> R.string.a11y_chart_trend_increasing
                ChartTrendDirection.DECREASING -> R.string.a11y_chart_trend_decreasing
                ChartTrendDirection.STABLE -> R.string.a11y_chart_trend_stable
            },
        )

    return if (timeContext.isNullOrBlank()) {
        stringResource(
            R.string.a11y_chart_summary,
            title,
            snapshot.minimumValue,
            snapshot.maximumValue,
            snapshot.latestValue,
            trendLabel,
        )
    } else {
        stringResource(
            R.string.a11y_chart_summary_with_context,
            title,
            timeContext,
            snapshot.minimumValue,
            snapshot.maximumValue,
            snapshot.latestValue,
            trendLabel,
        )
    }
}

private fun resolveChartTrendDirection(
    chartData: List<Float>,
    minValue: Float,
    maxValue: Float,
): ChartTrendDirection {
    if (chartData.size < 2) return ChartTrendDirection.STABLE

    val delta = chartData.last() - chartData.first()
    val range = maxValue - minValue
    val minimumThreshold =
        when {
            range >= 10f -> 1f
            range >= 1f -> 0.25f
            else -> 0.05f
        }
    val threshold = max(range * 0.08f, minimumThreshold)

    return when {
        abs(delta) <= threshold -> ChartTrendDirection.STABLE
        delta > 0f -> ChartTrendDirection.INCREASING
        else -> ChartTrendDirection.DECREASING
    }
}
