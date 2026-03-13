package com.devicepulse.data.device

import com.devicepulse.domain.model.CurrentUnit
import com.devicepulse.domain.model.SignConvention
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCapabilityManagerTest {

    @Test
    fun `DeviceProfile deviceId is lowercase combination`() {
        val profile = createProfile(manufacturer = "Samsung", model = "Galaxy S24")
        assertEquals("samsung_galaxy s24_34", profile.deviceId)
    }

    @Test
    fun `microamps unit for high readings`() {
        // Readings above 10000 should be interpreted as microamps
        val profile = createProfile(currentNowUnit = CurrentUnit.MICROAMPS)
        assertEquals(CurrentUnit.MICROAMPS, profile.currentNowUnit)
    }

    @Test
    fun `milliamps unit for low readings`() {
        // Readings below 10000 should be interpreted as milliamps
        val profile = createProfile(currentNowUnit = CurrentUnit.MILLIAMPS)
        assertEquals(CurrentUnit.MILLIAMPS, profile.currentNowUnit)
    }

    @Test
    fun `positive charging convention when positive during charge`() {
        val profile = createProfile(signConvention = SignConvention.POSITIVE_CHARGING)
        assertEquals(SignConvention.POSITIVE_CHARGING, profile.currentNowSignConvention)
    }

    @Test
    fun `negative charging convention when negative during charge`() {
        val profile = createProfile(signConvention = SignConvention.NEGATIVE_CHARGING)
        assertEquals(SignConvention.NEGATIVE_CHARGING, profile.currentNowSignConvention)
    }

    @Test
    fun `cycle count available on API 34+`() {
        val profile34 = createProfile(apiLevel = 34)
        assertTrue(profile34.cycleCountAvailable)

        val profile33 = createProfile(apiLevel = 33)
        assertFalse(profile33.cycleCountAvailable)
    }

    @Test
    fun `battery health percent available on API 34+`() {
        val profile35 = createProfile(apiLevel = 35)
        assertTrue(profile35.batteryHealthPercentAvailable)

        val profile26 = createProfile(apiLevel = 26)
        assertFalse(profile26.batteryHealthPercentAvailable)
    }

    @Test
    fun `thermal zones list stored correctly`() {
        val zones = listOf("cpu-0-0", "cpu-0-1", "battery")
        val profile = createProfile(thermalZones = zones)
        assertEquals(3, profile.thermalZonesAvailable.size)
        assertTrue(profile.thermalZonesAvailable.contains("battery"))
    }

    @Test
    fun `unreliable current marked as such`() {
        val reliable = createProfile(currentNowReliable = true)
        assertTrue(reliable.currentNowReliable)

        val unreliable = createProfile(currentNowReliable = false)
        assertFalse(unreliable.currentNowReliable)
    }

    @Test
    fun `empty thermal zones returns empty list`() {
        val profile = createProfile(thermalZones = emptyList())
        assertTrue(profile.thermalZonesAvailable.isEmpty())
    }

    private fun createProfile(
        manufacturer: String = "google",
        model: String = "Pixel 8",
        apiLevel: Int = 34,
        currentNowReliable: Boolean = true,
        currentNowUnit: CurrentUnit = CurrentUnit.MICROAMPS,
        signConvention: SignConvention = SignConvention.POSITIVE_CHARGING,
        thermalZones: List<String> = listOf("cpu-0-0", "battery")
    ) = DeviceProfile(
        manufacturer = manufacturer,
        model = model,
        apiLevel = apiLevel,
        currentNowReliable = currentNowReliable,
        currentNowUnit = currentNowUnit,
        currentNowSignConvention = signConvention,
        cycleCountAvailable = apiLevel >= 34,
        batteryHealthPercentAvailable = apiLevel >= 34,
        thermalZonesAvailable = thermalZones,
        storageHealthAvailable = true
    )
}
