package com.runcheck.ui.fullscreen

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow

fun parseFullscreenChartSource(rawSource: String?): FullscreenChartSource? =
    FullscreenChartSource.entries.firstOrNull { it.name == rawSource }

fun sanitizeFullscreenMetric(
    source: FullscreenChartSource,
    rawMetric: String?,
): String =
    when (source) {
        FullscreenChartSource.BATTERY_HISTORY -> {
            sanitizeEnumName(
                rawValue = rawMetric,
                allowedNames = BatteryHistoryMetric.entries.map { it.name },
                defaultValue = BatteryHistoryMetric.LEVEL.name,
            )
        }

        FullscreenChartSource.BATTERY_SESSION -> {
            sanitizeEnumName(
                rawValue = rawMetric,
                allowedNames = SessionGraphMetric.entries.map { it.name },
                defaultValue = SessionGraphMetric.CURRENT.name,
            )
        }

        FullscreenChartSource.NETWORK_HISTORY -> {
            sanitizeEnumName(
                rawValue = rawMetric,
                allowedNames = NetworkHistoryMetric.entries.map { it.name },
                defaultValue = NetworkHistoryMetric.SIGNAL.name,
            )
        }
    }

fun sanitizeFullscreenPeriod(
    source: FullscreenChartSource,
    rawPeriod: String?,
): String =
    when (source) {
        FullscreenChartSource.BATTERY_HISTORY -> {
            sanitizeEnumName(
                rawValue = rawPeriod,
                allowedNames = HistoryPeriod.entries.map { it.name },
                defaultValue = HistoryPeriod.DAY.name,
            )
        }

        FullscreenChartSource.BATTERY_SESSION -> {
            sanitizeEnumName(
                rawValue = rawPeriod,
                allowedNames = SessionGraphWindow.entries.map { it.name },
                defaultValue = SessionGraphWindow.ALL.name,
            )
        }

        FullscreenChartSource.NETWORK_HISTORY -> {
            sanitizeEnumName(
                rawValue = rawPeriod,
                allowedNames =
                    HistoryPeriod.entries
                        .filter { it != HistoryPeriod.SINCE_UNPLUG }
                        .map { it.name },
                defaultValue = HistoryPeriod.DAY.name,
            )
        }
    }

fun fullscreenChartRequiresPro(source: FullscreenChartSource): Boolean =
    source == FullscreenChartSource.BATTERY_HISTORY ||
        source == FullscreenChartSource.NETWORK_HISTORY

private fun sanitizeEnumName(
    rawValue: String?,
    allowedNames: List<String>,
    defaultValue: String,
): String = rawValue?.takeIf { it in allowedNames } ?: defaultValue
