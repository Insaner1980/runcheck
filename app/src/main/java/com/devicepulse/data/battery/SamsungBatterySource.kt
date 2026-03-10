package com.devicepulse.data.battery

import android.content.Context
import android.os.BatteryManager
import com.devicepulse.data.device.DeviceProfile
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.MeasuredValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SamsungBatterySource(
    context: Context,
    profile: DeviceProfile
) : GenericBatterySource(context, profile) {

    override fun getCurrentNow(): Flow<MeasuredValue<Int>> = flow {
        while (true) {
            val rawCurrent = batteryManager.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
            )
            val currentMa = normalizeCurrent(rawCurrent)

            // Samsung devices may report max theoretical current instead of actual
            // Mark as LOW confidence if the value seems suspiciously constant
            val confidence = if (profile.currentNowReliable) {
                Confidence.HIGH
            } else {
                Confidence.LOW
            }

            emit(MeasuredValue(currentMa, confidence))
            delay(POLLING_INTERVAL_MS)
        }
    }

    companion object {
        private const val POLLING_INTERVAL_MS = 2000L
    }
}
