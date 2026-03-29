package com.runcheck.data.battery

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.runcheck.data.device.DeviceProfile
import com.runcheck.domain.model.MeasuredValue
import kotlinx.coroutines.flow.Flow

/**
 * Samsung on API 34+: inherits cycle count / health % from [Android14BatterySource]
 * while keeping Samsung's max-theoretical-current detection in [getCurrentNow].
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class SamsungAndroid14BatterySource(
    context: Context,
    profile: DeviceProfile,
) : Android14BatterySource(context, profile) {
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
