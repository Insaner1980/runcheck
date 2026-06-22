package com.runcheck.data.device

import android.os.Build
import com.google.gson.Gson
import com.runcheck.data.db.dao.DeviceDao
import com.runcheck.data.db.entity.DeviceEntity
import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.DeviceProfileInfo
import com.runcheck.domain.model.SignConvention
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceProfileRepositoryImplTest {
    private val deviceDao: DeviceDao = mockk(relaxed = true)
    private val capabilityManager: DeviceCapabilityManager = mockk(relaxed = true)
    private val gson = Gson()
    private val repository = DeviceProfileRepositoryImpl(deviceDao, capabilityManager, gson)

    @Test
    fun `stored profile flow and sync calls map json to domain model`() =
        runTest {
            val profile = deviceProfile(apiLevel = 34)
            val entity = deviceEntity(profile = profile)
            every { deviceDao.getDevice() } returns flowOf(entity)
            coEvery { deviceDao.getDeviceSync() } returns entity

            val expected = profileInfo(apiLevel = 34)

            assertEquals(expected, repository.getProfile().first())
            assertEquals(expected, repository.getProfileSync())
        }

    @Test
    fun `refreshProfile persists detected profile and preserves firstSeen`() =
        runTest {
            val detected = deviceProfile(manufacturer = "Samsung", model = "S24", apiLevel = 35)
            val inserted = slot<DeviceEntity>()
            coEvery { capabilityManager.detectCapabilities() } returns detected
            coEvery { deviceDao.getDeviceSync() } returns
                deviceEntity(profile = deviceProfile(apiLevel = 34), firstSeen = 123L)

            val result = repository.refreshProfile()

            assertEquals(profileInfo(manufacturer = "Samsung", model = "S24", apiLevel = 35), result)
            coVerify(exactly = 1) { deviceDao.insertOrUpdate(capture(inserted)) }
            assertEquals("samsung_s24_35", inserted.captured.id)
            assertEquals(123L, inserted.captured.firstSeen)
            assertEquals(detected, gson.fromJson(inserted.captured.profileJson, DeviceProfile::class.java))
            coVerify(exactly = 1) { deviceDao.deleteAllExcept("samsung_s24_35") }
        }

    @Test
    fun `getDeviceProfile returns stored profile when api level matches runtime`() =
        runTest {
            val stored = deviceProfile(apiLevel = Build.VERSION.SDK_INT)
            coEvery { deviceDao.getDeviceSync() } returns deviceEntity(profile = stored)

            assertEquals(stored, repository.getDeviceProfile())
            coVerify(exactly = 0) { capabilityManager.detectCapabilities() }
        }

    private fun deviceEntity(
        profile: DeviceProfile,
        firstSeen: Long = 1_000L,
    ): DeviceEntity =
        DeviceEntity(
            id = profile.deviceId,
            manufacturer = profile.manufacturer,
            model = profile.model,
            apiLevel = profile.apiLevel,
            firstSeen = firstSeen,
            profileJson = gson.toJson(profile),
        )

    private fun deviceProfile(
        manufacturer: String = "Google",
        model: String = "Pixel 8",
        apiLevel: Int,
    ): DeviceProfile =
        DeviceProfile(
            manufacturer = manufacturer,
            model = model,
            apiLevel = apiLevel,
            currentNowReliable = true,
            currentNowUnit = CurrentUnit.MICROAMPS,
            currentNowSignConvention = SignConvention.POSITIVE_CHARGING,
            cycleCountAvailable = apiLevel >= 34,
            thermalZonesAvailable = listOf("battery"),
            storageHealthAvailable = true,
        )

    private fun profileInfo(
        manufacturer: String = "Google",
        model: String = "Pixel 8",
        apiLevel: Int,
    ): DeviceProfileInfo =
        DeviceProfileInfo(
            manufacturer = manufacturer,
            model = model,
            apiLevel = apiLevel,
            currentNowReliable = true,
            currentNowUnit = CurrentUnit.MICROAMPS,
            currentNowSignConvention = SignConvention.POSITIVE_CHARGING,
            cycleCountAvailable = apiLevel >= 34,
            thermalZonesAvailable = listOf("battery"),
            storageHealthAvailable = true,
        )
}
