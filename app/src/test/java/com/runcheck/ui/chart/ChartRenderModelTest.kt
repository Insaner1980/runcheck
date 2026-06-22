package com.runcheck.ui.chart

import androidx.compose.ui.graphics.Color
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.ThermalReading
import com.runcheck.ui.components.ChartQualityZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartRenderModelTest {
    @Test
    fun `battery temperature chart honors fahrenheit preference`() {
        val history =
            listOf(
                batteryReading(timestamp = 1_000L, level = 80, temperatureC = 30f),
                batteryReading(timestamp = 2_000L, level = 79, temperatureC = 31.5f),
                batteryReading(timestamp = 3_000L, level = 78, temperatureC = 33f),
            )

        val points =
            history.chartPointsFor(
                metric = BatteryHistoryMetric.TEMPERATURE,
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
            )

        assertEquals("°F", batteryMetricUnit(BatteryHistoryMetric.TEMPERATURE, TemperatureUnit.FAHRENHEIT))
        assertEquals(86.0f, points.first().second, 0.01f)
        assertEquals(91.4f, points.last().second, 0.01f)
    }

    @Test
    fun `network history chart downsampling keeps bounded point count`() {
        val points =
            (0 until 1_000)
                .map { index ->
                    index * 60_000L to (-40 - (index % 30)).toFloat()
                }.downsamplePairs(300)

        assertEquals(300, points.size)
        assertEquals(0L, points.first().first)
        assertEquals(999 * 60_000L, points.last().first)
        assertTrue(points.zipWithNext().all { (first, second) -> first.first <= second.first })
    }

    @Test
    fun `charging session summary uses latest contiguous charging session`() {
        val history =
            listOf(
                batteryReading(timestamp = 0L, level = 85, status = "DISCHARGING"),
                batteryReading(timestamp = 10_000L, level = 60),
                batteryReading(timestamp = 15 * 60_000L + 10_000L, level = 65, temperatureC = 34f),
                batteryReading(timestamp = 30 * 60_000L + 10_000L, level = 70, temperatureC = 36f),
            )

        val summary =
            calculateChargingSessionSummary(
                history = history,
                currentLevel = 70,
                chargingStatus = ChargingStatus.CHARGING,
            )

        assertNotNull(summary)
        assertEquals(60, summary?.startLevel)
        assertEquals(10, summary?.gainPercent)
        assertEquals(30 * 60_000L, summary?.durationMs)
        assertEquals(36f, summary?.peakTemperatureC ?: 0f, 0.01f)
        assertEquals(600, summary?.deliveredMah)
        assertEquals(1_200, summary?.averageCurrentMa)
        assertEquals(20f, summary?.averageSpeedPctPerHour ?: 0f, 0.01f)
        assertEquals(20f, summary?.recentSpeedPctPerHour ?: 0f, 0.01f)
        assertEquals(30 * 60_000L, summary?.remainingTo80Ms)
        assertEquals(90 * 60_000L, summary?.remainingTo100Ms)
    }

    @Test
    fun `charging session summary is absent when not charging or no charging samples exist`() {
        val dischargingHistory =
            listOf(
                batteryReading(timestamp = 0L, level = 70, status = "DISCHARGING"),
                batteryReading(timestamp = 10 * 60_000L, level = 69, status = "DISCHARGING"),
            )

        assertNull(
            calculateChargingSessionSummary(
                history = dischargingHistory,
                currentLevel = 69,
                chargingStatus = ChargingStatus.DISCHARGING,
            ),
        )
        assertNull(
            calculateChargingSessionSummary(
                history = dischargingHistory,
                currentLevel = 69,
                chargingStatus = ChargingStatus.CHARGING,
            ),
        )
    }

    @Test
    fun `charging summary omits pace estimates for short flat sessions`() {
        val history =
            listOf(
                batteryReading(timestamp = 0L, level = 50),
                batteryReading(timestamp = 4 * 60_000L, level = 50),
            )

        val summary =
            calculateChargingSessionSummary(
                history = history,
                currentLevel = 50,
                chargingStatus = ChargingStatus.CHARGING,
            )

        assertNotNull(summary)
        assertEquals(0, summary?.gainPercent)
        assertNull(summary?.averageSpeedPctPerHour)
        assertNull(summary?.recentSpeedPctPerHour)
        assertNull(summary?.remainingTo80Ms)
        assertNull(summary?.remainingTo100Ms)
    }

    @Test
    fun `session graph points apply selected time window and power conversion`() {
        val history =
            listOf(
                batteryReading(timestamp = 0L, level = 50),
                batteryReading(timestamp = 10 * 60_000L, level = 52),
                batteryReading(timestamp = 20 * 60_000L, level = 54),
                batteryReading(timestamp = 40 * 60_000L, level = 58),
            )

        val points =
            history.graphPointsFor(
                metric = SessionGraphMetric.POWER,
                window = SessionGraphWindow.THIRTY_MINUTES,
            )

        assertEquals(3, points.size)
        assertEquals(10 * 60_000L, points.first().first)
        assertEquals(4.8f, points.first().second, 0.01f)
        assertEquals(40 * 60_000L, points.last().first)
    }

    @Test
    fun `network history chart builder maps latency metric`() {
        val networkModel =
            buildNetworkHistoryChartModel(
                history =
                    listOf(
                        NetworkReading(
                            timestamp = 1_000L,
                            type = "WIFI",
                            signalDbm = null,
                            wifiSpeedMbps = null,
                            wifiFrequency = null,
                            carrier = null,
                            networkSubtype = null,
                            latencyMs = 42,
                        ),
                    ),
                metric = NetworkHistoryMetric.LATENCY,
                period = HistoryPeriod.HOUR,
                maxPoints = 10,
            )
        assertEquals(listOf(42f), networkModel.chartData)
        assertEquals(" ms", networkModel.unit)
        assertTrue(networkModel.xLabels.isEmpty())
    }

    @Test
    fun `thermal history chart builder maps cpu temperature metric`() {
        val thermalModel =
            buildThermalHistoryChartModel(
                history =
                    listOf(
                        ThermalReading(
                            timestamp = 1_000L,
                            batteryTempC = 35f,
                            cpuTempC = 40f,
                            thermalStatus = 3,
                            throttling = true,
                        ),
                    ),
                metric = ThermalHistoryMetric.CPU_TEMP,
                period = HistoryPeriod.HOUR,
                maxPoints = 10,
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
            )

        assertEquals(listOf(104f), thermalModel.chartData)
        assertEquals(" °F", thermalModel.unit)
        assertEquals(1, thermalModel.tooltipDecimals)
    }

    @Test
    fun `storage history chart builder maps used space metric`() {
        val storageModel =
            buildStorageHistoryChartModel(
                history =
                    listOf(
                        StorageReading(
                            timestamp = 1_000L,
                            totalBytes = 5L * GIB,
                            availableBytes = 2L * GIB,
                            appsBytes = 1L * GIB,
                            mediaBytes = 1L * GIB,
                        ),
                    ),
                metric = StorageHistoryMetric.USED_SPACE,
                period = HistoryPeriod.HOUR,
                maxPoints = 10,
            )

        assertEquals(listOf(3f), storageModel.chartData)
        assertEquals(" GB", storageModel.unit)
    }

    @Test
    fun `battery history model handles empty history without labels`() {
        val model =
            buildBatteryHistoryChartModel(
                history = emptyList(),
                metric = BatteryHistoryMetric.LEVEL,
                period = HistoryPeriod.HOUR,
                temperatureUnit = TemperatureUnit.CELSIUS,
                maxPoints = 10,
            )

        assertTrue(model.chartData.isEmpty())
        assertTrue(model.chartTimestamps.isEmpty())
        assertTrue(model.yLabels.isEmpty())
        assertTrue(model.xLabels.isEmpty())
        assertNull(model.averageValue)
    }

    @Test
    fun `axis label builders reject tiny ranges and round visible steps`() {
        assertTrue(buildBatteryYLabels(4f, 4.5f).isEmpty())
        assertTrue(buildNetworkYLabels(-55f, -54.5f).isEmpty())

        assertEquals(
            listOf("0", "20", "40", "60", "80"),
            buildBatteryYLabels(0f, 83f).map { it.label },
        )
        assertEquals(
            listOf("-90", "-80", "-70", "-60", "-50"),
            buildNetworkYLabels(-95f, -50f).map { it.label },
        )
    }

    @Test
    fun `quality zone color returns matched color at full alpha and default outside zones`() {
        val defaultColor = Color.White
        val zoneColor = Color.Red.copy(alpha = 0.08f)
        val zones = listOf(ChartQualityZone(minValue = 40f, maxValue = 45f, color = zoneColor))

        val matched = qualityZoneColorForValue(42f, zones, defaultColor)
        val outside = qualityZoneColorForValue(30f, zones, defaultColor)

        assertEquals(Color.Red.red, matched.red, 0.0f)
        assertEquals(Color.Red.green, matched.green, 0.0f)
        assertEquals(Color.Red.blue, matched.blue, 0.0f)
        assertEquals(1f, matched.alpha, 0.0f)
        assertEquals(defaultColor, outside)
    }

    private fun batteryReading(
        timestamp: Long,
        level: Int,
        temperatureC: Float = 30f,
        status: String = "CHARGING",
    ) = BatteryReading(
        id = timestamp,
        timestamp = timestamp,
        level = level,
        voltageMv = 4_000,
        temperatureC = temperatureC,
        currentMa = 1_200,
        currentConfidence = "HIGH",
        status = status,
        plugType = "USB",
        health = "GOOD",
        cycleCount = null,
        healthPct = null,
    )

    private companion object {
        private const val GIB = 1024L * 1024L * 1024L
    }
}
