package com.runcheck.ui.chart

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.ThermalReading
import com.runcheck.ui.common.convertTemperature
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
    val lineBreakIndices: Set<Int> = emptySet(),
) {
    val minValue: Float? get() = chartData.minOrNull()
    val maxValue: Float? get() = chartData.maxOrNull()
    val averageValue: Float?
        get() = if (chartData.isNotEmpty()) chartData.average().toFloat() else null
}

fun batteryMetricUnit(
    metric: BatteryHistoryMetric,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
): String =
    when (metric) {
        BatteryHistoryMetric.LEVEL -> "%"
        BatteryHistoryMetric.TEMPERATURE -> if (temperatureUnit == TemperatureUnit.CELSIUS) "°C" else "°F"
        BatteryHistoryMetric.CURRENT -> " mA"
        BatteryHistoryMetric.VOLTAGE -> " V"
    }

fun networkMetricUnit(metric: NetworkHistoryMetric): String =
    when (metric) {
        NetworkHistoryMetric.SIGNAL -> " dBm"
        NetworkHistoryMetric.LATENCY -> " ms"
    }

fun sessionMetricUnit(metric: SessionGraphMetric): String =
    when (metric) {
        SessionGraphMetric.CURRENT -> " mA"
        SessionGraphMetric.POWER -> " W"
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
    val series =
        history
            .chartPointsFor(metric, temperatureUnit)
            .toChartSeries(maxPoints)

    return ChartRenderModel(
        chartData = series.data,
        chartTimestamps = series.timestamps,
        unit = batteryMetricUnit(metric, temperatureUnit),
        yLabels = series.labelsWith(::buildBatteryYLabels),
        xLabels = series.xLabelsWith { buildBatteryXLabels(it, period) },
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
    val series =
        summary.readings
            .graphPointsFor(metric, window)
            .toChartSeries(maxPoints)

    return ChartRenderModel(
        chartData = series.data,
        chartTimestamps = series.timestamps,
        unit = sessionMetricUnit(metric),
        yLabels = series.labelsWith(::buildBatteryYLabels),
        xLabels = series.xLabelsWith(::buildSessionXLabels),
        tooltipDecimals = if (metric == SessionGraphMetric.POWER) 1 else 0,
        tooltipTimeSkeleton = "Hm",
        lineBreakIndices = findSessionLineBreakIndices(series.timestamps),
    )
}

internal fun findSessionLineBreakIndices(chartTimestamps: List<Long>): Set<Int> =
    chartTimestamps
        .zipWithNext()
        .mapIndexedNotNull { index, (start, end) ->
            (index + 1).takeIf { end - start > MAX_SESSION_SAMPLE_GAP_MS }
        }.toSet()

fun buildNetworkHistoryChartModel(
    history: List<NetworkReading>,
    metric: NetworkHistoryMetric,
    period: HistoryPeriod,
    maxPoints: Int,
): ChartRenderModel {
    val series =
        history
            .mapNotNull { reading ->
                val value =
                    when (metric) {
                        NetworkHistoryMetric.SIGNAL -> reading.signalDbm?.toFloat()
                        NetworkHistoryMetric.LATENCY -> reading.latencyMs?.toFloat()
                    }
                value?.let { reading.timestamp to it }
            }.toChartSeries(maxPoints)

    return ChartRenderModel(
        chartData = series.data,
        chartTimestamps = series.timestamps,
        unit = networkMetricUnit(metric),
        yLabels = series.labelsWith(::buildNetworkYLabels),
        xLabels = series.xLabelsWith { buildNetworkXLabels(it, period) },
    )
}

fun buildThermalHistoryChartModel(
    history: List<ThermalReading>,
    metric: ThermalHistoryMetric,
    period: HistoryPeriod,
    maxPoints: Int,
    temperatureUnit: TemperatureUnit,
): ChartRenderModel {
    val series =
        history
            .mapNotNull { reading ->
                val value =
                    when (metric) {
                        ThermalHistoryMetric.BATTERY_TEMP -> reading.batteryTempC
                        ThermalHistoryMetric.CPU_TEMP -> reading.cpuTempC
                    }
                value?.let { reading.timestamp to it }
            }.toChartSeries(maxPoints)
            .mapData { convertTemperature(it, temperatureUnit).toFloat() }
    val unit = if (temperatureUnit == TemperatureUnit.CELSIUS) " °C" else " °F"

    return ChartRenderModel(
        chartData = series.data,
        chartTimestamps = series.timestamps,
        unit = unit,
        yLabels = series.labelsWith(::buildNetworkYLabels),
        xLabels = series.xLabelsWith { buildNetworkXLabels(it, period) },
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
    val series =
        history
            .mapNotNull { reading ->
                val value =
                    when (metric) {
                        StorageHistoryMetric.USED_SPACE -> {
                            if (reading.totalBytes <= 0L) return@mapNotNull null
                            val availableBytes = reading.availableBytes.coerceIn(0L, reading.totalBytes)
                            ((reading.totalBytes - availableBytes).toDouble() / reading.totalBytes.toDouble() * 100.0)
                                .toFloat()
                        }

                        StorageHistoryMetric.AVAILABLE_SPACE -> {
                            if (reading.availableBytes < 0L) return@mapNotNull null
                            reading.availableBytes
                                .toDouble()
                                .div(BYTES_PER_GB)
                                .toFloat()
                        }
                    }
                reading.timestamp to value
            }.toChartSeries(maxPoints)

    return ChartRenderModel(
        chartData = series.data,
        chartTimestamps = series.timestamps,
        unit = if (metric == StorageHistoryMetric.USED_SPACE) "%" else " GB",
        yLabels = series.labelsWith(::buildNetworkYLabels),
        xLabels = series.xLabelsWith { buildNetworkXLabels(it, period) },
        tooltipDecimals = 1,
    )
}

private data class ChartSeries(
    val data: List<Float>,
    val timestamps: List<Long>,
) {
    fun labelsWith(builder: (min: Float, max: Float) -> List<ChartYLabel>): List<ChartYLabel> {
        val min = data.minOrNull() ?: return emptyList()
        val max = data.maxOrNull() ?: return emptyList()
        return builder(min, max)
    }

    fun xLabelsWith(builder: (timestamps: List<Long>) -> List<ChartXLabel>): List<ChartXLabel> =
        if (timestamps.size >= 2) builder(timestamps) else emptyList()

    fun mapData(transform: (Float) -> Float): ChartSeries = copy(data = data.map(transform))
}

private fun List<Pair<Long, Float>>.toChartSeries(maxPoints: Int): ChartSeries {
    val points = downsamplePairs(maxPoints)
    return ChartSeries(
        data = points.map { it.second },
        timestamps = points.map { it.first },
    )
}

private const val BYTES_PER_GB = 1_000_000_000.0
private const val DEFAULT_TOOLTIP_TIME_SKELETON = "HmMMMd"
