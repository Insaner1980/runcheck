package com.runcheck.ui.chart

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.ThermalReading
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.formatLocalizedDateTime
import com.runcheck.ui.components.ChartXLabel
import com.runcheck.ui.components.ChartYLabel

data class ChartRenderModel(
    val chartData: List<Float>,
    val chartTimestamps: List<Long>,
    val unit: String,
    val yLabels: List<ChartYLabel>,
    val xLabels: List<ChartXLabel>,
    val tooltipDecimals: Int = 0,
    val tooltipTimeSkeleton: String = DEFAULT_TOOLTIP_TIME_SKELETON,
    val temperatureUnit: TemperatureUnit? = null,
) {
    val minValue: Float? get() = chartData.minOrNull()
    val maxValue: Float? get() = chartData.maxOrNull()
    val averageValue: Float?
        get() = if (chartData.isNotEmpty()) chartData.average().toFloat() else null
}

fun formatChartTooltip(
    model: ChartRenderModel,
    index: Int,
): String =
    formatChartTooltip(
        chartData = model.chartData,
        chartTimestamps = model.chartTimestamps,
        index = index,
        unit = model.unit,
        decimals = model.tooltipDecimals,
        timeSkeleton = model.tooltipTimeSkeleton,
    )

fun formatChartTooltip(
    chartData: List<Float>,
    chartTimestamps: List<Long>,
    index: Int,
    unit: String,
    decimals: Int,
    timeSkeleton: String,
): String {
    val value = formatDecimal(chartData[index], decimals)
    val time = formatLocalizedDateTime(chartTimestamps[index], timeSkeleton)
    return "$value$unit · $time"
}

fun buildBatteryHistoryChartModel(
    history: List<BatteryReading>,
    metric: BatteryHistoryMetric,
    period: HistoryPeriod,
    temperatureUnit: TemperatureUnit,
    maxPoints: Int,
): ChartRenderModel {
    val chartPoints =
        history
            .chartPointsFor(metric, temperatureUnit)
            .downsamplePairs(maxPoints)
    val chartData = chartPoints.map { it.second }
    val chartTimestamps = chartPoints.map { it.first }
    val minValue = chartData.minOrNull()
    val maxValue = chartData.maxOrNull()

    return ChartRenderModel(
        chartData = chartData,
        chartTimestamps = chartTimestamps,
        unit = batteryMetricUnit(metric, temperatureUnit),
        yLabels = if (minValue != null && maxValue != null) buildBatteryYLabels(minValue, maxValue) else emptyList(),
        xLabels = if (chartTimestamps.size >= 2) buildBatteryXLabels(chartTimestamps, period) else emptyList(),
        tooltipDecimals =
            when (metric) {
                BatteryHistoryMetric.VOLTAGE -> 2
                BatteryHistoryMetric.TEMPERATURE -> 1
                else -> 0
            },
        temperatureUnit = temperatureUnit,
    )
}

fun buildBatterySessionChartModel(
    summary: ChargingSessionSummary,
    metric: SessionGraphMetric,
    window: SessionGraphWindow,
    maxPoints: Int,
): ChartRenderModel {
    val chartPoints =
        summary.readings
            .graphPointsFor(metric, window)
            .downsamplePairs(maxPoints)
    val chartData = chartPoints.map { it.second }
    val chartTimestamps = chartPoints.map { it.first }
    val minValue = chartData.minOrNull()
    val maxValue = chartData.maxOrNull()

    return ChartRenderModel(
        chartData = chartData,
        chartTimestamps = chartTimestamps,
        unit = sessionMetricUnit(metric),
        yLabels = if (minValue != null && maxValue != null) buildBatteryYLabels(minValue, maxValue) else emptyList(),
        xLabels = if (chartTimestamps.size >= 2) buildSessionXLabels(chartTimestamps) else emptyList(),
        tooltipDecimals = if (metric == SessionGraphMetric.POWER) 1 else 0,
        tooltipTimeSkeleton = "Hm",
    )
}

fun buildNetworkHistoryChartModel(
    history: List<NetworkReading>,
    metric: NetworkHistoryMetric,
    period: HistoryPeriod,
    maxPoints: Int,
): ChartRenderModel {
    val chartPoints =
        history
            .mapNotNull { reading ->
                val value =
                    when (metric) {
                        NetworkHistoryMetric.SIGNAL -> reading.signalDbm?.toFloat()
                        NetworkHistoryMetric.LATENCY -> reading.latencyMs?.toFloat()
                    }
                value?.let { reading.timestamp to it }
            }.downsamplePairs(maxPoints)
    val chartData = chartPoints.map { it.second }
    val chartTimestamps = chartPoints.map { it.first }
    val minValue = chartData.minOrNull()
    val maxValue = chartData.maxOrNull()

    return ChartRenderModel(
        chartData = chartData,
        chartTimestamps = chartTimestamps,
        unit = networkMetricUnit(metric),
        yLabels = if (minValue != null && maxValue != null) buildNetworkYLabels(minValue, maxValue) else emptyList(),
        xLabels = if (chartTimestamps.size >= 2) buildNetworkXLabels(chartTimestamps, period) else emptyList(),
    )
}

fun buildThermalHistoryChartModel(
    history: List<ThermalReading>,
    metric: ThermalHistoryMetric,
    period: HistoryPeriod,
    maxPoints: Int,
    temperatureUnit: TemperatureUnit,
): ChartRenderModel {
    val chartPoints =
        history
            .mapNotNull { reading ->
                val value =
                    when (metric) {
                        ThermalHistoryMetric.BATTERY_TEMP -> reading.batteryTempC
                        ThermalHistoryMetric.CPU_TEMP -> reading.cpuTempC
                    }
                value?.let { reading.timestamp to it }
            }.downsamplePairs(maxPoints)
    val chartData = chartPoints.map { it.second }
    val chartTimestamps = chartPoints.map { it.first }
    val displayData =
        if (temperatureUnit == TemperatureUnit.FAHRENHEIT) {
            chartData.map { it * 9f / 5f + 32f }
        } else {
            chartData
        }
    val unit = if (temperatureUnit == TemperatureUnit.CELSIUS) " °C" else " °F"
    val min = displayData.minOrNull()
    val max = displayData.maxOrNull()

    return ChartRenderModel(
        chartData = displayData,
        chartTimestamps = chartTimestamps,
        unit = unit,
        yLabels = if (min != null && max != null) buildNetworkYLabels(min, max) else emptyList(),
        xLabels = if (chartTimestamps.size >= 2) buildNetworkXLabels(chartTimestamps, period) else emptyList(),
        tooltipDecimals = 1,
        temperatureUnit = temperatureUnit,
    )
}

fun buildStorageHistoryChartModel(
    history: List<StorageReading>,
    metric: StorageHistoryMetric,
    period: HistoryPeriod,
    maxPoints: Int,
): ChartRenderModel {
    val chartPoints =
        history
            .map { reading ->
                val value =
                    when (metric) {
                        StorageHistoryMetric.USED_SPACE -> {
                            (reading.totalBytes - reading.availableBytes).toFloat() / (1024f * 1024f * 1024f)
                        }

                        StorageHistoryMetric.AVAILABLE_SPACE -> {
                            reading.availableBytes.toFloat() / (1024f * 1024f * 1024f)
                        }
                    }
                reading.timestamp to value
            }.downsamplePairs(maxPoints)
    val chartData = chartPoints.map { it.second }
    val chartTimestamps = chartPoints.map { it.first }
    val min = chartData.minOrNull()
    val max = chartData.maxOrNull()

    return ChartRenderModel(
        chartData = chartData,
        chartTimestamps = chartTimestamps,
        unit = " GB",
        yLabels = if (min != null && max != null) buildNetworkYLabels(min, max) else emptyList(),
        xLabels = if (chartTimestamps.size >= 2) buildNetworkXLabels(chartTimestamps, period) else emptyList(),
        tooltipDecimals = 1,
    )
}

private const val DEFAULT_TOOLTIP_TIME_SKELETON = "HmMMMd"
