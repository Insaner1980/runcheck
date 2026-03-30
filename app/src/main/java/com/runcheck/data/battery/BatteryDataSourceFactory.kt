package com.runcheck.data.battery

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.runcheck.data.device.DeviceProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryDataSourceFactory
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private var cachedSource: BatteryDataSource? = null
        private var cachedProfileKey: String? = null

        @Synchronized
        fun create(profile: DeviceProfile): BatteryDataSource {
            val key = "${profile.manufacturer}_${profile.apiLevel}"
            cachedSource?.let { source ->
                if (cachedProfileKey == key) return source
                (source as? GenericBatterySource)?.close()
            }

            val isApi34Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            val source =
                when {
                    profile.manufacturer == "samsung" -> {
                        if (isApi34Plus) {
                            createSamsungApi34Source(profile)
                        } else {
                            SamsungBatterySource(context, profile)
                        }
                    }

                    profile.manufacturer == "oneplus" -> {
                        if (isApi34Plus) {
                            createOnePlusApi34Source(profile)
                        } else {
                            OnePlusBatterySource(context, profile)
                        }
                    }

                    isApi34Plus -> {
                        createGenericApi34Source(profile)
                    }

                    else -> {
                        GenericBatterySource(context, profile)
                    }
                }
            cachedSource = source
            cachedProfileKey = key
            return source
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private fun createSamsungApi34Source(profile: DeviceProfile): BatteryDataSource =
            SamsungAndroid14BatterySource(context, profile)

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private fun createOnePlusApi34Source(profile: DeviceProfile): BatteryDataSource =
            OnePlusAndroid14BatterySource(context, profile)

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private fun createGenericApi34Source(profile: DeviceProfile): BatteryDataSource =
            Android14BatterySource(context, profile)
    }
