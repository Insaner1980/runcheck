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
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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
            coVerify(exactly = 0) { capabilityManager.detectCapabilities() }
            verifyNoWrites()
        }

    @Test
    fun `stored profile missing optional persisted json fields uses safe defaults`() =
        runTest {
            val profileJson =
                """
                {
                  "manufacturer": "Google",
                  "model": "Pixel 8",
                  "apiLevel": 34,
                  "currentNowReliable": true,
                  "currentNowUnit": "MICROAMPS",
                  "currentNowSignConvention": "POSITIVE_CHARGING",
                  "cycleCountAvailable": true
                }
                """.trimIndent()
            val entity =
                DeviceEntity(
                    id = "google_pixel 8_34",
                    manufacturer = "Google",
                    model = "Pixel 8",
                    apiLevel = 34,
                    firstSeen = 1_000L,
                    profileJson = profileJson,
                )
            every { deviceDao.getDevice() } returns flowOf(entity)
            coEvery { deviceDao.getDeviceSync() } returns entity

            val expected = profileInfo(apiLevel = 34, thermalZonesAvailable = emptyList())

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
            coVerifySequence {
                capabilityManager.detectCapabilities()
                deviceDao.getDeviceSync()
                deviceDao.replaceCurrent(capture(inserted))
            }
            assertEquals(deviceEntity(detected, firstSeen = 123L), inserted.captured)
            assertEquals(detected, gson.fromJson(inserted.captured.profileJson, DeviceProfile::class.java))
        }

    @Test
    fun `refreshProfile retains firstSeen for same identity`() =
        runTest {
            val detected = deviceProfile(apiLevel = 34)
            coEvery { capabilityManager.detectCapabilities() } returns detected
            coEvery { deviceDao.getDeviceSync() } returns
                deviceEntity(detected.copy(currentNowReliable = false), firstSeen = 456L)

            assertEquals(profileInfo(apiLevel = 34), repository.refreshProfile())

            coVerifySequence {
                capabilityManager.detectCapabilities()
                deviceDao.getDeviceSync()
                deviceDao.replaceCurrent(deviceEntity(detected, firstSeen = 456L))
            }
        }

    @Test
    fun `refreshProfile uses current time when table is empty`() =
        runTest {
            val detected = deviceProfile(apiLevel = 34)
            val inserted = slot<DeviceEntity>()
            coEvery { capabilityManager.detectCapabilities() } returns detected
            coEvery { deviceDao.getDeviceSync() } returns null

            val before = System.currentTimeMillis()
            assertEquals(profileInfo(apiLevel = 34), repository.refreshProfile())
            val after = System.currentTimeMillis()

            coVerifySequence {
                capabilityManager.detectCapabilities()
                deviceDao.getDeviceSync()
                deviceDao.replaceCurrent(capture(inserted))
            }
            assertTrue(inserted.captured.firstSeen in before..after)
            assertEquals(deviceEntity(detected, inserted.captured.firstSeen), inserted.captured)
        }

    @Test
    fun `stale and missing profiles inherit firstSeen from second read`() =
        runTest {
            val stale = deviceEntity(deviceProfile(apiLevel = Build.VERSION.SDK_INT + 1), firstSeen = 111L)
            for (initial in listOf(stale, null)) {
                val dao = mockk<DeviceDao>(relaxed = true)
                val detector = mockk<DeviceCapabilityManager>()
                val subject = DeviceProfileRepositoryImpl(dao, detector, gson)
                val detected = deviceProfile(apiLevel = Build.VERSION.SDK_INT)
                val second = deviceEntity(deviceProfile(apiLevel = 34), firstSeen = 222L)
                coEvery { dao.getDeviceSync() } returnsMany listOf(initial, second)
                coEvery { detector.detectCapabilities() } returns detected

                assertSame(detected, subject.getDeviceProfile())

                coVerifySequence {
                    dao.getDeviceSync()
                    detector.detectCapabilities()
                    dao.getDeviceSync()
                    dao.replaceCurrent(deviceEntity(detected, firstSeen = 222L))
                }
            }
        }

    @Test
    fun `refreshProfile propagates replacement failure`() =
        runTest {
            val failure = IllegalStateException("replacement failed")
            coEvery { capabilityManager.detectCapabilities() } returns deviceProfile(apiLevel = 34)
            coEvery { deviceDao.getDeviceSync() } returns null
            coEvery { deviceDao.replaceCurrent(any()) } throws failure

            assertSame(failure, runCatching { repository.refreshProfile() }.exceptionOrNull())
            coVerify(exactly = 1) { deviceDao.replaceCurrent(any()) }
        }

    @Test
    fun `getDeviceProfile returns stored profile when api level matches runtime`() =
        runTest {
            val stored = deviceProfile(apiLevel = Build.VERSION.SDK_INT)
            coEvery { deviceDao.getDeviceSync() } returns
                deviceEntity(profile = stored).copy(apiLevel = Build.VERSION.SDK_INT + 1)

            assertEquals(stored, repository.getDeviceProfile())
            coVerify(exactly = 0) { capabilityManager.detectCapabilities() }
            coVerify(exactly = 1) { deviceDao.getDeviceSync() }
            verifyNoWrites()
        }

    private fun verifyNoWrites() {
        coVerify(exactly = 0) {
            deviceDao.replaceCurrent(any())
            deviceDao.insertOrUpdate(any())
            deviceDao.deleteAllExcept(any())
        }
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
        thermalZonesAvailable: List<String> = listOf("battery"),
    ): DeviceProfileInfo =
        DeviceProfileInfo(
            manufacturer = manufacturer,
            model = model,
            apiLevel = apiLevel,
            currentNowReliable = true,
            currentNowUnit = CurrentUnit.MICROAMPS,
            currentNowSignConvention = SignConvention.POSITIVE_CHARGING,
            cycleCountAvailable = apiLevel >= 34,
            thermalZonesAvailable = thermalZonesAvailable,
            storageHealthAvailable = true,
        )
}
