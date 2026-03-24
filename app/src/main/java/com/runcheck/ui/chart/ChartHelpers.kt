package com.runcheck.ui.chart

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.ui.common.convertTemperature
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.formatLocalizedDateTime
import androidx.compose.ui.graphics.Color
import com.runcheck.ui.components.ChartQualityZone
import com.runcheck.ui.components.ChartXLabel
import com.runcheck.ui.components.ChartYLabel
import com.runcheck.ui.theme.statusColors
import kotlin.math.roundToInt

// ── Constants ───────────────────────────────────────────────────────────────────

private const val RECENT_SESSION_SAMPLE_COUNT = 4
private const val MIN_SESSION_SPEED_DURATION_MS = 10 * 60_000L
private const val MIN_RECENT_SPEED_DURATION_MS = 5 * 60_000L
private const val MAX_DELIVERY_INTERVAL_MS = 30 * 60_000L
private const val MIN_ESTIMATE_PACE_PER_HOUR = 0.25f
private const val TARGET_CHARGE_EIGHTY = 80
private const val TARGET_CHARGE_FULL = 100

// ── Downsampling ────────────────────────────────────────────────────────────────

fun <T> List<T>.downsamplePairs(maxPoints: Int): List<T> {
    if (size <= maxPoints || maxPoints <= 1) return this
    val lastIdx = lastIndex
    return buildList(maxPoints) {
        for (index in 0 until maxPoints) {
            val sourceIndex = ((index.toLong() * lastIdx) / (maxPoints - 1)).toInt()
            add(this@downsamplePairs[sourceIndex])
        }
    }
}

// ── Battery history chart points ────────────────────────────────────────────────

fun List<BatteryReading>.chartPointsFor(
    metric: BatteryHistoryMetric,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
): List<Pair<Long, Float>> = when (metric) {
    BatteryHistoryMetric.LEVEL -> map { it.timestamp to it.level.toFloat() }
    BatteryHistoryMetric.TEMPERATURE -> map {
        it.timestamp to convertTemperature(it.temperatureC, temperatureUnit).toFloat()
    }
    BatteryHistoryMetric.CURRENT -> mapNotNull { r -> r.currentMa?.let { r.timestamp to it.toFloat() } }
    BatteryHistoryMetric.VOLTAGE -> map { it.timestamp to it.voltageMv / 1000f }
}

// ── Session graph points ────────────────────────────────────────────────────────

fun List<BatteryReading>.graphPointsFor(
    metric: SessionGraphMetric,
    window: SessionGraphWindow
): List<Pair<Long, Float>> {
    if (isEmpty()) return emptyList()

    val filtered = window.durationMs?.let { duration ->
        val latestTimestamp = last().timestamp
        filter { latestTimestamp - it.timestamp <= duration }
    } ?: this

    return when (metric) {
        SessionGraphMetric.CURRENT -> filtered.mapNotNull { r -> r.currentMa?.let { r.timestamp to it.toFloat() } }
        SessionGraphMetric.POWER -> filtered.mapNotNull { r ->
            r.currentMa?.let { currentMa ->
                r.timestamp to (currentMa * (r.voltageMv / 1000f)) / 1000f
            }
        }
    }
}

// ── Charging session summary ────────────────────────────────────────────────────

fun calculateChargingSessionSummary(
    history: List<BatteryReading>,
    currentLevel: Int,
    chargingStatus: ChargingStatus
): ChargingSessionSummary? {
    if (chargingStatus != ChargingStatus.CHARGING || history.isEmpty()) return null

    val sorted = history.sortedBy { it.timestamp }
    val latestChargingIndex = sorted.indexOfLast { it.status == ChargingStatus.CHARGING.name }
    if (latestChargingIndex == -1) return null

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
    val averagePowerW = averageCurrentMa?.let { currentMa ->
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
        remainingTo80Ms = estimateRemainingChargeMs(
            currentLevel = currentLevel,
            targetLevel = TARGET_CHARGE_EIGHTY,
            pacePctPerHour = paceForEstimate
        ),
        remainingTo100Ms = estimateRemainingChargeMs(
            currentLevel = currentLevel,
            targetLevel = TARGET_CHARGE_FULL,
            pacePctPerHour = paceForEstimate
        ),
        readings = session
    )
}

private fun sessionAverageSpeed(gainPercent: Int, durationMs: Long): Float? {
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
        if (startCurrent != null && endCurrent != null && durationMs in 1..MAX_DELIVERY_INTERVAL_MS) {
            val averageCurrent = ((startCurrent + endCurrent) / 2f).coerceAtLeast(0f)
            deliveredMah += averageCurrent * (durationMs / 3_600_000f)
            hasIntervals = true
        }
    }

    return if (hasIntervals) deliveredMah.roundToInt() else null
}

private fun sessionAverageCurrent(
    session: List<BatteryReading>,
    deliveredMah: Int?
): Int? {
    if (deliveredMah == null || session.size < 2) return null
    val durationMs = (session.last().timestamp - session.first().timestamp).coerceAtLeast(0L)
    if (durationMs <= 0L) return null
    return (deliveredMah / (durationMs / 3_600_000f)).roundToInt()
}

private fun estimateRemainingChargeMs(
    currentLevel: Int,
    targetLevel: Int,
    pacePctPerHour: Float?
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

fun buildBatteryYLabels(minVal: Float, maxVal: Float): List<ChartYLabel> {
    val range = maxVal - minVal
    if (range < 1f) return emptyList()
    val rawStep = range / 4f
    val step = when {
        rawStep >= 20f -> (rawStep / 10f).toInt() * 10f
        rawStep >= 5f -> (rawStep / 5f).toInt() * 5f
        rawStep >= 1f -> rawStep.toInt().toFloat().coerceAtLeast(1f)
        else -> if (rawStep >= 0.1f) {
            val s = (rawStep * 10).toInt() / 10f
            s.coerceAtLeast(0.1f)
        } else 0.1f
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

fun buildNetworkYLabels(minVal: Float, maxVal: Float): List<ChartYLabel> {
    val range = maxVal - minVal
    if (range < 1f) return emptyList()
    val rawStep = range / 4f
    val step = when {
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

fun buildBatteryXLabels(timestamps: List<Long>, period: HistoryPeriod): List<ChartXLabel> {
    if (timestamps.size < 2) return emptyList()
    val first = timestamps.first()
    val last = timestamps.last()
    val span = last - first
    if (span <= 0) return emptyList()
    val (skeleton, count) = when (period) {
        HistoryPeriod.HOUR, HistoryPeriod.SIX_HOURS, HistoryPeriod.TWELVE_HOURS -> "Hm" to 4
        HistoryPeriod.SINCE_UNPLUG, HistoryPeriod.DAY -> "Hm" to 4
        HistoryPeriod.WEEK -> "EEEHm" to 4
        HistoryPeriod.MONTH -> "MMMd" to 4
        HistoryPeriod.ALL -> "MMMd" to 4
    }
    return buildList {
        for (i in 0..count) {
            val position = i.toFloat() / count
            val time = first + (span * position).toLong()
            add(ChartXLabel(position, formatLocalizedDateTime(time, skeleton)))
        }
    }
}

fun buildSessionXLabels(timestamps: List<Long>): List<ChartXLabel> {
    if (timestamps.size < 2) return emptyList()
    val first = timestamps.first()
    val last = timestamps.last()
    val span = last - first
    if (span <= 0) return emptyList()
    val count = 4
    return buildList {
        for (i in 0..count) {
            val position = i.toFloat() / count
            val time = first + (span * position).toLong()
            add(ChartXLabel(position, formatLocalizedDateTime(time, "Hm")))
        }
    }
}

fun buildNetworkXLabels(timestamps: List<Long>, period: HistoryPeriod): List<ChartXLabel> {
    if (timestamps.size < 2) return emptyList()
    val first = timestamps.first()
    val last = timestamps.last()
    val span = last - first
    if (span <= 0) return emptyList()
    val (skeleton, count) = when (period) {
        HistoryPeriod.HOUR, HistoryPeriod.SIX_HOURS, HistoryPeriod.TWELVE_HOURS -> "Hm" to 4
        HistoryPeriod.DAY, HistoryPeriod.SINCE_UNPLUG -> "Hm" to 4
        HistoryPeriod.WEEK -> "EEEHm" to 4
        HistoryPeriod.MONTH -> "MMMd" to 4
        HistoryPeriod.ALL -> "MMMd" to 4
    }
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
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
): List<ChartQualityZone>? {
    val colors = MaterialTheme.statusColors
    return when (metric) {
        BatteryHistoryMetric.LEVEL -> listOf(
            ChartQualityZone(minValue = 50f, maxValue = 100f, color = colors.healthy.copy(alpha = 0.06f)),
            ChartQualityZone(minValue = 20f, maxValue = 50f, color = colors.fair.copy(alpha = 0.06f)),
            ChartQualityZone(minValue = 0f, maxValue = 20f, color = colors.critical.copy(alpha = 0.06f))
        )
        BatteryHistoryMetric.TEMPERATURE -> listOf(
            ChartQualityZone(
                minValue = convertTemperature(0, temperatureUnit).toFloat(),
                maxValue = convertTemperature(35, temperatureUnit).toFloat(),
                color = colors.healthy.copy(alpha = 0.06f)
            ),
            ChartQualityZone(
                minValue = convertTemperature(35, temperatureUnit).toFloat(),
                maxValue = convertTemperature(40, temperatureUnit).toFloat(),
                color = colors.fair.copy(alpha = 0.06f)
            ),
            ChartQualityZone(
                minValue = convertTemperature(40, temperatureUnit).toFloat(),
                maxValue = convertTemperature(45, temperatureUnit).toFloat(),
                color = colors.poor.copy(alpha = 0.06f)
            ),
            ChartQualityZone(
                minValue = convertTemperature(45, temperatureUnit).toFloat(),
                maxValue = convertTemperature(60, temperatureUnit).toFloat(),
                color = colors.critical.copy(alpha = 0.06f)
            )
        )
        else -> null
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
        ChartQualityZone(minValue = -120f, maxValue = -80f, color = colors.critical.copy(alpha = 0.06f))
    )
}

// ── Unit helpers ────────────────────────────────────────────────────────────────

fun batteryMetricUnit(
    metric: BatteryHistoryMetric,
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS
): String = when (metric) {
    BatteryHistoryMetric.LEVEL -> "%"
    BatteryHistoryMetric.TEMPERATURE -> if (temperatureUnit == TemperatureUnit.CELSIUS) "°C" else "°F"
    BatteryHistoryMetric.CURRENT -> " mA"
    BatteryHistoryMetric.VOLTAGE -> " V"
}

fun networkMetricUnit(metric: NetworkHistoryMetric): String = when (metric) {
    NetworkHistoryMetric.SIGNAL -> " dBm"
    NetworkHistoryMetric.LATENCY -> " ms"
}

fun sessionMetricUnit(metric: SessionGraphMetric): String = when (metric) {
    SessionGraphMetric.CURRENT -> " mA"
    SessionGraphMetric.POWER -> " W"
}

// ── Composable label resolvers ──────────────────────────────────────────────────

@Composable
fun historyMetricLabel(metric: BatteryHistoryMetric): String = when (metric) {
    BatteryHistoryMetric.LEVEL -> stringResource(R.string.battery_history_metric_level)
    BatteryHistoryMetric.TEMPERATURE -> stringResource(R.string.battery_history_metric_temperature)
    BatteryHistoryMetric.CURRENT -> stringResource(R.string.battery_history_metric_current)
    BatteryHistoryMetric.VOLTAGE -> stringResource(R.string.battery_history_metric_voltage)
}

@Composable
fun sessionGraphMetricLabel(metric: SessionGraphMetric): String = when (metric) {
    SessionGraphMetric.CURRENT -> stringResource(R.string.battery_history_metric_current)
    SessionGraphMetric.POWER -> stringResource(R.string.battery_session_graph_metric_power)
}

@Composable
fun sessionGraphWindowLabel(window: SessionGraphWindow): String = when (window) {
    SessionGraphWindow.FIFTEEN_MINUTES -> stringResource(R.string.battery_session_graph_window_15m)
    SessionGraphWindow.THIRTY_MINUTES -> stringResource(R.string.battery_session_graph_window_30m)
    SessionGraphWindow.ALL -> stringResource(R.string.history_period_all)
}

@Composable
fun historyPeriodLabel(period: HistoryPeriod): String = when (period) {
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
fun networkHistoryMetricLabel(metric: NetworkHistoryMetric): String = when (metric) {
    NetworkHistoryMetric.SIGNAL -> stringResource(R.string.network_history_metric_signal)
    NetworkHistoryMetric.LATENCY -> stringResource(R.string.network_history_metric_latency)
}

@Composable
fun thermalQualityZones(temperatureUnit: TemperatureUnit): List<ChartQualityZone> {
    val colors = MaterialTheme.statusColors
    fun convert(c: Float) = if (temperatureUnit == TemperatureUnit.FAHRENHEIT) c * 9f / 5f + 32f else c
    return listOf(
        ChartQualityZone(minValue = convert(0f), maxValue = convert(35f), color = colors.healthy.copy(alpha = 0.06f)),
        ChartQualityZone(minValue = convert(35f), maxValue = convert(42f), color = colors.fair.copy(alpha = 0.06f)),
        ChartQualityZone(minValue = convert(42f), maxValue = convert(60f), color = colors.critical.copy(alpha = 0.06f))
    )
}

@Composable
fun storageQualityZones(metric: StorageHistoryMetric): List<ChartQualityZone>? {
    val statusColors = MaterialTheme.statusColors
    return when (metric) {
        StorageHistoryMetric.USED_SPACE -> listOf(
            ChartQualityZone(0f, 70f, statusColors.healthy.copy(alpha = 0.08f)),
            ChartQualityZone(70f, 90f, statusColors.fair.copy(alpha = 0.08f)),
            ChartQualityZone(90f, 100f, statusColors.critical.copy(alpha = 0.08f))
        )
        StorageHistoryMetric.AVAILABLE_SPACE -> null
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
    defaultColor: Color
): Color {
    for (zone in zones) {
        if (value >= zone.minValue && value < zone.maxValue) {
            return zone.color.copy(alpha = 1f)
        }
    }
    return defaultColor
}

@Composable
fun thermalHistoryMetricLabel(metric: ThermalHistoryMetric): String = when (metric) {
    ThermalHistoryMetric.BATTERY_TEMP -> stringResource(R.string.thermal_metric_battery_temp)
    ThermalHistoryMetric.CPU_TEMP -> stringResource(R.string.thermal_metric_cpu_temp)
}

@Composable
fun storageHistoryMetricLabel(metric: StorageHistoryMetric): String = when (metric) {
    StorageHistoryMetric.USED_SPACE -> stringResource(R.string.storage_metric_used)
    StorageHistoryMetric.AVAILABLE_SPACE -> stringResource(R.string.storage_metric_available)
}
