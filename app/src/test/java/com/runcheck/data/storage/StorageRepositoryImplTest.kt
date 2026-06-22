package com.runcheck.data.storage

import com.runcheck.data.db.dao.StorageReadingDao
import com.runcheck.data.db.entity.StorageReadingEntity
import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.model.MediaBreakdown
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.usecase.CalculateFillRateUseCase
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

class StorageRepositoryImplTest {
    private val storageDataSource: StorageDataSource = mockk(relaxed = true)
    private val storageReadingDao: StorageReadingDao = mockk(relaxed = true)
    private val repository =
        StorageRepositoryImpl(
            storageDataSource = storageDataSource,
            storageReadingDao = storageReadingDao,
            calculateFillRate = CalculateFillRateUseCase(StorageGrowthAnalyzer()),
            dispatchers = TestAppDispatchers(),
        )

    @Test
    fun `getStorageState maps storage info and fill rate into domain state`() =
        runTest {
            coEvery { storageDataSource.getStorageInfo() } returns storageInfo()
            coEvery { storageReadingDao.getReadingsSinceSync(any()) } returns
                listOf(storageReadingEntity(availableBytes = 5_000L))

            val state = repository.getStorageState().first()

            assertEquals(10_000L, state.totalBytes)
            assertEquals(4_000L, state.availableBytes)
            assertEquals(6_000L, state.usedBytes)
            assertEquals(60f, state.usagePercent, 0.001f)
            assertEquals(2_000L, state.appsBytes)
            assertEquals(500L, state.totalCacheBytes)
            assertEquals(12, state.appCount)
            assertEquals(true, state.sdCardAvailable)
            assertEquals("FBE", state.encryptionStatus)
            assertEquals(2, state.storageVolumes)
        }

    @Test
    fun `reading queries map entities to domain models`() =
        runTest {
            val entity = storageReadingEntity()
            val expected = storageReading()
            every { storageReadingDao.getReadingsSince(10L) } returns flowOf(listOf(entity))
            every { storageReadingDao.getReadingsSinceLimited(10L, 1) } returns flowOf(listOf(entity))
            coEvery { storageReadingDao.getReadingsSinceSync(10L) } returns listOf(entity)
            coEvery { storageReadingDao.getAll() } returns listOf(entity)

            assertEquals(listOf(expected), repository.getReadingsSince(10L, limit = null).first())
            assertEquals(listOf(expected), repository.getReadingsSince(10L, limit = 1).first())
            assertEquals(listOf(expected), repository.getReadingsSinceSync(10L))
            assertEquals(listOf(expected), repository.getAllReadings())
        }

    @Test
    fun `saveReading stores app and media bytes and delete methods delegate`() =
        runTest {
            val inserted = slot<StorageReadingEntity>()

            repository.saveReading(
                StorageState(
                    totalBytes = 10_000L,
                    availableBytes = 4_000L,
                    usedBytes = 6_000L,
                    usagePercent = 60f,
                    appsBytes = 2_000L,
                    mediaBreakdown =
                        MediaBreakdown(
                            imagesBytes = 100L,
                            videosBytes = 200L,
                            audioBytes = 300L,
                            documentsBytes = 400L,
                            downloadsBytes = 500L,
                        ),
                ),
            )
            repository.deleteOlderThan(100L)
            repository.deleteAll()

            coVerify(exactly = 1) { storageReadingDao.insert(capture(inserted)) }
            assertEquals(10_000L, inserted.captured.totalBytes)
            assertEquals(4_000L, inserted.captured.availableBytes)
            assertEquals(2_000L, inserted.captured.appsBytes)
            assertEquals(1_500L, inserted.captured.mediaBytes)
            coVerify(exactly = 1) { storageReadingDao.deleteOlderThan(100L) }
            coVerify(exactly = 1) { storageReadingDao.deleteAll() }
        }

    private fun storageInfo(): StorageDataSource.StorageInfo =
        StorageDataSource.StorageInfo(
            totalBytes = 10_000L,
            availableBytes = 4_000L,
            usedBytes = 6_000L,
            appsBytes = 2_000L,
            totalCacheBytes = 500L,
            appCount = 12,
            mediaBreakdown = null,
            trashInfo = null,
            sdCardAvailable = true,
            sdCardTotalBytes = 8_000L,
            sdCardAvailableBytes = 2_000L,
            fileSystemType = "ext4",
            encryptionStatus = "FBE",
            storageVolumes = 2,
        )

    private fun storageReadingEntity(availableBytes: Long = 4_000L): StorageReadingEntity =
        StorageReadingEntity(
            id = 6L,
            timestamp = 1_234L,
            totalBytes = 10_000L,
            availableBytes = availableBytes,
            appsBytes = 2_000L,
            mediaBytes = 1_500L,
        )

    private fun storageReading(): StorageReading =
        StorageReading(
            timestamp = 1_234L,
            totalBytes = 10_000L,
            availableBytes = 4_000L,
            appsBytes = 2_000L,
            mediaBytes = 1_500L,
        )
}
