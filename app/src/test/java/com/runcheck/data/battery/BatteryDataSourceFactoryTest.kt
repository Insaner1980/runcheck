package com.runcheck.data.battery

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.runcheck.data.device.DeviceProfile
import com.runcheck.util.TestAppDispatchers
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BatteryDataSourceFactoryTest(
    private val manufacturer: String,
    private val pre34Class: Class<out BatteryDataSource>,
    private val api34Class: Class<out BatteryDataSource>,
) {
    @Test
    fun `selects exact source using manufacturer and runtime API rather than profile API`() {
        // SDK_INT is final in this JVM environment; only its current branch executes here.
        val isApi34Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        val manager = mockk<BatteryManager>()
        val context = mockk<Context> { every { getSystemService(Context.BATTERY_SERVICE) } returns manager }
        val profile = DeviceProfile(manufacturer = manufacturer, apiLevel = if (isApi34Plus) 33 else 34)
        val source = BatteryDataSourceFactory(context, TestAppDispatchers()).create(profile)
        try {
            assertEquals(if (isApi34Plus) api34Class else pre34Class, source.javaClass)
        } finally {
            (source as GenericBatterySource).close()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "manufacturer={0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("samsung", SamsungBatterySource::class.java, SamsungAndroid14BatterySource::class.java),
                arrayOf("oneplus", OnePlusBatterySource::class.java, OnePlusAndroid14BatterySource::class.java),
                arrayOf("unknown", GenericBatterySource::class.java, Android14BatterySource::class.java),
                arrayOf("Samsung", GenericBatterySource::class.java, Android14BatterySource::class.java),
                arrayOf("OnePlus", GenericBatterySource::class.java, Android14BatterySource::class.java),
            )
    }
}
