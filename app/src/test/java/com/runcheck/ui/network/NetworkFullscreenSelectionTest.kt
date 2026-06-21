package com.runcheck.ui.network

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.ui.chart.NetworkHistoryMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkFullscreenSelectionTest {
    @Test
    fun `fullscreen result resolves metric and period together`() {
        val result =
            resolveNetworkFullscreenSelection(
                rawMetric = NetworkHistoryMetric.LATENCY.name,
                rawPeriod = HistoryPeriod.WEEK.name,
            )

        assertEquals(NetworkHistoryMetric.LATENCY, result?.metric)
        assertEquals(HistoryPeriod.WEEK, result?.period)
    }

    @Test
    fun `fullscreen result is ignored until both metric and period are present`() {
        assertNull(
            resolveNetworkFullscreenSelection(
                rawMetric = NetworkHistoryMetric.LATENCY.name,
                rawPeriod = null,
            ),
        )
    }
}
