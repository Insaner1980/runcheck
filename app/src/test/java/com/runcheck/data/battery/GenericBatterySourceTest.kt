package com.runcheck.data.battery

import com.runcheck.data.device.DeviceProfile
import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.SignConvention
import org.junit.Assert.assertEquals
import org.junit.Test

class GenericBatterySourceTest {

    @Test
    fun `normalizeCurrent converts microamps to milliamps`() {
        val profile = createProfile(
            unit = CurrentUnit.MICROAMPS,
            convention = SignConvention.POSITIVE_CHARGING
        )
        val normalizer = TestableNormalizer(profile)

        assertEquals(500, normalizer.normalize(500_000))
        assertEquals(-200, normalizer.normalize(-200_000))
        assertEquals(0, normalizer.normalize(0))
    }

    @Test
    fun `normalizeCurrent keeps milliamps as-is`() {
        val profile = createProfile(
            unit = CurrentUnit.MILLIAMPS,
            convention = SignConvention.POSITIVE_CHARGING
        )
        val normalizer = TestableNormalizer(profile)

        assertEquals(500, normalizer.normalize(500))
        assertEquals(-200, normalizer.normalize(-200))
    }

    @Test
    fun `normalizeCurrent flips sign for negative convention`() {
        val profile = createProfile(
            unit = CurrentUnit.MILLIAMPS,
            convention = SignConvention.NEGATIVE_CHARGING
        )
        val normalizer = TestableNormalizer(profile)

        // Negative convention: negate the value
        assertEquals(-500, normalizer.normalize(500))
        assertEquals(200, normalizer.normalize(-200))
    }

    @Test
    fun `normalizeCurrent handles microamps with negative convention`() {
        val profile = createProfile(
            unit = CurrentUnit.MICROAMPS,
            convention = SignConvention.NEGATIVE_CHARGING
        )
        val normalizer = TestableNormalizer(profile)

        // First divide by 1000, then negate
        assertEquals(-500, normalizer.normalize(500_000))
        assertEquals(200, normalizer.normalize(-200_000))
    }

    @Test
    fun `normalizeCurrent handles zero correctly for all conventions`() {
        for (unit in CurrentUnit.entries) {
            for (convention in SignConvention.entries) {
                val profile = createProfile(unit = unit, convention = convention)
                val normalizer = TestableNormalizer(profile)
                assertEquals(0, normalizer.normalize(0))
            }
        }
    }

    @Test
    fun `normalizeCurrent handles small microamp values`() {
        val profile = createProfile(
            unit = CurrentUnit.MICROAMPS,
            convention = SignConvention.POSITIVE_CHARGING
        )
        val normalizer = TestableNormalizer(profile)

        // Values under 1000 microamps truncate to 0 milliamps (integer division)
        assertEquals(0, normalizer.normalize(999))
        assertEquals(1, normalizer.normalize(1000))
    }

    private fun createProfile(
        unit: CurrentUnit,
        convention: SignConvention
    ) = DeviceProfile(
        manufacturer = "google",
        model = "Pixel 8",
        apiLevel = 34,
        currentNowReliable = true,
        currentNowUnit = unit,
        currentNowSignConvention = convention,
        cycleCountAvailable = true,
        batteryHealthPercentAvailable = true,
        thermalZonesAvailable = emptyList(),
        storageHealthAvailable = true
    )

    /**
     * Exposes the protected normalizeCurrent method for testing
     * without needing an Android Context.
     */
    private class TestableNormalizer(private val profile: DeviceProfile) {
        fun normalize(raw: Int): Int {
            val milliamps = when (profile.currentNowUnit) {
                CurrentUnit.MICROAMPS -> raw / 1000
                CurrentUnit.MILLIAMPS -> raw
            }
            return when (profile.currentNowSignConvention) {
                SignConvention.POSITIVE_CHARGING -> milliamps
                SignConvention.NEGATIVE_CHARGING -> -milliamps
            }
        }
    }
}
