package com.devicepulse.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import com.devicepulse.data.device.DeviceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class Android14BatterySource(
    context: Context,
    profile: DeviceProfile
) : GenericBatterySource(context, profile) {

    override fun getCycleCount(): Flow<Int?> = flow {
        // 1. Try broadcast extra (API 34+, no special permission needed)
        val cycleCount = readCycleCountFromBroadcast()
            // 2. Fall back to BatteryManager property (needs BATTERY_STATS on Android 17+)
            ?: try {
                val value = batteryManager.getIntProperty(PROPERTY_CHARGING_CYCLE_COUNT)
                if (value > 0 && value != Int.MIN_VALUE) value else null
            } catch (_: Exception) {
                null
            }
            // 3. Fall back to sysfs (blocked by SELinux on newer Android)
            ?: readSysfsInt(SYSFS_CYCLE_COUNT)

        emit(cycleCount)
    }

    override fun getHealthPercent(): Flow<Int?> = flow {
        // 1. Try BatteryManager property
        val health = try {
            val value = batteryManager.getIntProperty(PROPERTY_STATE_OF_HEALTH)
            if (value in 1..100) value else null
        } catch (_: Exception) {
            null
        }
            // 2. Fall back to sysfs calculation
            ?: calculateHealthFromSysfs()

        emit(health)
    }

    private fun readCycleCountFromBroadcast(): Int? {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val cycleCount = intent?.getIntExtra(EXTRA_CYCLE_COUNT, -1) ?: -1
            if (cycleCount > 0) cycleCount else null
        } catch (_: Exception) {
            null
        }
    }

    private fun readSysfsInt(path: String): Int? {
        return try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                file.readText().trim().toIntOrNull()?.takeIf { it > 0 }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateHealthFromSysfs(): Int? {
        val chargeFull = readSysfsInt(SYSFS_CHARGE_FULL) ?: return null
        val chargeFullDesign = readSysfsInt(SYSFS_CHARGE_FULL_DESIGN) ?: return null
        if (chargeFullDesign <= 0) return null
        val pct = (chargeFull * 100L / chargeFullDesign).toInt()
        return if (pct in 1..100) pct else null
    }

    companion object {
        private const val PROPERTY_CHARGING_CYCLE_COUNT = 8
        private const val PROPERTY_STATE_OF_HEALTH = 12
        // Broadcast extra added in API 34 — no special permission needed
        private const val EXTRA_CYCLE_COUNT = "android.os.extra.CYCLE_COUNT"
        private const val SYSFS_CYCLE_COUNT = "/sys/class/power_supply/battery/cycle_count"
        private const val SYSFS_CHARGE_FULL = "/sys/class/power_supply/battery/charge_full"
        private const val SYSFS_CHARGE_FULL_DESIGN = "/sys/class/power_supply/battery/charge_full_design"
    }
}
