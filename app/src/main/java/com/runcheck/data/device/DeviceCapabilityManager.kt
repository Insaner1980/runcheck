package com.runcheck.data.device

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.SignConvention
import com.runcheck.util.BatteryIntentReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class DeviceCapabilityManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        suspend fun detectCapabilities(): DeviceProfile {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val model = Build.MODEL
            val apiLevel = Build.VERSION.SDK_INT

            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            val currentValidation = validateCurrentNow(batteryManager)
            val cycleCountAvailable = detectCycleCountAvailability(apiLevel)

            return DeviceProfile(
                manufacturer = manufacturer,
                model = model,
                apiLevel = apiLevel,
                currentNowReliable = currentValidation.isReliable,
                currentNowUnit = currentValidation.unit,
                currentNowSignConvention = currentValidation.signConvention,
                cycleCountAvailable = cycleCountAvailable,
                thermalZonesAvailable = emptyList(),
                storageHealthAvailable = true,
            )
        }

        private suspend fun validateCurrentNow(batteryManager: BatteryManager): CurrentValidation {
            val readings = mutableListOf<Int>()

            repeat(VALIDATION_SAMPLE_COUNT) {
                val current =
                    try {
                        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    } catch (_: Exception) {
                        return CurrentValidation(
                            isReliable = false,
                            unit = CurrentUnit.MICROAMPS,
                            signConvention = SignConvention.POSITIVE_CHARGING,
                        )
                    }
                if (current == Int.MIN_VALUE) {
                    return CurrentValidation(
                        isReliable = false,
                        unit = CurrentUnit.MICROAMPS,
                        signConvention = SignConvention.POSITIVE_CHARGING,
                    )
                }
                readings.add(current)
                if (it < VALIDATION_SAMPLE_COUNT - 1) {
                    delay(VALIDATION_SAMPLE_DELAY_MS)
                }
            }

            val unit = inferUnit(readings)
            val isReliable = isCurrentNowReliable(readings)
            val signConvention = inferSignConvention(batteryManager.isCharging, readings)

            return CurrentValidation(
                isReliable = isReliable,
                unit = unit,
                signConvention = signConvention,
            )
        }

        private fun inferUnit(readings: List<Int>): CurrentUnit = Companion.inferUnit(readings)

        private fun inferSignConvention(
            isCharging: Boolean,
            readings: List<Int>,
        ): SignConvention = Companion.inferSignConvention(isCharging, readings)

        private fun detectCycleCountAvailability(apiLevel: Int): Boolean {
            if (apiLevel < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
            val batteryIntent = BatteryIntentReader.readBatteryChangedStickyIntent(context)
            val cycleCount = batteryIntent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1
            return cycleCount > 0
        }

        private data class CurrentValidation(
            val isReliable: Boolean,
            val unit: CurrentUnit,
            val signConvention: SignConvention,
        )

        companion object {
            private const val VALIDATION_SAMPLE_COUNT = 3
            private const val VALIDATION_SAMPLE_DELAY_MS = 300L

            private const val MAX_PLAUSIBLE_CURRENT_MA = 10000

            @VisibleForTesting
            internal fun inferUnit(_readings: List<Int>): CurrentUnit = CurrentUnit.MICROAMPS

            @VisibleForTesting
            internal fun isCurrentNowReliable(readings: List<Int>): Boolean {
                val unit = inferUnit(readings)
                return readings.any { it != 0 } &&
                    readings.all { reading ->
                        val normalizedMa = if (unit == CurrentUnit.MICROAMPS) abs(reading) / 1000 else abs(reading)
                        normalizedMa in 0..MAX_PLAUSIBLE_CURRENT_MA
                    }
            }

            @VisibleForTesting
            internal fun inferSignConvention(
                isCharging: Boolean,
                readings: List<Int>,
            ): SignConvention {
                val avgReading = readings.average()
                val chargingUsesPositiveReadings =
                    if (isCharging) {
                        avgReading > 0
                    } else {
                        avgReading < 0
                    }
                return if (chargingUsesPositiveReadings) {
                    SignConvention.POSITIVE_CHARGING
                } else {
                    SignConvention.NEGATIVE_CHARGING
                }
            }
        }
    }
