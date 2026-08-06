package com.runcheck.ui.chart

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.ui.common.convertTemperature
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.formatLocalizedDateTime
import com.runcheck.ui.components.ChartQualityZone
import com.runcheck.ui.components.ChartXLabel
import com.runcheck.ui.components.ChartYLabel
import com.runcheck.ui.theme.statusColors
import kotlin.math.floor
import kotlin.math.roundToInt

// ── Constants ───────────────────────────────────────────────────────────────────

private const val RECENT_SESSION_SAMPLE_COUNT = 4
private const val MIN_SESSION_SPEED_DURATION_MS = 10 * 60_000L
private const val MIN_RECENT_SPEED_DURATION_MS = 5 * 60_000L
internal const val MAX_SESSION_SAMPLE_GAP_MS = 30 * 60_000L
private const val MIN_ESTIMATE_PACE_PER_HOUR = 0.25f
private const val TARGET_CHARGE_EIGHTY = 80
private const val TARGET_CHARGE_FULL = 100

// ── Downsampling ────────────────────────────────────────────────────────────────

fun List<Pair<Long, Float>>.downsamplePairs(maxPoints: Int): List<Pair<Long, Float>> {
    if (maxPoints <= 0) return emptyList()
    if (size <= maxPoints) return this
    if (maxPoints == 1) return listOf(first())
    if (maxPoints == 2) return listOf(first(), last())

    val bucketSize = (size - 2).toDouble() / (maxPoints - 2)
    val originTimestamp = first().first
    var selectedIndex = 0

    return buildList(maxPoints) {
        add(this@downsamplePairs.first())

        for (bucketIndex in 0 until maxPoints - 2) {
            val averageStart =
                (floor((bucketIndex + 1) * bucketSize).toInt() + 1)
                    .coerceAtMost(this@downsamplePairs.size)
            val averageEnd =
                (floor((bucketIndex + 2) * bucketSize).toInt() + 1)
                    .coerceAtMost(this@downsamplePairs.size)
            val averageRange = this@downsamplePairs.subList(averageStart, averageEnd)
            val averageTimestamp =
                averageRange.sumOf { (timestamp, _) -> (timestamp - originTimestamp).toDouble() } /
                    averageRange.size
            val averageValue = averageRange.sumOf { (_, value) -> value.toDouble() } / averageRange.size

            val rangeStart = floor(bucketIndex * bucketSize).toInt() + 1
            val rangeEnd =
                (floor((bucketIndex + 1) * bucketSize).toInt() + 1)
                    .coerceAtMost(this@downsamplePairs.lastIndex)
            val selected = this@downsamplePairs[selectedIndex]
            val selectedTimestamp = (selected.first - originTimestamp).toDouble()
            var largestArea = Double.NEGATIVE_INFINITY
            var nextSelectedIndex = rangeStart

            for (candidateIndex in rangeStart until rangeEnd) {
                val candidate = this@downsamplePairs[candidateIndex]
                val candidateTimestamp = (candidate.first - originTimestamp).toDouble()
                val area =
                    kotlin.math.abs(
                        (selectedTimestamp - averageTimestamp) *
                            (candidate.second - selected.second).toDouble() -
                            (selectedTimestamp - candidateTimestamp) *
                            (averageValue - selected.second.toDouble()),
                    )
                if (area > largestArea) {
                    largestArea = area
                    nextSelectedIndex = candidateIndex
                }
            }

            selectedIndex = nextSelectedIndex
            add(this@downsamplePairs[selectedIndex])
        }

        add(this@downsamplePairs.last())
    }
}

// ── Battery history chart points ────────────────────────────────────────────────

fun List<BatteryReading>.chartPointsFor(
    metric: BatteryHistoryMetric,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
): List<Pair<Long, Float>> =
    when (metric) {
        BatteryHistoryMetric.LEVEL -> {
            map { it.timestamp to it.level.toFloat() }
        }

        BatteryHistoryMetric.TEMPERATURE -> {
            map {
                it.timestamp to convertTemperature(it.temperatureC, temperatureUnit).toFloat()
            }
        }

        BatteryHistoryMetric.CURRENT -> {
            mapNotNull { r -> r.currentMa?.let { r.timestamp to it.toFloat() } }
        }

        BatteryHistoryMetric.VOLTAGE -> {
            map { it.timestamp to it.voltageMv / 1000f }
        }
    }

// ── Session graph points ────────────────────────────────────────────────────────

fun List<BatteryReading>.graphPointsFor(
    metric: SessionGraphMetric,
    window: SessionGraphWindow,
): List<Pair<Long, Float>> {
    if (isEmpty()) return emptyList()

    val filtered =
        window.durationMs?.let { duration ->
            val latestTimestamp = last().timestamp
            filter { latestTimestamp - it.timestamp <= duration }
        } ?: this

    return when (metric) {
        SessionGraphMetric.CURRENT -> {
            filtered.mapNotNull { r -> r.currentMa?.let { r.timestamp to it.toFloat() } }
        }

        SessionGraphMetric.POWER -> {
            filtered.mapNotNull { r ->
                r.currentMa?.let { currentMa ->
                    r.timestamp to (currentMa * (r.voltageMv / 1000f)) / 1000f
                }
            }
        }
    }
}

// ── Charging session summary ────────────────────────────────────────────────────

fun calculateChargingSessionSummary(
    history: List<BatteryReading>,
    currentLevel: Int,
    chargingStatus: ChargingStatus,
): ChargingSessionSummary? {
    if (chargingStatus != ChargingStatus.CHARGING || history.isEmpty()) return null

    val sorted = history.sortedBy { it.timestamp }
    val latestChargingIndex = sorted.lastIndex
    if (sorted[latestChargingIndex].status != ChargingStatus.CHARGING.name) return null

    var startIndex = latestChargingIndex
    while (startIndex > 0 && sorted[startIndex - 1].status == ChargingStatus.CHARGING.name) {
        startIndex--
    }

    val session = sorted.subList(startIndex, latestChargingIndex + 1)
    val first = session.firstOrNull() ?: return null
    val last = session.lastOrNull() ?: return null
    val durationMs = (last.timestamp - first.timestamp).coerceAtLeast(0L)
    val gainPercent = currentLevel - first.level
    val averageSpeedPctPerHour = sessionAverageSpeed(gainPercent, durationMs)
    val recentSpeedPctPerHour = sessionRecentSpeed(session)
    val deliveredMah = sessionDeliveredMah(session)
    val averageCurrentMa = sessionAverageCurrent(session, deliveredMah)
    val averagePowerW =
        averageCurrentMa?.let { currentMa ->
            val avgVoltageV = session.map { it.voltageMv / 1000f }.average().toFloat()
            (currentMa * avgVoltageV) / 1000f
        }
    val paceForEstimate = averageSpeedPctPerHour ?: recentSpeedPctPerHour

    return ChargingSessionSummary(
        startLevel = first.level,
        gainPercent = gainPercent,
        durationMs = durationMs,
        peakTemperatureC = session.maxOf { it.temperatureC },
        averageCurrentMa = averageCurrentMa,
        deliveredMah = deliveredMah,
        averagePowerW = averagePowerW,
        averageSpeedPctPerHour = averageSpeedPctPerHour,
        recentSpeedPctPerHour = recentSpeedPctPerHour,
        remainingTo80Ms =
            estimateRemainingChargeMs(
                currentLevel = currentLevel,
                targetLevel = TARGET_CHARGE_EIGHTY,
                pacePctPerHour = paceForEstimate,
            ),
        remainingTo100Ms =
            estimateRemainingChargeMs(
                currentLevel = currentLevel,
                targetLevel = TARGET_CHARGE_FULL,
                pacePctPerHour = paceForEstimate,
            ),
        readings = session,
    )
}

private fun sessionAverageSpeed(
    gainPercent: Int,
    durationMs: Long,
): Float? {
    if (durationMs < MIN_SESSION_SPEED_DURATION_MS || gainPercent <= 0) return null
    return gainPercent * 3_600_000f / durationMs
}

private fun sessionRecentSpeed(session: List<BatteryReading>): Float? {
    val recent = session.takeLast(RECENT_SESSION_SAMPLE_COUNT)
    if (recent.size < 2) return null
    val first = recent.first()
    val last = recent.last()
    val durationMs = (last.timestamp - first.timestamp).coerceAtLeast(0L)
    val levelGain = last.level - first.level
    if (durationMs < MIN_RECENT_SPEED_DURATION_MS || levelGain <= 0) return null
    return levelGain * 3_600_000f / durationMs
}

private fun sessionDeliveredMah(session: List<BatteryReading>): Int? {
    var deliveredMah = 0f
    var hasIntervals = false

    session.zipWithNext().forEach { (start, end) ->
        val startCurrent = start.currentMa
        val endCurrent = end.currentMa
        val durationMs = end.timestamp - start.timestamp
        if (startCurrent != null && endCurrent != null && durationMs in 1..MAX_SESSION_SAMPLE_GAP_MS) {
            val averageCurrent = ((startCurrent + endCurrent) / 2f).coerceAtLeast(0f)
            deliveredMah += averageCurrent * (durationMs / 3_600_000f)
            hasIntervals = true
        }
    }

    return if (hasIntervals) deliveredMah.roundToInt() else null
}

private fun sessionAverageCurrent(
    session: List<BatteryReading>,
    deliveredMah: Int?,
): Int? {
    if (deliveredMah == null || session.size < 2) return null
    val durationMs = (session.last().timestamp - session.first().timestamp).coerceAtLeast(0L)
    if (durationMs <= 0L) return null
    return (deliveredMah / (durationMs / 3_600_000f)).roundToInt()
}

private fun estimateRemainingChargeMs(
    currentLevel: Int,
    targetLevel: Int,
    pacePctPerHour: Float?,
): Long? {
    if (pacePctPerHour == null || pacePctPerHour < MIN_ESTIMATE_PACE_PER_HOUR || currentLevel >= targetLevel) {
        return null
    }
    return (((targetLevel - currentLevel) / pacePctPerHour) * 3_600_000f).roundToInt().toLong()
}

fun ChargingSessionSummary.hasGraphData(): Boolean =
    readings.graphPointsFor(SessionGraphMetric.CURRENT, SessionGraphWindow.ALL).size >= 2 ||
        readings.graphPointsFor(SessionGraphMetric.POWER, SessionGraphWindow.ALL).size >= 2

// ── Y-axis label builders ───────────────────────────────────────────────────────

fun buildBatteryYLabels(
    minVal: Float,
    maxVal: Float,
): List<ChartYLabel> {
    val range = maxVal - minVal
    if (range < 1f) return emptyList()
    val rawStep = range / 4f
    val step =
        when {
            rawStep >= 20f -> {
                (rawStep / 10f).toInt() * 10f
            }

            rawStep >= 5f -> {
                (rawStep / 5f).toInt() * 5f
            }

            rawStep >= 1f -> {
                rawStep.toInt().toFloat().coerceAtLeast(1f)
            }

            else -> {
                if (rawStep >= 0.1f) {
                    val s = (rawStep * 10).toInt() / 10f
                    s.coerceAtLeast(0.1f)
                } else {
                    0.1f
                }
            }
        }
    val start = (kotlin.math.ceil(minVal / step.toDouble()) * step).toFloat()
    return buildList {
        var v = start
        while (v <= maxVal) {
            val label = if (step < 1f) formatDecimal(v, 1) else "${v.toInt()}"
            add(ChartYLabel(v, label))
            v += step
        }
    }
}

fun buildNetworkYLabels(
    minVal: Float,
    maxVal: Float,
): List<ChartYLabel> {
    val range = maxVal - minVal
    if (range < 1f) return emptyList()
    val rawStep = range / 4f
    val step =
        when {
            rawStep >= 20f -> (rawStep / 10f).toInt() * 10f
            rawStep >= 5f -> (rawStep / 5f).toInt() * 5f
            rawStep >= 1f -> rawStep.toInt().toFloat().coerceAtLeast(1f)
            else -> 1f
        }
    val start = (kotlin.math.ceil(minVal / step.toDouble()) * step).toFloat()
    return buildList {
        var v = start
        while (v <= maxVal) {
            add(ChartYLabel(v, "${v.toInt()}"))
            v += step
        }
    }
}

// ── X-axis label builders ───────────────────────────────────────────────────────

fun buildBatteryXLabels(
    timestamps: List<Long>,
    period: HistoryPeriod,
): List<ChartXLabel> = buildHistoryXLabels(timestamps, period)

fun buildSessionXLabels(timestamps: List<Long>): List<ChartXLabel> = buildXLabels(timestamps, skeleton = "Hm")

fun buildNetworkXLabels(
    timestamps: List<Long>,
    period: HistoryPeriod,
): List<ChartXLabel> = buildHistoryXLabels(timestamps, period)

private fun buildHistoryXLabels(
    timestamps: List<Long>,
    period: HistoryPeriod,
): List<ChartXLabel> = buildXLabels(timestamps, skeleton = historyLabelSkeleton(period))

private fun historyLabelSkeleton(period: HistoryPeriod): String =
    when (period) {
        HistoryPeriod.HOUR,
        HistoryPeriod.SIX_HOURS,
        HistoryPeriod.TWELVE_HOURS,
        HistoryPeriod.SINCE_UNPLUG,
        HistoryPeriod.DAY,
        -> "Hm"

        HistoryPeriod.WEEK -> "EEEHm"

        HistoryPeriod.MONTH, HistoryPeriod.ALL -> "MMMd"
    }

private fun buildXLabels(
    timestamps: List<Long>,
    skeleton: String,
    count: Int = 4,
): List<ChartXLabel> {
    if (timestamps.size < 2) return emptyList()
    val first = timestamps.first()
    val last = timestamps.last()
    val span = last - first
    if (span <= 0) return emptyList()
    return buildList {
        for (i in 0..count) {
            val position = i.toFloat() / count
            val time = first + (span * position).toLong()
            add(ChartXLabel(position, formatLocalizedDateTime(time, skeleton)))
        }
    }
}

// ── Quality zone builders ───────────────────────────────────────────────────────

@Composable
fun batteryQualityZones(
    metric: BatteryHistoryMetric,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
): List<ChartQualityZone>? {
    val colors = MaterialTheme.statusColors
    return when (metric) {
        BatteryHistoryMetric.LEVEL -> {
            listOf(
                ChartQualityZone(minValue = 50f, maxValue = 100f, color = colors.healthy.copy(alpha = 0.06f)),
                ChartQualityZone(minValue = 20f, maxValue = 50f, color = colors.fair.copy(alpha = 0.06f)),
                ChartQualityZone(minValue = 0f, maxValue = 20f, color = colors.critical.copy(alpha = 0.06f)),
            )
        }

        BatteryHistoryMetric.TEMPERATURE -> {
            listOf(
                ChartQualityZone(
                    minValue = convertTemperature(0, temperatureUnit).toFloat(),
                    maxValue = convertTemperature(35, temperatureUnit).toFloat(),
                    color = colors.healthy.copy(alpha = 0.06f),
                ),
                ChartQualityZone(
                    minValue = convertTemperature(35, temperatureUnit).toFloat(),
                    maxValue = convertTemperature(40, temperatureUnit).toFloat(),
                    color = colors.fair.copy(alpha = 0.06f),
                ),
                ChartQualityZone(
                    minValue = convertTemperature(40, temperatureUnit).toFloat(),
                    maxValue = convertTemperature(45, temperatureUnit).toFloat(),
                    color = colors.poor.copy(alpha = 0.06f),
                ),
                ChartQualityZone(
                    minValue = convertTemperature(45, temperatureUnit).toFloat(),
                    maxValue = convertTemperature(60, temperatureUnit).toFloat(),
                    color = colors.critical.copy(alpha = 0.06f),
                ),
            )
        }

        else -> {
            null
        }
    }
}

@Composable
fun signalQualityZones(metric: NetworkHistoryMetric): List<ChartQualityZone>? {
    if (metric != NetworkHistoryMetric.SIGNAL) return null
    val colors = MaterialTheme.statusColors
    return listOf(
        ChartQualityZone(minValue = -50f, maxValue = 0f, color = colors.healthy.copy(alpha = 0.07f)),
        ChartQualityZone(minValue = -60f, maxValue = -50f, color = colors.healthy.copy(alpha = 0.05f)),
        ChartQualityZone(minValue = -70f, maxValue = -60f, color = colors.fair.copy(alpha = 0.06f)),
        ChartQualityZone(minValue = -80f, maxValue = -70f, color = colors.poor.copy(alpha = 0.06f)),
        ChartQualityZone(minValue = -120f, maxValue = -80f, color = colors.critical.copy(alpha = 0.06f)),
    )
}

// ── Unit helpers ────────────────────────────────────────────────────────────────

// ── Composable label resolvers ──────────────────────────────────────────────────

@Composable
fun historyMetricLabel(metric: BatteryHistoryMetric): String =
    when (metric) {
        BatteryHistoryMetric.LEVEL -> stringResource(R.string.battery_history_metric_level)
        BatteryHistoryMetric.TEMPERATURE -> stringResource(R.string.battery_history_metric_temperature)
        BatteryHistoryMetric.CURRENT -> stringResource(R.string.battery_history_metric_current)
        BatteryHistoryMetric.VOLTAGE -> stringResource(R.string.battery_history_metric_voltage)
    }

@Composable
fun sessionGraphMetricLabel(metric: SessionGraphMetric): String =
    when (metric) {
        SessionGraphMetric.CURRENT -> stringResource(R.string.battery_history_metric_current)
        SessionGraphMetric.POWER -> stringResource(R.string.battery_session_graph_metric_power)
    }

@Composable
fun sessionGraphWindowLabel(window: SessionGraphWindow): String =
    when (window) {
        SessionGraphWindow.FIFTEEN_MINUTES -> stringResource(R.string.battery_session_graph_window_15m)
        SessionGraphWindow.THIRTY_MINUTES -> stringResource(R.string.battery_session_graph_window_30m)
        SessionGraphWindow.ALL -> stringResource(R.string.history_period_all)
    }

@Composable
fun historyPeriodLabel(period: HistoryPeriod): String =
    when (period) {
        HistoryPeriod.SINCE_UNPLUG -> stringResource(R.string.history_period_since_unplug)
        HistoryPeriod.HOUR -> stringResource(R.string.history_period_hour)
        HistoryPeriod.SIX_HOURS -> stringResource(R.string.history_period_6h)
        HistoryPeriod.TWELVE_HOURS -> stringResource(R.string.history_period_12h)
        HistoryPeriod.DAY -> stringResource(R.string.history_period_day)
        HistoryPeriod.WEEK -> stringResource(R.string.history_period_week)
        HistoryPeriod.MONTH -> stringResource(R.string.history_period_month)
        HistoryPeriod.ALL -> stringResource(R.string.history_period_all)
    }

@Composable
fun networkHistoryMetricLabel(metric: NetworkHistoryMetric): String =
    when (metric) {
        NetworkHistoryMetric.SIGNAL -> stringResource(R.string.network_history_metric_signal)
        NetworkHistoryMetric.LATENCY -> stringResource(R.string.network_history_metric_latency)
    }

@Composable
fun thermalQualityZones(temperatureUnit: TemperatureUnit): List<ChartQualityZone> {
    val colors = MaterialTheme.statusColors

    fun convert(celsius: Float) = convertTemperature(celsius, temperatureUnit).toFloat()
    return listOf(
        ChartQualityZone(minValue = convert(0f), maxValue = convert(35f), color = colors.healthy.copy(alpha = 0.06f)),
        ChartQualityZone(minValue = convert(35f), maxValue = convert(42f), color = colors.fair.copy(alpha = 0.06f)),
        ChartQualityZone(minValue = convert(42f), maxValue = convert(60f), color = colors.critical.copy(alpha = 0.06f)),
    )
}

@Composable
fun storageQualityZones(metric: StorageHistoryMetric): List<ChartQualityZone>? {
    val statusColors = MaterialTheme.statusColors
    return when (metric) {
        StorageHistoryMetric.USED_SPACE -> {
            listOf(
                ChartQualityZone(0f, 74.999f, statusColors.healthy.copy(alpha = 0.08f)),
                ChartQualityZone(75f, 84.999f, statusColors.fair.copy(alpha = 0.08f)),
                ChartQualityZone(85f, 94.999f, statusColors.poor.copy(alpha = 0.08f)),
                ChartQualityZone(95f, 100f, statusColors.critical.copy(alpha = 0.08f)),
            )
        }

        StorageHistoryMetric.AVAILABLE_SPACE -> {
            null
        }
    }
}

/**
 * Maps a [value] to the full-alpha color of the [ChartQualityZone] it falls within.
 * Zone colors are stored at low alpha (0.06f–0.08f) for background rendering;
 * this helper returns the color at full alpha, suitable for the data line.
 * Returns [defaultColor] when no zone matches.
 */
fun qualityZoneColorForValue(
    value: Float,
    zones: List<ChartQualityZone>,
    defaultColor: Color,
): Color {
    for (zone in zones.asReversed()) {
        if (value >= zone.minValue && value <= zone.maxValue) {
            return zone.color.copy(alpha = 1f)
        }
    }
    return defaultColor
}

@Composable
fun thermalHistoryMetricLabel(metric: ThermalHistoryMetric): String =
    when (metric) {
        ThermalHistoryMetric.BATTERY_TEMP -> stringResource(R.string.thermal_metric_battery_temp)
        ThermalHistoryMetric.CPU_TEMP -> stringResource(R.string.thermal_metric_cpu_temp)
    }

@Composable
fun storageHistoryMetricLabel(metric: StorageHistoryMetric): String =
    when (metric) {
        StorageHistoryMetric.USED_SPACE -> stringResource(R.string.storage_metric_used)
        StorageHistoryMetric.AVAILABLE_SPACE -> stringResource(R.string.storage_metric_available)
    }
