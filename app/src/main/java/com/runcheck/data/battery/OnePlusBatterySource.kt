package com.runcheck.data.battery

import android.content.Context
import com.runcheck.data.device.DeviceProfile
import com.runcheck.util.AppDispatchers

class OnePlusBatterySource(
    context: Context,
    profile: DeviceProfile,
    dispatchers: AppDispatchers,
) : GenericBatterySource(context, profile, dispatchers)
