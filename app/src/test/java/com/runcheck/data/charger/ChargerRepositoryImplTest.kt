package com.runcheck.data.charger

import com.runcheck.data.db.dao.ChargerDao
import com.runcheck.data.db.entity.ChargerProfileEntity
import com.runcheck.data.db.entity.ChargingSessionEntity
import com.runcheck.domain.model.ChargerProfile
import com.runcheck.domain.model.ChargingSession
import com.runcheck.util.TestAppDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargerRepositoryImplTest {
    private val chargerDao: ChargerDao = mockk(relaxed = true)

    @Test
    fun `profile flow and sync calls map entities to domain models`() =
        runTest {
            val repository = repository()
            val entity = ChargerProfileEntity(id = 5L, name = "USB-C", created = 1_234L)
            val expected = listOf(ChargerProfile(id = 5L, name = "USB-C", created = 1_234L))
            every { chargerDao.getChargerProfiles() } returns flowOf(listOf(entity))
            coEvery { chargerDao.getChargerProfilesSync() } returns listOf(entity)

            assertEquals(expected, repository.getChargerProfiles().first())
            assertEquals(expected, repository.getChargerProfilesSync())
        }

    @Test
    fun `session flow sync and active calls map entities to domain models`() =
        runTest {
            val repository = repository()
            val entity = sessionEntity()
            val expected = sessionDomain()
            every { chargerDao.getAllSessions() } returns flowOf(listOf(entity))
            coEvery { chargerDao.getAllSessionsSync() } returns listOf(entity)
            coEvery { chargerDao.getActiveSession() } returns entity

            assertEquals(listOf(expected), repository.getAllSessions().first())
            assertEquals(listOf(expected), repository.getAllSessionsSync())
            assertEquals(expected, repository.getActiveSession())
        }

    @Test
    fun `insert charger trims name and forwards generated id`() =
        runTest {
            val repository = repository()
            val inserted = slot<ChargerProfileEntity>()
            coEvery { chargerDao.insertCharger(capture(inserted)) } returns 9L

            assertEquals(9L, repository.insertCharger("  Wall charger  "))
            assertEquals("Wall charger", inserted.captured.name)
            assertTrue(inserted.captured.created > 0L)
        }

    @Test
    fun `write methods convert sessions and delegate to dao`() =
        runTest {
            val repository = repository()
            val inserted = slot<ChargingSessionEntity>()
            coEvery { chargerDao.insertSession(capture(inserted)) } returns 12L

            assertEquals(12L, repository.insertSession(sessionDomain()))
            assertEquals(sessionEntity(), inserted.captured)

            repository.completeSession(
                id = 8L,
                endTime = 1_200L,
                endLevel = 95,
                avgCurrentMa = 450,
                maxCurrentMa = 900,
                avgVoltageMv = 4_200,
                avgPowerMw = 3_780,
            )
            repository.deleteChargerById(5L)
            repository.deleteSessionsOlderThan(99L)

            coVerify(exactly = 1) {
                chargerDao.completeSession(
                    id = 8L,
                    endTime = 1_200L,
                    endLevel = 95,
                    avgCurrentMa = 450,
                    maxCurrentMa = 900,
                    avgVoltageMv = 4_200,
                    avgPowerMw = 3_780,
                )
            }
            coVerify(exactly = 1) { chargerDao.deleteChargerById(5L) }
            coVerify(exactly = 1) { chargerDao.deleteSessionsOlderThan(99L) }
        }

    private fun repository(): ChargerRepositoryImpl =
        ChargerRepositoryImpl(
            chargerDao = chargerDao,
            dispatchers = TestAppDispatchers(),
        )

    private fun sessionEntity(): ChargingSessionEntity =
        ChargingSessionEntity(
            id = 8L,
            chargerId = 5L,
            startTime = 1_000L,
            endTime = 1_200L,
            startLevel = 40,
            endLevel = 95,
            avgCurrentMa = 450,
            maxCurrentMa = 900,
            avgVoltageMv = 4_200,
            avgPowerMw = 3_780,
            plugType = "USB",
        )

    private fun sessionDomain(): ChargingSession =
        ChargingSession(
            id = 8L,
            chargerId = 5L,
            startTime = 1_000L,
            endTime = 1_200L,
            startLevel = 40,
            endLevel = 95,
            avgCurrentMa = 450,
            maxCurrentMa = 900,
            avgVoltageMv = 4_200,
            avgPowerMw = 3_780,
            plugType = "USB",
        )
}
