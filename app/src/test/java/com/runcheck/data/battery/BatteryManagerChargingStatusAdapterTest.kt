package com.runcheck.data.battery

import android.os.BatteryManager
import com.runcheck.domain.model.ChargingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryManagerChargingStatusAdapterTest {
    @Test
    fun `battery manager statuses map to charging status`() {
        val cases =
            listOf(
                BatteryManager.BATTERY_STATUS_CHARGING to ChargingStatus.CHARGING,
                BatteryManager.BATTERY_STATUS_DISCHARGING to ChargingStatus.DISCHARGING,
                BatteryManager.BATTERY_STATUS_FULL to ChargingStatus.FULL,
                BatteryManager.BATTERY_STATUS_NOT_CHARGING to ChargingStatus.NOT_CHARGING,
                BatteryManager.BATTERY_STATUS_UNKNOWN to ChargingStatus.NOT_CHARGING,
                0 to ChargingStatus.NOT_CHARGING,
                Int.MAX_VALUE to ChargingStatus.NOT_CHARGING,
            )

        for ((status, expected) in cases) {
            assertEquals(expected, chargingStatusFromBatteryManager(status))
        }
    }
}
