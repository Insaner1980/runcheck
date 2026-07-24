package com.runcheck.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MonitoringFreshnessPolicy
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryWidgetFreshnessTest {
    @Test
    fun `fresh battery reading produces content`() {
        val state =
            batteryWidgetRenderState(
                monitoringInterval = MonitoringInterval.FIFTEEN,
                reading = batteryReading(NOW_MILLIS),
                nowMillis = NOW_MILLIS,
            )

        assertTrue(state is WidgetRenderState.Content)
    }

    @Test
    fun `battery reading older than monitoring freshness is stale`() {
        val staleAfter = MonitoringFreshnessPolicy.staleAfterMillis(MonitoringInterval.FIFTEEN.minutes)

        val state =
            batteryWidgetRenderState(
                monitoringInterval = MonitoringInterval.FIFTEEN,
                reading = batteryReading(NOW_MILLIS - staleAfter - 1L),
                nowMillis = NOW_MILLIS,
            )

        assertEquals(WidgetRenderState.Stale, state)
    }

    @Test
    fun `future battery reading is stale`() {
        val state =
            batteryWidgetRenderState(
                monitoringInterval = MonitoringInterval.FIFTEEN,
                reading = batteryReading(NOW_MILLIS + 1L),
                nowMillis = NOW_MILLIS,
            )

        assertEquals(WidgetRenderState.Stale, state)
    }

    @Test
    fun `minimum battery presentation fits its declared bounds at supported font scales`() {
        val minimumSize = DpSize(110.dp, 72.dp)

        listOf(1f, 1.3f, 2f).forEach { fontScale ->
            val presentation = batteryWidgetPresentationFor(minimumSize, fontScale)

            assertEquals(WidgetLayout.COMPACT, presentation.layout)
            assertTrue(minimumSize.height.value >= 48f)
            assertTrue(
                "Battery content and padding must fit at fontScale $fontScale",
                presentation.requiredTotalHeightDp(fontScale) <= minimumSize.height.value,
            )
            assertEquals(1, presentation.valueMaxLines)
            assertEquals(1, presentation.detailMaxLines)
        }
    }

    @Test
    fun `minimum battery presentation keeps production level and temperature compact`() {
        val presentation = batteryWidgetPresentationFor(DpSize(110.dp, 72.dp), fontScale = 2f)
        val text =
            batteryWidgetTextModel(
                level = "80%",
                temperature = "25.0°C",
                current = "100 mA",
                title = "Battery Status",
                presentation = presentation,
            )

        assertEquals("80%", text.level)
        assertEquals("25.0°C", text.temperature)
        assertEquals(null, text.current)
        assertEquals(null, text.title)
    }

    private fun batteryReading(timestamp: Long) =
        BatteryReadingEntity(
            timestamp = timestamp,
            level = 80,
            voltageMv = 4_000,
            temperatureC = 25f,
            currentMa = 100,
            currentConfidence = Confidence.HIGH.name,
            status = ChargingStatus.DISCHARGING.name,
            plugType = PlugType.NONE.name,
            health = BatteryHealth.GOOD.name,
            cycleCount = 100,
            healthPct = 95,
        )

    private companion object {
        const val NOW_MILLIS = 100_000_000L
    }
}
