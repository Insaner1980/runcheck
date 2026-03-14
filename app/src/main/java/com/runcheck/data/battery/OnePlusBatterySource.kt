package com.runcheck.data.battery

import android.content.Context
import android.os.BatteryManager
import com.runcheck.data.device.DeviceProfile
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MeasuredValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.abs

class OnePlusBatterySource(
    context: Context,
    profile: DeviceProfile
) : GenericBatterySource(context, profile) {

    override fun getCurrentNow(): Flow<MeasuredValue<Int>> = flow {
        while (true) {
            val rawCurrent = readCurrentNowRaw()
            if (rawCurrent == null) {
                emit(unavailableCurrent())
                delay(POLLING_INTERVAL_MS)
                continue
            }

            val profileNormalizedMa = normalizeCurrent(rawCurrent)
            val isCharging = batteryManager.isCharging
            val currentMa = when {
                isCharging && profileNormalizedMa < 0 -> abs(profileNormalizedMa)
                !isCharging && profileNormalizedMa > 0 -> -abs(profileNormalizedMa)
                else -> profileNormalizedMa
            }

            val confidence = when {
                rawCurrent == 0 -> Confidence.UNAVAILABLE
                profile.currentNowReliable -> Confidence.HIGH
                else -> Confidence.LOW
            }
            emit(MeasuredValue(currentMa, confidence))
            delay(POLLING_INTERVAL_MS)
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val POLLING_INTERVAL_MS = 2000L
    }
}
