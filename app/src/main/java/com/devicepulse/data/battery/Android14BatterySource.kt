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
        val cycleCount = try {
            batteryManager.getIntProperty(PROPERTY_CHARGING_CYCLE_COUNT)
        } catch (_: SecurityException) {
            Int.MIN_VALUE
        }
        emit(if (cycleCount > 0) cycleCount else null)
    }

    override fun getHealthPercent(): Flow<Int?> = flow {
        val health = try {
            batteryManager.getIntProperty(PROPERTY_STATE_OF_HEALTH)
        } catch (_: SecurityException) {
            Int.MIN_VALUE
        }
        emit(if (health > 0) health else null)
    }

    companion object {
        private const val PROPERTY_CHARGING_CYCLE_COUNT = 8
        private const val PROPERTY_STATE_OF_HEALTH = 12
    }
}
