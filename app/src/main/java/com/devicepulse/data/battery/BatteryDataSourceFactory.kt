package com.devicepulse.data.battery

import android.content.Context
import android.os.Build
import com.devicepulse.data.device.DeviceProfile

class BatteryDataSourceFactory(
    private val context: Context
) {
    fun create(profile: DeviceProfile): BatteryDataSource = when {
        profile.apiLevel >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            Android14BatterySource(context, profile)
        profile.manufacturer == "samsung" ->
            SamsungBatterySource(context, profile)
        profile.manufacturer == "oneplus" ->
            OnePlusBatterySource(context, profile)
        else ->
            GenericBatterySource(context, profile)
    }
}
