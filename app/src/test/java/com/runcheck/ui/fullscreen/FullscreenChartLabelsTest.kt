package com.runcheck.ui.fullscreen

import android.content.res.Resources
import android.os.Trace
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.LocalResources
import com.runcheck.R
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow
import com.runcheck.ui.chart.historyMetricLabel
import com.runcheck.ui.chart.networkHistoryMetricLabel
import com.runcheck.ui.chart.sessionGraphMetricLabel
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FullscreenChartLabelsTest {
    private val strings =
        mapOf(
            R.string.battery_history_metric_level to "Level",
            R.string.battery_history_metric_temperature to "Temp",
            R.string.battery_history_metric_current to "Current",
            R.string.battery_history_metric_voltage to "Voltage",
            R.string.battery_session_graph_metric_power to "Power",
            R.string.network_history_metric_signal to "Signal",
            R.string.network_history_metric_latency to "Latency",
            R.string.history_period_since_unplug to "Since unplug",
            R.string.history_period_hour to "1h",
            R.string.history_period_6h to "6h",
            R.string.history_period_12h to "12h",
            R.string.history_period_day to "24h",
            R.string.history_period_week to "Week",
            R.string.history_period_month to "Month",
            R.string.history_period_all to "All",
            R.string.battery_session_graph_window_15m to "15m",
            R.string.battery_session_graph_window_30m to "30m",
            R.string.fullscreen_chart_title_battery to "Battery %s",
            R.string.fullscreen_chart_title_session to "Session %s",
            R.string.fullscreen_chart_title_network to "Network %s",
            R.string.a11y_chart_context_history to "Over %s",
            R.string.a11y_chart_context_session to "This charging session",
            R.string.a11y_chart_context_session_window to "Over %s of this charging session",
        )

    @Before
    fun mockAndroidTracing() {
        mockkStatic(Trace::class)
        every { Trace.beginSection(any()) } just Runs
        every { Trace.endSection() } just Runs
    }

    @After
    fun restoreAndroidTracing() {
        unmockkStatic(Trace::class)
    }

    private fun mockResources(): Resources {
        val resources = mockk<Resources>()

        every { resources.getString(any()) } answers { strings.getValue(firstArg()) }
        every { resources.getString(any(), *anyVararg()) } answers {
            strings.getValue(firstArg()).format(
                *args.drop(1).toTypedArray().let {
                    if (it.size == 1 && it[0] is Array<*>) it[0] as Array<*> else it
                },
            )
        }
        return resources
    }

    @Test
    fun `typed labels titles and accessibility time context retain existing strings`() =
        runTest {
            val resources = mockResources()
            val recomposer = Recomposer(coroutineContext)
            val composition = Composition(NoOpApplier(), recomposer)
            val historyPeriods = listOf("Since unplug", "1h", "6h", "12h", "24h", "Week", "Month", "All")
            try {
                composition.setContent {
                    CompositionLocalProvider(LocalResources provides resources) {
                        BatteryHistoryMetric.entries
                            .zip(listOf("Level", "Temp", "Current", "Voltage"))
                            .forEach { (metric, label) ->
                                HistoryPeriod.entries.zip(historyPeriods).forEach { (period, periodLabel) ->
                                    val selection = FullscreenChartSelection.BatteryHistory(metric, period)
                                    assertEquals(label, historyMetricLabel(metric))
                                    assertEquals("Battery $label", resolveChartTitle(selection))
                                    assertEquals(periodLabel, resolvePeriodLabel(selection))
                                    assertEquals("Over $periodLabel", resolveChartTimeContext(selection))
                                }
                            }
                        SessionGraphMetric.entries.zip(listOf("Current", "Power")).forEach { (metric, label) ->
                            SessionGraphWindow.entries
                                .zip(listOf("15m", "30m", "All"))
                                .forEach { (period, periodLabel) ->
                                    val selection = FullscreenChartSelection.BatterySession(metric, period)
                                    assertEquals(label, sessionGraphMetricLabel(metric))
                                    assertEquals("Session $label", resolveChartTitle(selection))
                                    assertEquals(periodLabel, resolvePeriodLabel(selection))
                                    assertEquals(
                                        if (period ==
                                            SessionGraphWindow.ALL
                                        ) {
                                            "This charging session"
                                        } else {
                                            "Over $periodLabel of this charging session"
                                        },
                                        resolveChartTimeContext(selection),
                                    )
                                }
                        }
                        NetworkHistoryMetric.entries.zip(listOf("Signal", "Latency")).forEach { (metric, label) ->
                            HistoryPeriod.entries
                                .zip(
                                    historyPeriods,
                                ).filter { it.first != HistoryPeriod.SINCE_UNPLUG }
                                .forEach { (period, periodLabel) ->
                                    val selection = FullscreenChartSelection.NetworkHistory(metric, period)
                                    assertEquals(label, networkHistoryMetricLabel(metric))
                                    assertEquals("Network $label", resolveChartTitle(selection))
                                    assertEquals(periodLabel, resolvePeriodLabel(selection))
                                    assertEquals("Over $periodLabel", resolveChartTimeContext(selection))
                                }
                        }
                    }
                }
            } finally {
                composition.dispose()
                recomposer.cancel()
            }
        }
}

private class NoOpApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(
        index: Int,
        instance: Unit,
    ) = Unit

    override fun insertBottomUp(
        index: Int,
        instance: Unit,
    ) = Unit

    override fun remove(
        index: Int,
        count: Int,
    ) = Unit

    override fun move(
        from: Int,
        to: Int,
        count: Int,
    ) = Unit

    override fun onClear() = Unit
}
