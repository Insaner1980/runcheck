package com.runcheck.data.battery

import android.content.Context
import android.os.BatteryManager
import com.runcheck.data.device.DeviceProfile
import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.SignConvention
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class GenericBatterySourceTest {

    @Test
    fun `normalizeCurrent converts microamps to milliamps`() {
        val source = createTestSource(
            unit = CurrentUnit.MICROAMPS,
            convention = SignConvention.POSITIVE_CHARGING
        )

        assertEquals(500, source.testNormalizeCurrent(500_000))
        assertEquals(-200, source.testNormalizeCurrent(-200_000))
        assertEquals(0, source.testNormalizeCurrent(0))
    }

    @Test
    fun `normalizeCurrent keeps milliamps as-is`() {
        val source = createTestSource(
            unit = CurrentUnit.MILLIAMPS,
            convention = SignConvention.POSITIVE_CHARGING
        )

        assertEquals(500, source.testNormalizeCurrent(500))
        assertEquals(-200, source.testNormalizeCurrent(-200))
    }

    @Test
    fun `normalizeCurrent flips sign for negative convention`() {
        val source = createTestSource(
            unit = CurrentUnit.MILLIAMPS,
            convention = SignConvention.NEGATIVE_CHARGING
        )

        // Negative convention: negate the value
        assertEquals(-500, source.testNormalizeCurrent(500))
        assertEquals(200, source.testNormalizeCurrent(-200))
    }

    @Test
    fun `normalizeCurrent handles microamps with negative convention`() {
        val source = createTestSource(
            unit = CurrentUnit.MICROAMPS,
            convention = SignConvention.NEGATIVE_CHARGING
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
        val source = createTestSource(
            unit = CurrentUnit.MICROAMPS,
            convention = SignConvention.POSITIVE_CHARGING
        )

        // Values under 1000 microamps truncate to 0 milliamps (integer division)
        assertEquals(0, source.testNormalizeCurrent(999))
        assertEquals(1, source.testNormalizeCurrent(1000))
    }

    @Test
    fun `normalizeCurrent falls back to runtime microamp heuristic for large readings`() {
        val source = createTestSource(
            unit = CurrentUnit.MILLIAMPS,
            convention = SignConvention.POSITIVE_CHARGING
        )

        assertEquals(5000, source.testNormalizeCurrent(5_000_000))
    }

    @Test
    fun `nonzero unreliable current is surfaced as low confidence instead of unavailable`() {
        val source = createTestSource(
            unit = CurrentUnit.MILLIAMPS,
            convention = SignConvention.POSITIVE_CHARGING,
            reliable = false
        )

        assertEquals(Confidence.LOW, source.testCalculateCurrentConfidence(1234))
        assertEquals(Confidence.UNAVAILABLE, source.testCalculateCurrentConfidence(0))
    }

    private fun createTestSource(
        unit: CurrentUnit,
        convention: SignConvention,
        reliable: Boolean = true
    ): TestableGenericBatterySource {
        val profile = DeviceProfile(
            manufacturer = "google",
            model = "Pixel 8",
            apiLevel = 34,
            currentNowReliable = reliable,
            currentNowUnit = unit,
            currentNowSignConvention = convention,
            cycleCountAvailable = true,
            thermalZonesAvailable = emptyList(),
            storageHealthAvailable = true
        )
        val mockContext: Context = mockk {
            every { getSystemService(Context.BATTERY_SERVICE) } returns mockk<BatteryManager>(relaxed = true)
        }
        return TestableGenericBatterySource(mockContext, profile)
    }

    /**
     * Minimal subclass that exposes the real protected normalizeCurrent()
     * method for testing. Does NOT reimplement any logic.
     */
    private class TestableGenericBatterySource(
        context: Context,
        profile: DeviceProfile
    ) : GenericBatterySource(context, profile) {

        fun testNormalizeCurrent(raw: Int): Int = normalizeCurrent(raw)
        fun testCalculateCurrentConfidence(raw: Int): Confidence = calculateCurrentConfidence(raw)
    }
}
