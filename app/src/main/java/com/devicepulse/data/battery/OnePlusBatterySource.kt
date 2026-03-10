package com.devicepulse.data.battery

import android.content.Context
import android.os.BatteryManager
import com.devicepulse.data.device.DeviceProfile
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.CurrentUnit
import com.devicepulse.domain.model.MeasuredValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.abs

class OnePlusBatterySource(
    context: Context,
    profile: DeviceProfile
) : GenericBatterySource(context, profile) {

    override fun getCurrentNow(): Flow<MeasuredValue<Int>> = flow {
        while (true) {
            val rawCurrent = batteryManager.getIntProperty(
                BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
            )

            // OnePlus/SUPERVOOC may use inverted sign convention
            // Normalize: positive = charging, negative = discharging
            val milliamps = when (profile.currentNowUnit) {
                CurrentUnit.MICROAMPS -> rawCurrent / 1000
                CurrentUnit.MILLIAMPS -> rawCurrent
            }

            // SUPERVOOC sign is often inverted from standard convention
            val normalizedMa = abs(milliamps)
            val isCharging = batteryManager.isCharging
            val currentMa = if (isCharging) normalizedMa else -normalizedMa

            val confidence = if (profile.currentNowReliable) Confidence.HIGH else Confidence.LOW
            emit(MeasuredValue(currentMa, confidence))
            delay(POLLING_INTERVAL_MS)
        }
    }

    companion object {
        private const val POLLING_INTERVAL_MS = 2000L
    }
}
