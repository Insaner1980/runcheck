package com.runcheck.ui.chart

import android.text.format.DateFormat
import com.runcheck.R
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.NetworkReading
import com.runcheck.ui.components.isIsolatedChartPoint
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class NetworkSignalHistoryTest {
    @Before
    fun setUpDateFormatting() {
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } returns "MMM d HH:mm"
    }

    @After
    fun tearDownDateFormatting() {
        unmockkStatic(DateFormat::class)
    }

    private fun reading(
        time: Long = 0L,
        type: String = "WIFI",
        dbm: Int? = -60,
        subtype: String? = null,
    ) = NetworkReading(time, type, dbm, null, null, null, subtype, 42)

    private fun model(
        history: List<NetworkReading>,
        budget: Int = 300,
    ) = buildNetworkHistoryChartModel(history, NetworkHistoryMetric.SIGNAL, HistoryPeriod.DAY, budget)

    @Test
    fun `single families retain numeric values and persisted context`() {
        for ((type, subtype, family) in listOf(
            Triple("WIFI", null, NetworkSignalFamily.WIFI),
            Triple("CELLULAR", "5G", NetworkSignalFamily.FIVE_G),
            Triple("CELLULAR", "4G LTE", NetworkSignalFamily.CELLULAR),
            Triple("CELLULAR", null, NetworkSignalFamily.CELLULAR),
            Triple("CELLULAR", "Unknown", NetworkSignalFamily.CELLULAR),
        )) {
            val chart = model(listOf(reading(type = type, subtype = subtype), reading(1, type, -95, subtype)))
            assertEquals(listOf(-60f, -95f), chart.chartData)
            assertEquals(setOf(family), chart.networkSignalFamilies)
            assertEquals(
                List(2) { NetworkSignalContext(ConnectionType.valueOf(type), subtype) },
                chart.networkSignalContexts,
            )
            assertTrue(chart.lineBreakIndices.isEmpty())
        }
    }

    @Test
    fun `continuity uses radio families without inventing missing 5G identity`() {
        val wifi = reading()
        val lte = reading(type = "CELLULAR", subtype = "4G LTE")
        val fiveG = lte.copy(networkSubtype = "5G NSA")
        for (same in listOf(wifi, lte, fiveG)) assertTrue(isContinuousNetworkSignal(same, same.copy(timestamp = 1)))
        for ((first, second) in listOf(wifi to lte, lte to fiveG, fiveG to lte)) {
            assertFalse(isContinuousNetworkSignal(first, second))
            assertEquals(setOf(1), model(listOf(first, second)).lineBreakIndices)
        }
        assertTrue(isContinuousNetworkSignal(lte, lte.copy(networkSubtype = null)))
        assertFalse(isContinuousNetworkSignal(fiveG, lte.copy(networkSubtype = null)))
        assertEquals(NetworkSignalFamily.CELLULAR, NetworkSignalContext(ConnectionType.CELLULAR, "5g").family)
    }

    @Test
    fun `null and non radio intervals break lines without zero points`() {
        for (type in listOf("WIFI", "CELLULAR", "NONE", "VPN", "ETHERNET")) {
            val chart = model(listOf(reading(), reading(1, type, null), reading(2, dbm = -62)))
            assertEquals(listOf(-60f, -62f), chart.chartData)
            assertEquals(listOf(0L, 2L), chart.chartTimestamps)
            assertEquals(setOf(1), chart.lineBreakIndices)
        }
        assertTrue(model(listOf(reading(type = "NONE", dbm = null))).chartData.isEmpty())
    }

    @Test
    fun `unknown persisted type keeps its point isolated with unknown context`() {
        val chart = model(listOf(reading(), reading(1, "SATELLITE", -45), reading(2)))

        assertEquals(listOf(-60f, -45f, -60f), chart.chartData)
        assertEquals(listOf(0L, 1L, 2L), chart.chartTimestamps)
        assertEquals(
            listOf(ConnectionType.WIFI, null, ConnectionType.WIFI),
            chart.networkSignalContexts.map { it.connectionType },
        )
        assertEquals(setOf(NetworkSignalFamily.WIFI, NetworkSignalFamily.UNKNOWN), chart.networkSignalFamilies)
        assertEquals(setOf(1, 2), chart.lineBreakIndices)
    }

    @Test
    fun `known persisted NONE omits its signal point and breaks continuity`() {
        val chart = model(listOf(reading(), reading(1, "NONE", -45), reading(2)))

        assertEquals(listOf(-60f, -60f), chart.chartData)
        assertEquals(listOf(0L, 2L), chart.chartTimestamps)
        assertEquals(
            listOf(ConnectionType.WIFI, ConnectionType.WIFI),
            chart.networkSignalContexts.map { it.connectionType },
        )
        assertEquals(setOf(1), chart.lineBreakIndices)
    }

    @Test
    fun `duplicate timestamp and value keep the selected observation context`() {
        val history = listOf(reading(), reading(type = "CELLULAR", subtype = "5G"), reading(type = "CELLULAR"))
        val all = model(history)
        assertEquals(listOf(null, "5G", null), all.networkSignalContexts.map { it.networkSubtype })
        assertEquals(setOf(1, 2), all.lineBreakIndices)
        val endpoints = model(history, 2)
        assertEquals(
            listOf(ConnectionType.WIFI, ConnectionType.CELLULAR),
            endpoints.networkSignalContexts.map {
                it.connectionType
            },
        )
        assertEquals(listOf(null, null), endpoints.networkSignalContexts.map { it.networkSubtype })
        assertEquals(setOf(1), endpoints.lineBreakIndices)
        assertEquals(listOf("5G"), model(history.drop(1), 1).networkSignalContexts.map { it.networkSubtype })
    }

    @Test
    fun `embedded and fullscreen reduction preserve every crossed original segment`() {
        val history =
            (0..2400).map { index ->
                when {
                    index in 800..805 -> reading(index.toLong(), "CELLULAR", -95, "5G")
                    index == 1600 -> reading(index.toLong(), dbm = null)
                    else -> reading(index.toLong(), dbm = -60 - index % 7)
                }
            }
        for (budget in listOf(MAX_NETWORK_HISTORY_POINTS, MAX_FULLSCREEN_CHART_POINTS)) {
            val chart = model(history, budget)
            assertEquals(budget, chart.chartData.size)
            assertEquals(0L, chart.chartTimestamps.first())
            assertEquals(2400L, chart.chartTimestamps.last())
            chart.chartTimestamps.zipWithNext().forEachIndexed { index, (first, last) ->
                val crossedBreak =
                    (first.toInt() + 1..last.toInt()).any {
                        !isContinuousNetworkSignal(history[it - 1], history[it])
                    }
                assertEquals(crossedBreak, index + 1 in chart.lineBreakIndices)
            }
            chart.chartTimestamps.forEachIndexed { index, timestamp ->
                val original = history[timestamp.toInt()]
                assertEquals(original.signalDbm?.toFloat(), chart.chartData[index])
                assertEquals(original.networkSubtype, chart.networkSignalContexts[index].networkSubtype)
            }
            assertTrue(chart.lineBreakIndices.size >= 2)
        }
    }

    @Test
    fun `an omitted intervening family cannot reconnect equal endpoint families`() {
        val chart = model(listOf(reading(), reading(1, "CELLULAR", -60, "5G"), reading(2)), 2)
        assertEquals(setOf(1), chart.lineBreakIndices)
        assertEquals(
            listOf(ConnectionType.WIFI, ConnectionType.WIFI),
            chart.networkSignalContexts.map { it.connectionType },
        )
        assertEquals(
            R.string.network_signal_history_mixed,
            networkSignalHistoryContextResource(chart.networkSignalFamilies),
        )
    }

    @Test
    fun `many breaks and zero or one point budgets remain valid`() {
        val history = (0..1200).map { reading(it.toLong(), if (it % 2 == 0) "WIFI" else "CELLULAR") }
        for (budget in listOf(300, 600)) {
            val chart = model(history, budget)
            assertEquals((1 until budget).toSet(), chart.lineBreakIndices)
        }
        assertTrue(model(history, 0).chartData.isEmpty())
        assertEquals(listOf(-60f), model(history, 1).chartData)
        assertTrue(model(history, 1).lineBreakIndices.isEmpty())
    }

    @Test
    fun `numeric scale preserves low narrow and constant dBm values`() {
        for (values in listOf(listOf(-95, -60), listOf(-140, -130), listOf(-61, -60), listOf(-95, -95))) {
            val chart = model(values.mapIndexed { index, value -> reading(index.toLong(), dbm = value) })
            assertEquals(values.map(Int::toFloat), chart.chartData)
            assertEquals(" dBm", chart.unit)
            assertTrue(chart.yLabels.all { it.value in (values.min() - 1f)..(values.max() + 1f) })
            if (values.distinct().size == 1) {
                assertTrue(chart.yLabels.first().value < values.first())
                assertTrue(chart.yLabels.last().value > values.first())
            }
        }
    }

    @Test
    fun `tooltip keeps dBm and time and appends only supplied historical context`() {
        val chart = model(listOf(reading(), reading(1, "CELLULAR", -95, "4G LTE")))
        val plain = formatChartTooltip(chart, 1, " | ")
        assertTrue(plain.startsWith("-95 dBm | "))
        assertEquals("$plain | 4G LTE", formatChartTooltip(chart, 1, " | ", "4G LTE"))
    }

    @Test
    fun `accessibility identifies families without quality claims`() {
        val expected =
            mapOf(
                NetworkSignalFamily.WIFI to R.string.network_signal_history_wifi,
                NetworkSignalFamily.FIVE_G to R.string.network_signal_history_5g,
                NetworkSignalFamily.CELLULAR to R.string.network_signal_history_cellular,
                NetworkSignalFamily.UNKNOWN to R.string.network_signal_history_unknown,
            )
        expected.forEach { (family, resource) ->
            assertEquals(resource, networkSignalHistoryContextResource(setOf(family)))
        }
        assertEquals(null, networkSignalHistoryContextResource(emptySet()))
        val snapshot = buildChartAccessibilitySnapshot(listOf(-60f, -95f), " dBm", 0)
        assertEquals(ChartTrendDirection.DECREASING, snapshot?.trendDirection)
    }

    @Test
    fun `latency still uses numeric downsampling with no signal context or breaks`() {
        val history = listOf(reading(), reading(1, "NONE", null), reading(2, "CELLULAR", -95, "5G"))
        val chart = buildNetworkHistoryChartModel(history, NetworkHistoryMetric.LATENCY, HistoryPeriod.DAY, 2)
        assertEquals(listOf(42f, 42f), chart.chartData)
        assertEquals(listOf(0L, 2L), chart.chartTimestamps)
        assertTrue(chart.lineBreakIndices.isEmpty())
        assertTrue(chart.networkSignalContexts.isEmpty())
        assertTrue(chart.networkSignalFamilies.isEmpty())
        assertTrue(chart.yLabels.isEmpty())
    }

    @Test
    fun `isolated segment markers do not affect continuous lines`() {
        assertFalse(isIsolatedChartPoint(0, 3, emptySet()))
        assertFalse(isIsolatedChartPoint(1, 3, emptySet()))
        assertFalse(isIsolatedChartPoint(2, 3, emptySet()))
        for (index in 0..2) assertTrue(isIsolatedChartPoint(index, 3, setOf(1, 2)))
        assertFalse(isIsolatedChartPoint(1, 3, setOf(1)))
    }

    @Test
    fun `both render paths use neutral defaults and forward original breaks and context`() {
        val root =
            generateSequence(File(".").absoluteFile) { it.parentFile }
                .first { File(it, "app/src/main").isDirectory }

        fun source(path: String) = File(root, "app/src/main/java/com/runcheck/$path").readText()
        val embedded =
            source("ui/network/NetworkDetailScreen.kt")
                .substringAfter("private fun SignalHistoryCard(")
                .substringBefore("private fun SpeedTestSummaryCard(")
        val fullscreen =
            source("ui/fullscreen/FullscreenChartScreen.kt")
                .substringAfter("private fun FullscreenChartContent(")
                .substringBefore("private fun FullscreenChartEmptyContent(")
        assertTrue(embedded.contains("qualityZones = null"))
        assertTrue(
            Regex("is FullscreenChartSelection.NetworkHistory -> \\{\\s*null\\s*\\}").containsMatchIn(fullscreen),
        )
        assertFalse(source("ui/chart/ChartHelpers.kt").contains("fun signalQualityZones"))
        assertFalse(fullscreen.contains("statusColor"))
        assertFalse(fullscreen.contains("networkState"))
        assertFalse(embedded.contains("networkState"))
        assertFalse(embedded.contains("lineColor ="))
        assertFalse(fullscreen.contains("lineColor ="))
        assertTrue(embedded.contains("chartModel.toFullscreenSuccess("))
        val mapping =
            source(
                "ui/fullscreen/FullscreenChartViewModel.kt",
            ).substringAfter("internal fun ChartRenderModel.toFullscreenSuccess(")
                .substringBefore("sealed interface FullscreenChartUiState")
        assertTrue(mapping.contains("lineBreakIndices = lineBreakIndices"))
        assertTrue(mapping.contains("networkSignalContexts = networkSignalContexts"))
        assertTrue(mapping.contains("networkSignalFamilies = networkSignalFamilies"))
        assertTrue(source("ui/chart/HistoryChartContent.kt").contains("lineBreakIndices = chartModel.lineBreakIndices"))
        assertTrue(fullscreen.contains("lineBreakIndices = state.lineBreakIndices"))
        assertTrue(fullscreen.contains("pointContext = pointContexts.getOrNull(index)"))
    }
}
