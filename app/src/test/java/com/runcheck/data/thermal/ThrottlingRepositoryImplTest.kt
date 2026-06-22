package com.runcheck.data.thermal

import com.runcheck.data.db.dao.ThrottlingEventDao
import com.runcheck.data.db.entity.ThrottlingEventEntity
import com.runcheck.domain.model.ThrottlingEvent
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
import org.junit.Test

class ThrottlingRepositoryImplTest {
    private val throttlingEventDao: ThrottlingEventDao = mockk(relaxed = true)
    private val repository = ThrottlingRepositoryImpl(throttlingEventDao, TestAppDispatchers())

    @Test
    fun `recent and sync event queries map entities to domain models`() =
        runTest {
            val entity = throttlingEventEntity()
            val expected = throttlingEvent()
            every { throttlingEventDao.getRecentEvents(5) } returns flowOf(listOf(entity))
            coEvery { throttlingEventDao.getEventsSinceSync(10L) } returns listOf(entity)

            assertEquals(listOf(expected), repository.getRecentEvents(5).first())
            assertEquals(listOf(expected), repository.getEventsSinceSync(10L))
        }

    @Test
    fun `write methods convert and delegate to dao`() =
        runTest {
            val inserted = slot<ThrottlingEventEntity>()
            coEvery { throttlingEventDao.insert(capture(inserted)) } returns 7L

            assertEquals(7L, repository.insert(throttlingEvent()))
            assertEquals(throttlingEventEntity(), inserted.captured)

            repository.updateSnapshot(
                id = 7L,
                thermalStatus = "SEVERE",
                batteryTempC = 43.5f,
                cpuTempC = 55.5f,
                foregroundApp = "Maps",
            )
            repository.updateDuration(id = 7L, durationMs = 12_000L)
            repository.deleteOlderThan(100L)
            repository.deleteAll()

            coVerify(exactly = 1) {
                throttlingEventDao.updateSnapshot(7L, "SEVERE", 43.5f, 55.5f, "Maps")
            }
            coVerify(exactly = 1) { throttlingEventDao.updateDuration(7L, 12_000L) }
            coVerify(exactly = 1) { throttlingEventDao.deleteOlderThan(100L) }
            coVerify(exactly = 1) { throttlingEventDao.deleteAll() }
        }

    private fun throttlingEventEntity(): ThrottlingEventEntity =
        ThrottlingEventEntity(
            id = 7L,
            timestamp = 1_000L,
            thermalStatus = "SEVERE",
            batteryTempC = 43.5f,
            cpuTempC = 55.5f,
            foregroundApp = "Maps",
            durationMs = 12_000L,
        )

    private fun throttlingEvent(): ThrottlingEvent =
        ThrottlingEvent(
            id = 7L,
            timestamp = 1_000L,
            thermalStatus = "SEVERE",
            batteryTempC = 43.5f,
            cpuTempC = 55.5f,
            foregroundApp = "Maps",
            durationMs = 12_000L,
        )
}
