package com.runcheck.ui.fullscreen

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow

sealed interface FullscreenChartSelection {
    data class BatteryHistory(
        val metric: BatteryHistoryMetric,
        val period: HistoryPeriod,
    ) : FullscreenChartSelection {
        val metricOptions: List<BatteryHistoryMetric> get() = BatteryHistoryMetric.entries
        val periodOptions: List<HistoryPeriod> get() = HistoryPeriod.entries
    }

    data class BatterySession(
        val metric: SessionGraphMetric,
        val period: SessionGraphWindow,
    ) : FullscreenChartSelection {
        val metricOptions: List<SessionGraphMetric> get() = SessionGraphMetric.entries
        val periodOptions: List<SessionGraphWindow> get() = SessionGraphWindow.entries
    }

    data class NetworkHistory(
        val metric: NetworkHistoryMetric,
        val period: HistoryPeriod,
    ) : FullscreenChartSelection {
        init {
            require(period != HistoryPeriod.SINCE_UNPLUG)
        }

        val metricOptions: List<NetworkHistoryMetric> get() = NetworkHistoryMetric.entries
        val periodOptions: List<HistoryPeriod> get() = HistoryPeriod.entries.filter { it != HistoryPeriod.SINCE_UNPLUG }
    }

    val source: FullscreenChartSource
        get() =
            when (this) {
                is BatteryHistory -> FullscreenChartSource.BATTERY_HISTORY
                is BatterySession -> FullscreenChartSource.BATTERY_SESSION
                is NetworkHistory -> FullscreenChartSource.NETWORK_HISTORY
            }
}
