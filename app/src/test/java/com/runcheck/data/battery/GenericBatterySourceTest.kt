package com.runcheck.data.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.runcheck.data.device.DeviceProfile
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.SignConvention
import com.runcheck.util.AppDispatchers
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GenericBatterySourceTest {
    @Test
    fun `battery level normalization stays in percentage range without integer overflow`() {
        val source = createTestSource(CurrentUnit.MICROAMPS, SignConvention.POSITIVE_CHARGING)

        assertEquals(0, source.testNormalizeLevel(0, 100))
        assertEquals(50, source.testNormalizeLevel(100, 200))
        assertEquals(0, source.testNormalizeLevel(-1, 100))
        assertEquals(100, source.testNormalizeLevel(101, 100))
        assertEquals(0, source.testNormalizeLevel(50, 0))
        assertEquals(0, source.testNormalizeLevel(50, -1))
        assertEquals(100, source.testNormalizeLevel(Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test
    fun `normalizeCurrent converts microamps to milliamps`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MICROAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
            )

        assertEquals(500, source.testNormalizeCurrent(500_000))
        assertEquals(-200, source.testNormalizeCurrent(-200_000))
        assertEquals(0, source.testNormalizeCurrent(0))
    }

    @Test
    fun `normalizeCurrent uses microamps when a legacy profile says milliamps`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MILLIAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
            )

        assertEquals(500, source.testNormalizeCurrent(500_000))
        assertEquals(-200, source.testNormalizeCurrent(-200_000))
    }

    @Test
    fun `normalizeCurrent flips sign for negative convention`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MILLIAMPS,
                convention = SignConvention.NEGATIVE_CHARGING,
            )

        // Negative convention: negate the value
        assertEquals(-500, source.testNormalizeCurrent(500_000))
        assertEquals(200, source.testNormalizeCurrent(-200_000))
    }

    @Test
    fun `normalizeCurrent handles microamps with negative convention`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MICROAMPS,
                convention = SignConvention.NEGATIVE_CHARGING,
            )

        // First divide by 1000, then negate
        assertEquals(-500, source.testNormalizeCurrent(500_000))
        assertEquals(200, source.testNormalizeCurrent(-200_000))
    }

    @Test
    fun `normalizeCurrent handles zero correctly for all conventions`() {
        for (unit in CurrentUnit.entries) {
            for (convention in SignConvention.entries) {
                val source = createTestSource(unit = unit, convention = convention)
                assertEquals(0, source.testNormalizeCurrent(0))
            }
        }
    }

    @Test
    fun `normalizeCurrent handles small microamp values`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MICROAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
            )

        // Values under 1000 microamps truncate to 0 milliamps (integer division)
        assertEquals(0, source.testNormalizeCurrent(999))
        assertEquals(1, source.testNormalizeCurrent(1000))
    }

    @Test
    fun `normalizeCurrent uses microamps for small readings from a legacy milliamps profile`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MILLIAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
            )

        assertEquals(10, source.testNormalizeCurrent(10_000))
        assertEquals(-10, source.testNormalizeCurrent(-10_000))
    }

    @Test
    fun `nonzero unreliable current is surfaced as low confidence instead of unavailable`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MILLIAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
                reliable = false,
            )

        assertEquals(Confidence.LOW, source.testCalculateCurrentConfidence(1234))
        assertEquals(Confidence.UNAVAILABLE, source.testCalculateCurrentConfidence(0))
    }

    @Test
    fun `current confidence rejects readings outside the plausible milliamp range`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MICROAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
            )

        assertEquals(Confidence.HIGH, source.testCalculateCurrentConfidence(10_000_000))
        assertEquals(Confidence.HIGH, source.testCalculateCurrentConfidence(-10_000_000))
        assertEquals(Confidence.UNAVAILABLE, source.testCalculateCurrentConfidence(10_001_000))
        assertEquals(Confidence.UNAVAILABLE, source.testCalculateCurrentConfidence(-10_001_000))
    }

    @Test
    fun `alignCurrentSignWithChargeState corrects signs that disagree with charge state`() {
        val chargingSource =
            createTestSource(
                unit = CurrentUnit.MILLIAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
                isCharging = true,
            )
        val dischargingSource =
            createTestSource(
                unit = CurrentUnit.MILLIAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
                isCharging = false,
            )

        assertEquals(500, chargingSource.testAlignCurrentSignWithChargeState(-500))
        assertEquals(500, chargingSource.testAlignCurrentSignWithChargeState(500))
        assertEquals(-500, dischargingSource.testAlignCurrentSignWithChargeState(500))
        assertEquals(-500, dischargingSource.testAlignCurrentSignWithChargeState(-500))
    }

    @Test
    fun `battery health and plug mappings fall back safely for unknown values`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MILLIAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
            )

        assertEquals(BatteryHealth.GOOD, source.testMapHealth(BatteryManager.BATTERY_HEALTH_GOOD))
        assertEquals(BatteryHealth.OVERHEAT, source.testMapHealth(BatteryManager.BATTERY_HEALTH_OVERHEAT))
        assertEquals(BatteryHealth.UNKNOWN, source.testMapHealth(-1))

        assertEquals(PlugType.AC, source.testMapPlugType(BatteryManager.BATTERY_PLUGGED_AC))
        assertEquals(PlugType.USB, source.testMapPlugType(BatteryManager.BATTERY_PLUGGED_USB))
        assertEquals(PlugType.WIRELESS, source.testMapPlugType(BatteryManager.BATTERY_PLUGGED_WIRELESS))
        assertEquals(PlugType.NONE, source.testMapPlugType(-1))
    }

    @Test
    fun `charging status flow maps full battery intent`() =
        runTest {
            val batteryIntent: Intent =
                mockk {
                    every { getIntExtra(BatteryManager.EXTRA_STATUS, 0) } returns BatteryManager.BATTERY_STATUS_FULL
                }
            mockkStatic(ContextCompat::class)
            every {
                ContextCompat.registerReceiver(
                    any(),
                    any<BroadcastReceiver>(),
                    any<IntentFilter>(),
                    ContextCompat.RECEIVER_NOT_EXPORTED,
                )
            } answers {
                arg<BroadcastReceiver>(1).onReceive(mockk(relaxed = true), batteryIntent)
                batteryIntent
            }
            val source =
                createTestSource(
                    unit = CurrentUnit.MILLIAMPS,
                    convention = SignConvention.POSITIVE_CHARGING,
                )

            try {
                assertEquals(ChargingStatus.FULL, source.getChargingStatus().first())
            } finally {
                source.close()
                unmockkStatic(ContextCompat::class)
            }
        }

    @Test
    fun `charge counter emits only positive values`() =
        runTest {
            val positiveSource =
                createTestSource(
                    unit = CurrentUnit.MICROAMPS,
                    convention = SignConvention.POSITIVE_CHARGING,
                    chargeCounterRaw = 1_234_000,
                )
            val negativeSource =
                createTestSource(
                    unit = CurrentUnit.MICROAMPS,
                    convention = SignConvention.POSITIVE_CHARGING,
                    chargeCounterRaw = -1_000,
                )

            assertEquals(1_234, positiveSource.getChargeCounter().first())
            assertEquals(null, negativeSource.getChargeCounter().first())
        }

    @Test
    fun `current flow preserves raw zero truncation confidence and charge state alignment`() =
        runTest {
            val cases =
                listOf(
                    Triple(0, 0, Confidence.UNAVAILABLE),
                    Triple(999, 0, Confidence.HIGH),
                    Triple(-999, 0, Confidence.HIGH),
                    Triple(500_999, 500, Confidence.HIGH),
                    Triple(-500_999, 500, Confidence.HIGH),
                    Triple(10_000_000, 10_000, Confidence.HIGH),
                    Triple(-10_000_000, 10_000, Confidence.HIGH),
                    Triple(10_000_999, 10_000, Confidence.HIGH),
                    Triple(-10_000_999, 10_000, Confidence.HIGH),
                    Triple(10_001_000, 10_001, Confidence.UNAVAILABLE),
                    Triple(-10_001_000, 10_001, Confidence.UNAVAILABLE),
                    Triple(Int.MIN_VALUE, 0, Confidence.UNAVAILABLE),
                )
            for (reliable in listOf(true, false)) {
                for (isCharging in listOf(true, false)) {
                    for ((raw, magnitude, baseConfidence) in cases) {
                        val source =
                            createTestSource(
                                unit = CurrentUnit.MICROAMPS,
                                convention = SignConvention.POSITIVE_CHARGING,
                                reliable = reliable,
                                isCharging = isCharging,
                                currentNowRaw = raw,
                            )
                        val confidence =
                            if (!reliable && baseConfidence == Confidence.HIGH) Confidence.LOW else baseConfidence
                        try {
                            assertEquals(
                                "raw=$raw reliable=$reliable charging=$isCharging",
                                MeasuredValue(if (isCharging) magnitude else -magnitude, confidence),
                                source.getCurrentNow().first(),
                            )
                        } finally {
                            source.close()
                        }
                    }
                }
            }
        }

    @Test
    fun `current property read failure remains unavailable`() =
        runTest {
            val source =
                createTestSource(
                    unit = CurrentUnit.MICROAMPS,
                    convention = SignConvention.POSITIVE_CHARGING,
                    currentNowError = IllegalStateException("Sensor unavailable"),
                )
            try {
                assertEquals(MeasuredValue(0, Confidence.UNAVAILABLE), source.getCurrentNow().first())
            } finally {
                source.close()
            }
        }

    private fun createTestSource(
        unit: CurrentUnit,
        convention: SignConvention,
        reliable: Boolean = true,
        isCharging: Boolean = false,
        chargeCounterRaw: Int = 0,
        currentNowRaw: Int = 0,
        currentNowError: Exception? = null,
    ): TestableGenericBatterySource {
        val profile =
            DeviceProfile(
                manufacturer = "google",
                model = "Pixel 8",
                apiLevel = 34,
                currentNowReliable = reliable,
                currentNowUnit = unit,
                currentNowSignConvention = convention,
                cycleCountAvailable = true,
                thermalZonesAvailable = emptyList(),
                storageHealthAvailable = true,
            )
        val batteryManager =
            mockk<BatteryManager>(relaxed = true) {
                every { this@mockk.isCharging } returns isCharging
                every { getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) } returns
                    chargeCounterRaw
                every { getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) } answers {
                    currentNowError?.let { throw it }
                    currentNowRaw
                }
            }
        val mockContext: Context =
            mockk {
                every { getSystemService(Context.BATTERY_SERVICE) } returns batteryManager
                every { unregisterReceiver(any()) } returns Unit
            }
        return TestableGenericBatterySource(mockContext, profile, AppDispatchers())
    }

    /**
     * Minimal subclass that exposes the real protected normalizeCurrent()
     * method for testing. Does NOT reimplement any logic.
     */
    private class TestableGenericBatterySource(
        context: Context,
        profile: DeviceProfile,
        dispatchers: AppDispatchers,
    ) : GenericBatterySource(context, profile, dispatchers) {
        fun testNormalizeLevel(
            level: Int,
            scale: Int,
        ): Int = normalizeLevel(level, scale)

        fun testNormalizeCurrent(raw: Int): Int = normalizeCurrent(raw)

        fun testCalculateCurrentConfidence(raw: Int): Confidence = calculateCurrentConfidence(raw)

        fun testAlignCurrentSignWithChargeState(currentMa: Int): Int = alignCurrentSignWithChargeState(currentMa)

        fun testMapHealth(health: Int) = mapHealth(health)

        fun testMapPlugType(plugged: Int) = mapPlugType(plugged)
    }
}
