package com.runcheck.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import com.runcheck.data.device.DeviceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
open class Android14BatterySource(
    context: Context,
    profile: DeviceProfile,
) : GenericBatterySource(context, profile) {
    override fun getCycleCount(): Flow<Int?> =
        flow {
            val cycleCount =
                readCycleCountFromBroadcast()
                    // Keep the OEM-specific sysfs fallback for devices that expose it.
                    ?: readSysfsInt(SYSFS_CYCLE_COUNT)?.takeIf { it <= MAX_PLAUSIBLE_CYCLE_COUNT }

            emit(cycleCount)
        }.flowOn(Dispatchers.IO)

    override fun getHealthPercent(): Flow<Int?> =
        flow {
            emit(calculateHealthFromSysfs())
        }.flowOn(Dispatchers.IO)

    private fun readCycleCountFromBroadcast(): Int? =
        try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val cycleCount = intent?.getIntExtra(EXTRA_CYCLE_COUNT, -1) ?: -1
            if (cycleCount > 0) cycleCount else null
        } catch (_: Exception) {
            null
        }

    private fun readSysfsInt(path: String): Int? =
        try {
            val file = File(path)
            if (file.exists() && file.canRead()) {
                file
                    .readText()
                    .trim()
                    .toIntOrNull()
                    ?.takeIf { it > 0 }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

    private fun calculateHealthFromSysfs(): Int? {
        val chargeFull =
            readSysfsInt(SYSFS_CHARGE_FULL)
                ?.takeIf { it >= MIN_PLAUSIBLE_CHARGE_VALUE } ?: return null
        val chargeFullDesign =
            readSysfsInt(SYSFS_CHARGE_FULL_DESIGN)
                ?.takeIf { it >= MIN_PLAUSIBLE_CHARGE_VALUE } ?: return null
        if (chargeFull > chargeFullDesign) return null
        val pct = (chargeFull * 100L / chargeFullDesign).toInt()
        return if (pct in 1..100) pct else null
    }

    companion object {
        // Broadcast extra added in API 34 — no special permission needed
        private const val EXTRA_CYCLE_COUNT = "android.os.extra.CYCLE_COUNT"
        private const val SYSFS_CYCLE_COUNT = "/sys/class/power_supply/battery/cycle_count"
        private const val SYSFS_CHARGE_FULL = "/sys/class/power_supply/battery/charge_full"
        private const val SYSFS_CHARGE_FULL_DESIGN = "/sys/class/power_supply/battery/charge_full_design"
        private const val MAX_PLAUSIBLE_CYCLE_COUNT = 10_000
        private const val MIN_PLAUSIBLE_CHARGE_VALUE = 100_000
    }
}
