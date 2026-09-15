package com.runcheck.ui.common

import com.runcheck.testutil.findRootDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.readText

/** Source contracts complement the classifier and chart JVM tests; they do not render Compose. */
class BatteryTemperaturePresentationConsumersTest {
    private val uiRoot = findRootDir().resolve("app/src/main/java/com/runcheck/ui")

    private fun source(path: String) = uiRoot.resolve(path).readText().replace(Regex("\\s+"), " ")

    @Test
    fun `battery detail and live line use canonical status while Home uses severity directly`() {
        val battery = source("battery/BatteryDetailScreen.kt")
        assertTrue(battery.contains("valueColor = statusColorForBatteryTemperature(battery.temperatureC)"))
        assertTrue(battery.contains("lineColor = statusColorForBatteryTemperature(battery.temperatureC)"))
        assertFalse(battery.contains("fun temperatureColor"))
        assertTrue(
            source("theme/StatusColors.kt").contains(
                "statusColor(BatteryTemperaturePresentation.classify(tempC).severity)",
            ),
        )
        assertTrue(
            source("home/HomeStatusTiles.kt").contains(
                "BatteryTemperaturePresentation.classify(state.thermalState.batteryTempC).severity",
            ),
        )
    }

    @Test
    fun `descriptive resource mapping preserves all five words`() {
        val formatter =
            source("common/UiFormatters.kt")
                .substringAfter("fun temperatureBandLabel")
                .substringBefore("@Composable")
        assertTrue(formatter.contains("BatteryTemperaturePresentation.classify(temperatureC)"))
        mapOf("COOL" to "cool", "NORMAL" to "normal", "WARM" to "warm", "HOT" to "hot", "CRITICAL" to "critical")
            .forEach { (band, resource) ->
                assertTrue(
                    formatter.contains(
                        "BatteryTemperaturePresentation.Band.$band -> stringResource(R.string.thermal_$resource)",
                    ),
                )
            }
    }

    @Test
    fun `thermal hero segments agree with raw Celsius classification at boundaries`() {
        val thermal = source("thermal/ThermalDetailScreen.kt")
        assertTrue(thermal.contains("val tempColor = statusColorForBatteryTemperature(thermal.batteryTempC)"))
        assertTrue(thermal.contains("val bandLabel = temperatureBandLabel(thermal.batteryTempC)"))
        val segmentBlock = thermal.substringAfter("val thermalSegments =").substringBefore("RuncheckCard(")
        val starts =
            listOf(
                "0f",
                "BatteryTemperaturePresentation.FAIR_START_C",
                "BatteryTemperaturePresentation.POOR_START_C",
                "BatteryTemperaturePresentation.CRITICAL_START_C",
            )
        assertEquals(starts, Regex("rangeStart = ([\\w.]+)").findAll(segmentBlock).map { it.groupValues[1] }.toList())
        val labels = listOf("healthy", "fair", "poor", "critical")
        labels.forEach { label ->
            assertTrue(thermal.contains("val ${label}Label = stringResource(R.string.status_$label)"))
            assertTrue(segmentBlock.contains("label = ${label}Label, color = statusColors.$label"))
        }
        // The unchanged shared component selects the last inclusive lower boundary.
        assertTrue(source("components/SegmentedStatusBar.kt").contains("indexOfLast { currentValue >= it.rangeStart }"))
        val lowerBounds =
            listOf(
                0f,
                BatteryTemperaturePresentation.FAIR_START_C,
                BatteryTemperaturePresentation.POOR_START_C,
                BatteryTemperaturePresentation.CRITICAL_START_C,
            )
        listOf(25f, 35f, 40f, 45f).forEach { celsius ->
            assertEquals(
                BatteryTemperaturePresentation
                    .classify(celsius)
                    .severity.name
                    .lowercase(),
                labels[lowerBounds.indexOfLast { celsius >= it }],
            )
        }
        assertTrue(thermal.contains("currentValue = thermal.batteryTempC"))
    }

    @Test
    fun `history fullscreen CPU and heat strip keep their intended owners`() {
        val thermal = source("thermal/ThermalDetailScreen.kt")
        assertTrue(thermal.contains("thermalQualityZones(metric, temperatureUnit)"))
        assertTrue(thermal.contains("statusColorForCpuTemperature(it)"))
        assertTrue(source("fullscreen/FullscreenChartScreen.kt").contains("batteryQualityZones(selection.metric,"))
        assertTrue(
            source("chart/ChartHelpers.kt").contains(
                "BatteryHistoryMetric.TEMPERATURE -> { batteryTemperatureQualityZones(temperatureUnit, colors) }",
            ),
        )
        val heat = source("components/HeatStrip.kt")
        assertTrue(heat.contains("shouldPulseHeatStrip(temperatureC, MaterialTheme.reducedMotion)"))
        assertTrue(heat.contains("temperatureBandLabel(temperatureC)"))
        listOf("FAIR", "POOR", "CRITICAL").forEach { severity ->
            assertTrue(heat.contains("tempToStop(BatteryTemperaturePresentation.${severity}_START_C) - transitionHalf"))
            assertTrue(heat.contains("tempToStop(BatteryTemperaturePresentation.${severity}_START_C) + transitionHalf"))
        }
        assertTrue(heat.contains("val transitionHalf = 1f / rangeC"))
    }
}
