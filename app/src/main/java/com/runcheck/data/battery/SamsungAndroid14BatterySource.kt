package com.runcheck.data.battery

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.runcheck.data.device.DeviceProfile
import com.runcheck.util.AppDispatchers

/**
 * Samsung on API 34+: inherits cycle count / health % from [Android14BatterySource]
 * while keeping Samsung's max-theoretical-current detection in [getCurrentNow].
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class SamsungAndroid14BatterySource(
    context: Context,
    profile: DeviceProfile,
    dispatchers: AppDispatchers,
) : Android14BatterySource(context, profile, dispatchers) {
    override fun getCurrentNow() = samsungCurrentNowFlow()
}
