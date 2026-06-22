package com.runcheck.data.battery

import android.content.Context
import android.os.BatteryManager
import com.runcheck.data.device.DeviceProfile
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.SignConvention
import com.runcheck.util.AppDispatchers
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class GenericBatterySourceTest {
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
    fun `normalizeCurrent keeps milliamps as-is`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MILLIAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
            )

        assertEquals(500, source.testNormalizeCurrent(500))
        assertEquals(-200, source.testNormalizeCurrent(-200))
    }

    @Test
    fun `normalizeCurrent flips sign for negative convention`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MILLIAMPS,
                convention = SignConvention.NEGATIVE_CHARGING,
            )

        // Negative convention: negate the value
        assertEquals(-500, source.testNormalizeCurrent(500))
        assertEquals(200, source.testNormalizeCurrent(-200))
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
    fun `normalizeCurrent falls back to runtime microamp heuristic for large readings`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MILLIAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
            )

        assertEquals(5000, source.testNormalizeCurrent(5_000_000))
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
    fun `battery intent integer mappings fall back safely for unknown values`() {
        val source =
            createTestSource(
                unit = CurrentUnit.MILLIAMPS,
                convention = SignConvention.POSITIVE_CHARGING,
            )

        assertEquals(BatteryHealth.GOOD, source.testMapHealth(BatteryManager.BATTERY_HEALTH_GOOD))
        assertEquals(BatteryHealth.OVERHEAT, source.testMapHealth(BatteryManager.BATTERY_HEALTH_OVERHEAT))
        assertEquals(BatteryHealth.UNKNOWN, source.testMapHealth(-1))

        assertEquals(
            ChargingStatus.CHARGING,
            source.testMapChargingStatus(BatteryManager.BATTERY_STATUS_CHARGING),
        )
        assertEquals(
            ChargingStatus.DISCHARGING,
            source.testMapChargingStatus(BatteryManager.BATTERY_STATUS_DISCHARGING),
        )
        assertEquals(ChargingStatus.NOT_CHARGING, source.testMapChargingStatus(-1))

        assertEquals(PlugType.AC, source.testMapPlugType(BatteryManager.BATTERY_PLUGGED_AC))
        assertEquals(PlugType.USB, source.testMapPlugType(BatteryManager.BATTERY_PLUGGED_USB))
        assertEquals(PlugType.WIRELESS, source.testMapPlugType(BatteryManager.BATTERY_PLUGGED_WIRELESS))
        assertEquals(PlugType.NONE, source.testMapPlugType(-1))
    }

    private fun createTestSource(
        unit: CurrentUnit,
        convention: SignConvention,
        reliable: Boolean = true,
        isCharging: Boolean = false,
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
            }
        val mockContext: Context =
            mockk {
                every { getSystemService(Context.BATTERY_SERVICE) } returns batteryManager
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
        fun testNormalizeCurrent(raw: Int): Int = normalizeCurrent(raw)

        fun testCalculateCurrentConfidence(raw: Int): Confidence = calculateCurrentConfidence(raw)

        fun testAlignCurrentSignWithChargeState(currentMa: Int): Int = alignCurrentSignWithChargeState(currentMa)

        fun testMapHealth(health: Int) = mapHealth(health)

        fun testMapChargingStatus(status: Int) = mapChargingStatus(status)

        fun testMapPlugType(plugged: Int) = mapPlugType(plugged)
    }
}
