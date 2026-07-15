package com.runcheck.widget

import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.PlugType
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryWidgetSnapshotTest {
    @Test
    fun `unavailable persisted current is omitted from the battery widget`() {
        val snapshot =
            batteryReading(currentMa = 0, currentConfidence = Confidence.UNAVAILABLE.name)
                .toBatteryWidgetSnapshot()

        assertNull(snapshot.currentMa)
    }

    private fun batteryReading(
        currentMa: Int?,
        currentConfidence: String,
    ) = BatteryReadingEntity(
        timestamp = 1L,
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
}
