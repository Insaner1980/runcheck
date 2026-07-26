package com.runcheck.widget

import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryWidgetSnapshotTest {
    @Test
    fun `fresh persisted reading produces widget content`() {
        val state = renderState(timestamp = NOW_MILLIS)

        assertTrue(state is WidgetRenderState.Content)
    }

    @Test
    fun `reading older than configured monitoring freshness is stale`() {
        val staleAge = MonitoringInterval.FIFTEEN.minutes * 60_000L * 3L + 1L

        assertEquals(WidgetRenderState.Stale, renderState(timestamp = NOW_MILLIS - staleAge))
    }

    @Test
    fun `future reading is stale`() {
        assertEquals(WidgetRenderState.Stale, renderState(timestamp = NOW_MILLIS + 1L))
    }

    @Test
    fun `unavailable persisted current is omitted from the battery widget`() {
        val snapshot =
            batteryReading(currentMa = 0, currentConfidence = Confidence.UNAVAILABLE.name)
                .toBatteryWidgetSnapshot()

        assertNull(snapshot.currentMa)
    }

    private fun batteryReading(
        timestamp: Long = 1L,
        currentMa: Int?,
        currentConfidence: String,
    ) = BatteryReadingEntity(
        timestamp = timestamp,
        level = 75,
        voltageMv = 3_900,
        temperatureC = 30f,
        currentMa = currentMa,
        currentConfidence = currentConfidence,
        status = ChargingStatus.DISCHARGING.name,
        plugType = PlugType.NONE.name,
        health = BatteryHealth.GOOD.name,
        cycleCount = null,
        healthPct = null,
    )

    private fun renderState(timestamp: Long): WidgetRenderState<BatteryWidgetSnapshot> =
        batteryWidgetRenderState(
            isPro = true,
            monitoringInterval = MonitoringInterval.FIFTEEN,
            reading =
                batteryReading(
                    timestamp = timestamp,
                    currentMa = 100,
                    currentConfidence = Confidence.HIGH.name,
                ),
            nowMillis = NOW_MILLIS,
        )

    private companion object {
        const val NOW_MILLIS = 100_000_000L
    }
}
