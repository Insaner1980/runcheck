package com.runcheck.data.battery

import android.content.Context
import com.runcheck.data.device.DeviceProfile
import com.runcheck.domain.model.MeasuredValue
import kotlinx.coroutines.flow.Flow

class SamsungBatterySource(
    context: Context,
    profile: DeviceProfile,
) : GenericBatterySource(context, profile) {
    override fun getCurrentNow(): Flow<MeasuredValue<Int>> =
        samsungCurrentNowFlow(
            stableReadingThreshold = STABLE_READING_THRESHOLD,
            suspiciousConstantCurrentMa = SUSPICIOUS_CONSTANT_CURRENT_MA,
        )

    companion object {
        private const val STABLE_READING_THRESHOLD = 3
        private const val SUSPICIOUS_CONSTANT_CURRENT_MA = 3000
    }
}
