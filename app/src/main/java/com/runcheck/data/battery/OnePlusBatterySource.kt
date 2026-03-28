package com.runcheck.data.battery

import android.content.Context
import com.runcheck.data.device.DeviceProfile
import com.runcheck.domain.model.MeasuredValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class OnePlusBatterySource(
    context: Context,
    profile: DeviceProfile,
) : GenericBatterySource(context, profile) {
    override fun getCurrentNow(): Flow<MeasuredValue<Int>> =
        flow {
            while (true) {
                val rawCurrent = readCurrentNowRaw()
                if (rawCurrent == null) {
                    emit(unavailableCurrent())
                    delay(POLLING_INTERVAL_MS)
                    continue
                }

                val currentMa = alignCurrentSignWithChargeState(normalizeCurrent(rawCurrent))
                val confidence = calculateCurrentConfidence(rawCurrent)
                emit(MeasuredValue(currentMa, confidence))
                delay(POLLING_INTERVAL_MS)
            }
        }.flowOn(Dispatchers.IO)

    companion object {
        private const val POLLING_INTERVAL_MS = 2000L
    }
}
