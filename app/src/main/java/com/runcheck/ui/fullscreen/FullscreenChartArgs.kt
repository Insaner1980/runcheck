package com.runcheck.ui.fullscreen

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow

fun parseFullscreenChartSource(rawSource: String?): FullscreenChartSource? =
    FullscreenChartSource.entries.firstOrNull { it.name == rawSource }

fun parseFullscreenChartSelection(
    rawSource: String?,
    rawMetric: String?,
    rawPeriod: String?,
): FullscreenChartSelection =
    decodeFullscreenChartSelection(
        parseFullscreenChartSource(rawSource) ?: FullscreenChartSource.BATTERY_SESSION,
        rawMetric,
        rawPeriod,
    )

private fun decodeFullscreenChartSelection(
    source: FullscreenChartSource,
    rawMetric: String?,
    rawPeriod: String?,
): FullscreenChartSelection =
    when (source) {
        FullscreenChartSource.BATTERY_HISTORY -> {
            FullscreenChartSelection.BatteryHistory(
                BatteryHistoryMetric.entries.firstOrNull { it.name == rawMetric } ?: BatteryHistoryMetric.LEVEL,
                HistoryPeriod.entries.firstOrNull { it.name == rawPeriod } ?: HistoryPeriod.DAY,
            )
        }

        FullscreenChartSource.BATTERY_SESSION -> {
            FullscreenChartSelection.BatterySession(
                SessionGraphMetric.entries.firstOrNull { it.name == rawMetric } ?: SessionGraphMetric.CURRENT,
                SessionGraphWindow.entries.firstOrNull { it.name == rawPeriod } ?: SessionGraphWindow.ALL,
            )
        }

        FullscreenChartSource.NETWORK_HISTORY -> {
            FullscreenChartSelection.NetworkHistory(
                NetworkHistoryMetric.entries.firstOrNull { it.name == rawMetric } ?: NetworkHistoryMetric.SIGNAL,
                HistoryPeriod.entries.firstOrNull { it.name == rawPeriod && it != HistoryPeriod.SINCE_UNPLUG }
                    ?: HistoryPeriod.DAY,
            )
        }
    }

fun sanitizeFullscreenMetric(
    source: FullscreenChartSource,
    rawMetric: String?,
): String = decodeFullscreenChartSelection(source, rawMetric, null).metricArgument()

fun sanitizeFullscreenPeriod(
    source: FullscreenChartSource,
    rawPeriod: String?,
): String = decodeFullscreenChartSelection(source, null, rawPeriod).periodArgument()

/** Stable String encoding for SavedStateHandle and navigation results. */
internal fun FullscreenChartSelection.metricArgument(): String =
    when (this) {
        is FullscreenChartSelection.BatteryHistory -> metric.name
        is FullscreenChartSelection.BatterySession -> metric.name
        is FullscreenChartSelection.NetworkHistory -> metric.name
    }

internal fun FullscreenChartSelection.periodArgument(): String =
    when (this) {
        is FullscreenChartSelection.BatteryHistory -> period.name
        is FullscreenChartSelection.BatterySession -> period.name
        is FullscreenChartSelection.NetworkHistory -> period.name
    }

fun fullscreenChartRequiresPro(source: FullscreenChartSource): Boolean =
    source == FullscreenChartSource.BATTERY_HISTORY ||
        source == FullscreenChartSource.NETWORK_HISTORY
