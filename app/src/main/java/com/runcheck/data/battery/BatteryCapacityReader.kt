package com.runcheck.data.battery

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryCapacityReader @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /**
     * Returns the device's design battery capacity in mAh using Android's
     * internal PowerProfile API via reflection. Returns null if unavailable.
     */
    fun getDesignCapacityMah(): Int? = try {
        val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
        val constructor = powerProfileClass.getConstructor(Context::class.java)
        val instance = constructor.newInstance(context)
        val capacity = powerProfileClass
            .getMethod("getBatteryCapacity")
            .invoke(instance) as Double
        if (capacity > 0) capacity.toInt() else null
    } catch (_: Exception) {
        null
    }
}
