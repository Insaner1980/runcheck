package com.devicepulse.data.battery

import android.content.Context
import android.os.BatteryManager
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
        val cycleCount = batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CHARGING_CYCLE_COUNT
        )
        emit(if (cycleCount > 0) cycleCount else null)
    }

    override fun getHealthPercent(): Flow<Int?> = flow {
        val health = batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_STATE_OF_HEALTH
        )
        emit(if (health > 0) health else null)
    }
}
