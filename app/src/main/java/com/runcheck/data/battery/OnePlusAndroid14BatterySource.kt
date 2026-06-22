package com.runcheck.data.battery

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.runcheck.data.device.DeviceProfile
import com.runcheck.util.AppDispatchers

/**
 * OnePlus on API 34+: inherits cycle count / health % from [Android14BatterySource]
 * while keeping profile-based OnePlus SUPERVOOC current sign correction.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class OnePlusAndroid14BatterySource(
    context: Context,
    profile: DeviceProfile,
    dispatchers: AppDispatchers,
) : Android14BatterySource(context, profile, dispatchers)
