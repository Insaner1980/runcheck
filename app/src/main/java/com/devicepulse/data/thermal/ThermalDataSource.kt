package com.devicepulse.data.thermal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.devicepulse.data.device.DeviceProfile
import com.devicepulse.domain.model.ThermalStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalDataSource @Inject constructor(
    private val context: Context
) {

    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun getBatteryTemperature(): Flow<Float> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                trySend(temp)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }

    fun getCpuTemperature(thermalZones: List<String>): Flow<Float?> = flow {
        while (true) {
            val temp = readCpuTemperature(thermalZones)
            emit(temp)
            delay(POLLING_INTERVAL_MS)
        }
    }

    fun getThermalStatus(): Flow<ThermalStatus> = flow {
        while (true) {
            val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mapThermalStatus(powerManager.currentThermalStatus)
            } else {
                ThermalStatus.NONE
            }
            emit(status)
            delay(POLLING_INTERVAL_MS)
        }
    }

    private fun readCpuTemperature(zones: List<String>): Float? {
        val thermalDir = File("/sys/class/thermal/")
        if (!thermalDir.exists()) return null

        val zoneFiles = thermalDir.listFiles()
            ?.filter { it.name.startsWith("thermal_zone") }
            ?: return null

        for (zone in zoneFiles) {
            val typeFile = File(zone, "type")
            val tempFile = File(zone, "temp")

            if (!tempFile.exists() || !tempFile.canRead()) continue

            val type = if (typeFile.exists() && typeFile.canRead()) {
                typeFile.readText().trim()
            } else {
                zone.name
            }

            if (zones.isEmpty() || zones.contains(type)) {
                val tempStr = tempFile.readText().trim()
                val tempValue = tempStr.toIntOrNull() ?: continue
                // Thermal zone temps are usually in millidegrees
                return if (tempValue > 1000) tempValue / 1000f else tempValue.toFloat()
            }
        }
        return null
    }

    private fun mapThermalStatus(status: Int): ThermalStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalStatus.NONE
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.SHUTDOWN
            else -> ThermalStatus.NONE
        }
    }

    companion object {
        private const val POLLING_INTERVAL_MS = 3000L
    }
}
