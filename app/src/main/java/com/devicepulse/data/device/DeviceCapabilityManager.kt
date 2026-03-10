package com.devicepulse.data.device

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.devicepulse.domain.model.CurrentUnit
import com.devicepulse.domain.model.SignConvention
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class DeviceCapabilityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun detectCapabilities(): DeviceProfile {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL
        val apiLevel = Build.VERSION.SDK_INT

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val currentValidation = validateCurrentNow(batteryManager)
        val thermalZones = discoverThermalZones()

        return DeviceProfile(
            manufacturer = manufacturer,
            model = model,
            apiLevel = apiLevel,
            currentNowReliable = currentValidation.isReliable,
            currentNowUnit = currentValidation.unit,
            currentNowSignConvention = currentValidation.signConvention,
            cycleCountAvailable = apiLevel >= 34,
            batteryHealthPercentAvailable = apiLevel >= 34,
            thermalZonesAvailable = thermalZones,
            storageHealthAvailable = true
        )
    }

    private suspend fun validateCurrentNow(batteryManager: BatteryManager): CurrentValidation {
        val readings = mutableListOf<Int>()

        repeat(VALIDATION_SAMPLE_COUNT) {
            val current = try {
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            } catch (_: SecurityException) {
                return CurrentValidation(
                    isReliable = false,
                    unit = CurrentUnit.MILLIAMPS,
                    signConvention = SignConvention.POSITIVE_CHARGING
                )
            }
            readings.add(current)
            if (it < VALIDATION_SAMPLE_COUNT - 1) {
                delay(VALIDATION_SAMPLE_DELAY_MS)
            }
        }

        val nonZero = readings.any { it != 0 }
        val changing = readings.distinct().size > 1
        val unit = inferUnit(readings)
        val plausible = readings.all { reading ->
            val normalizedMa = if (unit == CurrentUnit.MICROAMPS) abs(reading) / 1000 else abs(reading)
            normalizedMa in 0..MAX_PLAUSIBLE_CURRENT_MA
        }

        val isReliable = nonZero && changing && plausible
        val signConvention = inferSignConvention(batteryManager, readings)

        return CurrentValidation(
            isReliable = isReliable,
            unit = unit,
            signConvention = signConvention
        )
    }

    private fun inferUnit(readings: List<Int>): CurrentUnit {
        val maxAbs = readings.maxOfOrNull { abs(it) } ?: 0
        return if (maxAbs > MICROAMP_THRESHOLD) CurrentUnit.MICROAMPS else CurrentUnit.MILLIAMPS
    }

    private fun inferSignConvention(
        batteryManager: BatteryManager,
        readings: List<Int>
    ): SignConvention {
        val isCharging = batteryManager.isCharging
        val avgReading = readings.average()

        return if (isCharging && avgReading > 0 || !isCharging && avgReading < 0) {
            SignConvention.POSITIVE_CHARGING
        } else {
            SignConvention.NEGATIVE_CHARGING
        }
    }

    private fun discoverThermalZones(): List<String> {
        val thermalDir = File("/sys/class/thermal/")
        if (!thermalDir.exists()) return emptyList()

        return thermalDir.listFiles()
            ?.filter { it.name.startsWith("thermal_zone") }
            ?.mapNotNull { zone ->
                val tempFile = File(zone, "temp")
                val typeFile = File(zone, "type")
                if (tempFile.exists() && tempFile.canRead()) {
                    val type = if (typeFile.exists() && typeFile.canRead()) {
                        typeFile.readText().trim()
                    } else {
                        zone.name
                    }
                    type
                } else {
                    null
                }
            }
            ?: emptyList()
    }

    private data class CurrentValidation(
        val isReliable: Boolean,
        val unit: CurrentUnit,
        val signConvention: SignConvention
    )

    companion object {
        private const val VALIDATION_SAMPLE_COUNT = 5
        private const val VALIDATION_SAMPLE_DELAY_MS = 500L
        private const val MICROAMP_THRESHOLD = 10000
        private const val MAX_PLAUSIBLE_CURRENT_MA = 10000
    }
}
