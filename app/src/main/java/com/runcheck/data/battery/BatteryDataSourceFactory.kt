package com.runcheck.data.battery

import android.content.Context
import android.os.Build
import com.runcheck.data.device.DeviceProfile

class BatteryDataSourceFactory(
    private val context: Context
) {
    fun create(profile: DeviceProfile): BatteryDataSource {
        val isApi34Plus = profile.apiLevel >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        return when {
            // Manufacturer-specific sources take priority for getCurrentNow() quirks.
            // On API 34+ use combined variants that also provide cycle count / health %.
            profile.manufacturer == "samsung" ->
                if (isApi34Plus) SamsungAndroid14BatterySource(context, profile)
                else SamsungBatterySource(context, profile)
            profile.manufacturer == "oneplus" ->
                if (isApi34Plus) OnePlusAndroid14BatterySource(context, profile)
                else OnePlusBatterySource(context, profile)
            isApi34Plus ->
                Android14BatterySource(context, profile)
            else ->
                GenericBatterySource(context, profile)
        }
    }
}
