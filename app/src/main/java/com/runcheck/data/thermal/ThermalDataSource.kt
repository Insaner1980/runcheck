package com.devicepulse.data.thermal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.devicepulse.domain.model.ThermalStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
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
        val stickyIntent = context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        stickyIntent?.let { receiver.onReceive(context, it) }
        awaitClose { context.unregisterReceiver(receiver) }
    }

    fun getCpuTemperature(thermalZones: List<String>): Flow<Float?> = flow {
        if (thermalZones.isEmpty()) {
            emit(null)
            return@flow
        }

        // Try once — if it fails, emit null and stop (avoids SELinux log spam)
        val temp = readCpuTemperature(thermalZones)
        emit(temp)
        if (temp != null) {
            while (true) {
                delay(POLLING_INTERVAL_MS)
                emit(readCpuTemperature(thermalZones))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getThermalHeadroom(): Flow<Float?> = flow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            while (true) {
                val headroom = try {
                    val value = powerManager.getThermalHeadroom(HEADROOM_FORECAST_SECONDS)
                    if (value.isNaN() || value < 0f) null else value
                } catch (_: Exception) {
                    null
                }
                emit(headroom)
                delay(POLLING_INTERVAL_MS)
            }
        } else {
            emit(null)
        }
    }

    fun getThermalStatus(): Flow<ThermalStatus> = flow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            emit(ThermalStatus.NONE)
            return@flow
        }

        emitAllThermalStatus()
    }

    private fun readCpuTemperature(zones: List<String>): Float? {
        if (zones.isEmpty()) return null

        val thermalDir = File("/sys/class/thermal/")
        if (!thermalDir.exists() || !thermalDir.canRead()) return null

        return try {
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

                if (zones.contains(type)) {
                    val tempStr = tempFile.readText().trim()
                    val tempValue = tempStr.toIntOrNull() ?: continue
                    return if (tempValue > 1000) tempValue / 1000f else tempValue.toFloat()
                }
            }
            null
        } catch (_: SecurityException) {
            null
        }
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
        private const val HEADROOM_FORECAST_SECONDS = 10
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<ThermalStatus>.emitAllThermalStatus() {
        callbackFlow {
            trySend(mapThermalStatus(powerManager.currentThermalStatus))

            val listener = PowerManager.OnThermalStatusChangedListener { status ->
                trySend(mapThermalStatus(status))
            }

            powerManager.addThermalStatusListener(
                ContextCompat.getMainExecutor(context),
                listener
            )

            awaitClose {
                powerManager.removeThermalStatusListener(listener)
            }
        }.collect { emit(it) }
    }
}
