package com.runcheck.service.monitor

import com.runcheck.R
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveNotificationCurrentTest {
    @Test
    fun `unavailable current is omitted from live notification`() {
        assertNull(batteryState(currentMa = 0, confidence = Confidence.UNAVAILABLE).currentForLiveNotification())
    }

    @Test
    fun `available current is retained for live notification`() {
        assertEquals(
            MeasuredValue(-420, Confidence.HIGH),
            batteryState(currentMa = -420, confidence = Confidence.HIGH).currentForLiveNotification(),
        )
    }

    @Test
    fun `estimated current is labelled in live notification`() {
        val current = batteryState(currentMa = -420, confidence = Confidence.LOW).currentForLiveNotification()

        assertEquals(MeasuredValue(-420, Confidence.LOW), current)
        assertEquals(
            R.string.live_notif_estimated_current,
            current?.let { liveNotificationCurrentLabelRes(it.confidence) },
        )
    }

    private fun batteryState(
        currentMa: Int,
        confidence: Confidence,
    ) = BatteryState(
        level = 75,
        voltageMv = 3_900,
        temperatureC = 30f,
        currentMa = MeasuredValue(currentMa, confidence),
        chargingStatus = ChargingStatus.DISCHARGING,
        plugType = PlugType.NONE,
        health = BatteryHealth.GOOD,
        technology = "Li-ion",
    )
}
