package com.devicepulse.data.battery

import android.content.Context
import android.os.BatteryManager
import com.devicepulse.data.device.DeviceProfile
import com.devicepulse.domain.model.Confidence
import com.devicepulse.domain.model.MeasuredValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.abs

class SamsungBatterySource(
    context: Context,
    profile: DeviceProfile
) : GenericBatterySource(context, profile) {

    override fun getCurrentNow(): Flow<MeasuredValue<Int>> = flow {
        var previousCurrentMa: Int? = null
        var stableReadingCount = 0

        while (true) {
            val rawCurrent = readCurrentNowRaw()
            if (rawCurrent == null) {
                emit(unavailableCurrent())
                delay(POLLING_INTERVAL_MS)
                continue
            }
            val currentMa = normalizeCurrent(rawCurrent)

            stableReadingCount = if (currentMa == previousCurrentMa) {
                stableReadingCount + 1
            } else {
                1
            }
            previousCurrentMa = currentMa

            // Samsung devices may report max theoretical current instead of actual
            // Flag long-lived constant high-current readings as LOW confidence.
            val looksLikeMaxTheoreticalCurrent = stableReadingCount >= STABLE_READING_THRESHOLD &&
                abs(currentMa) >= SUSPICIOUS_CONSTANT_CURRENT_MA

            val confidence = when {
                rawCurrent == 0 -> Confidence.UNAVAILABLE
                !profile.currentNowReliable || looksLikeMaxTheoreticalCurrent -> Confidence.LOW
                else -> Confidence.HIGH
            }

            emit(MeasuredValue(currentMa, confidence))
            delay(POLLING_INTERVAL_MS)
        }
    }

    companion object {
        private const val POLLING_INTERVAL_MS = 2000L
        private const val STABLE_READING_THRESHOLD = 3
        private const val SUSPICIOUS_CONSTANT_CURRENT_MA = 3000
    }
}
