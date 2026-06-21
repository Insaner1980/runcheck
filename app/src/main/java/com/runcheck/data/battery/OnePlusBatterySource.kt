package com.runcheck.data.battery

import android.content.Context
import com.runcheck.data.device.DeviceProfile

class OnePlusBatterySource(
    context: Context,
    profile: DeviceProfile,
) : GenericBatterySource(context, profile)
