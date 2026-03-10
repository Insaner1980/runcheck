package com.devicepulse.data.battery

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.devicepulse.data.device.DeviceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class Android14BatterySource(
    context: Context,
    profile: DeviceProfile
) : GenericBatterySource(context, profile) {

    override fun getCycleCount(): Flow<Int?> = flow {
        // BatteryManager.BATTERY_PROPERTY_CHARGING_CYCLE_COUNT = 8 (API 34, not in public SDK)
        val cycleCount = batteryManager.getIntProperty(PROPERTY_CHARGING_CYCLE_COUNT)
        emit(if (cycleCount > 0) cycleCount else null)
    }

    override fun getHealthPercent(): Flow<Int?> = flow {
        // BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH (API 34, not in public SDK)
        val health = batteryManager.getIntProperty(PROPERTY_STATE_OF_HEALTH)
        emit(if (health > 0) health else null)
    }

    companion object {
        // These constants are defined in BatteryManager source but not exposed in public SDK
        private const val PROPERTY_CHARGING_CYCLE_COUNT = 8
        private const val PROPERTY_STATE_OF_HEALTH = 12
    }
}
