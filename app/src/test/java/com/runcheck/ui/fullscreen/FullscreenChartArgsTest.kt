package com.runcheck.ui.fullscreen

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenChartArgsTest {
    @Test
    fun `every valid source metric and period round trips at the boundary`() {
        for (metric in BatteryHistoryMetric.entries) {
            for (period in HistoryPeriod.entries) {
                assertRoundTrip(FullscreenChartSelection.BatteryHistory(metric, period))
            }
        }
        for (metric in SessionGraphMetric.entries) {
            for (period in SessionGraphWindow.entries) {
                assertRoundTrip(FullscreenChartSelection.BatterySession(metric, period))
            }
        }
        for (metric in NetworkHistoryMetric.entries) {
            for (period in HistoryPeriod.entries.filter { it != HistoryPeriod.SINCE_UNPLUG }) {
                assertRoundTrip(FullscreenChartSelection.NetworkHistory(metric, period))
            }
        }
    }

    @Test
    fun `missing unknown empty wrong case and cross source input uses existing defaults`() {
        val invalid = listOf(null, "", "unknown", "level", "day")
        for (metric in invalid + "POWER") {
            assertEquals(
                FullscreenChartSelection.BatteryHistory(BatteryHistoryMetric.LEVEL, HistoryPeriod.MONTH),
                parseFullscreenChartSelection("BATTERY_HISTORY", metric, "MONTH"),
            )
        }
        for (period in invalid + "THIRTY_MINUTES") {
            assertEquals(
                FullscreenChartSelection.BatteryHistory(BatteryHistoryMetric.TEMPERATURE, HistoryPeriod.DAY),
                parseFullscreenChartSelection("BATTERY_HISTORY", "TEMPERATURE", period),
            )
        }
        for (metric in invalid + "TEMPERATURE") {
            assertEquals(
                FullscreenChartSelection.BatterySession(SessionGraphMetric.CURRENT, SessionGraphWindow.THIRTY_MINUTES),
                parseFullscreenChartSelection("BATTERY_SESSION", metric, "THIRTY_MINUTES"),
            )
        }
        for (period in invalid + "MONTH") {
            assertEquals(
                FullscreenChartSelection.BatterySession(SessionGraphMetric.POWER, SessionGraphWindow.ALL),
                parseFullscreenChartSelection("BATTERY_SESSION", "POWER", period),
            )
        }
        for (metric in invalid + "TEMPERATURE") {
            assertEquals(
                FullscreenChartSelection.NetworkHistory(NetworkHistoryMetric.SIGNAL, HistoryPeriod.WEEK),
                parseFullscreenChartSelection("NETWORK_HISTORY", metric, "WEEK"),
            )
        }
        for (period in invalid + listOf("THIRTY_MINUTES", "SINCE_UNPLUG")) {
            assertEquals(
                FullscreenChartSelection.NetworkHistory(NetworkHistoryMetric.LATENCY, HistoryPeriod.DAY),
                parseFullscreenChartSelection("NETWORK_HISTORY", "LATENCY", period),
            )
        }
    }

    @Test
    fun `invalid source parser stays nullable while fullscreen defaults to battery session`() {
        for (source in listOf(null, "", "unknown", "battery_history")) {
            assertNull(parseFullscreenChartSource(source))
            assertEquals(
                FullscreenChartSelection.BatterySession(SessionGraphMetric.CURRENT, SessionGraphWindow.ALL),
                parseFullscreenChartSelection(source, null, null),
            )
            assertEquals(
                FullscreenChartSelection.BatterySession(SessionGraphMetric.POWER, SessionGraphWindow.THIRTY_MINUTES),
                parseFullscreenChartSelection(source, "POWER", "THIRTY_MINUTES"),
            )
        }
    }

    @Test
    fun `all retains source specific enum meaning and network options exclude since unplug`() {
        val history =
            parseFullscreenChartSelection(
                "BATTERY_HISTORY",
                null,
                "ALL",
            ) as FullscreenChartSelection.BatteryHistory
        val session =
            parseFullscreenChartSelection(
                "BATTERY_SESSION",
                null,
                "ALL",
            ) as FullscreenChartSelection.BatterySession
        assertEquals(HistoryPeriod.ALL, history.period)
        assertEquals(0L, history.period.durationMs)
        assertEquals(SessionGraphWindow.ALL, session.period)
        assertNull(session.period.durationMs)
        assertEquals(HistoryPeriod.entries, history.periodOptions)
        assertEquals(SessionGraphWindow.entries, session.periodOptions)
        val network =
            parseFullscreenChartSelection(
                "NETWORK_HISTORY",
                null,
                null,
            ) as FullscreenChartSelection.NetworkHistory
        assertEquals(NetworkHistoryMetric.entries, network.metricOptions)
        assertTrue(HistoryPeriod.SINCE_UNPLUG !in network.periodOptions)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `typed network selection rejects since unplug`() {
        FullscreenChartSelection.NetworkHistory(NetworkHistoryMetric.SIGNAL, HistoryPeriod.SINCE_UNPLUG)
    }

    @Test
    fun `typed seeds match identity and are consumed once even on mismatch`() {
        val selections =
            listOf(
                FullscreenChartSelection.BatteryHistory(BatteryHistoryMetric.LEVEL, HistoryPeriod.ALL),
                FullscreenChartSelection.BatterySession(SessionGraphMetric.CURRENT, SessionGraphWindow.ALL),
                FullscreenChartSelection.NetworkHistory(NetworkHistoryMetric.SIGNAL, HistoryPeriod.DAY),
            )
        try {
            for ((index, selection) in selections.withIndex()) {
                val state = FullscreenChartUiState.Empty(selection)
                FullscreenChartSeedStore.prime(selection.source, state)
                assertEquals(state, FullscreenChartSeedStore.take(selection))
                assertNull(FullscreenChartSeedStore.take(selection))
                FullscreenChartSeedStore.prime(selection.source, state)
                assertNull(FullscreenChartSeedStore.take(selections[(index + 1) % selections.size]))
                assertNull(FullscreenChartSeedStore.take(selection))
                val different =
                    when (selection) {
                        is FullscreenChartSelection.BatteryHistory -> {
                            selection.copy(
                                metric = BatteryHistoryMetric.TEMPERATURE,
                            )
                        }

                        is FullscreenChartSelection.BatterySession -> {
                            selection.copy(
                                period = SessionGraphWindow.FIFTEEN_MINUTES,
                            )
                        }

                        is FullscreenChartSelection.NetworkHistory -> {
                            selection.copy(metric = NetworkHistoryMetric.LATENCY)
                        }
                    }
                FullscreenChartSeedStore.prime(selection.source, state)
                assertNull(FullscreenChartSeedStore.take(different))
                assertNull(FullscreenChartSeedStore.take(selection))
            }
        } finally {
            FullscreenChartSeedStore.clear()
        }
    }

    private fun assertRoundTrip(selection: FullscreenChartSelection) {
        assertEquals(
            selection,
            parseFullscreenChartSelection(
                selection.source.name,
                selection.metricArgument(),
                selection.periodArgument(),
            ),
        )
        assertEquals(selection.metricArgument(), sanitizeFullscreenMetric(selection.source, selection.metricArgument()))
        assertEquals(selection.periodArgument(), sanitizeFullscreenPeriod(selection.source, selection.periodArgument()))
    }
}
