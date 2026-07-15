package com.runcheck.data.appusage

import com.runcheck.data.appusage.AppUsageDataSource.AppUsageSnapshot
import com.runcheck.data.db.dao.AppBatteryUsageDao
import com.runcheck.data.db.dao.AppBatteryUsageSummaryRow
import com.runcheck.data.db.entity.AppBatteryUsageEntity
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.AppUsageListSummary
import com.runcheck.domain.repository.UserPreferencesRepository
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

class AppBatteryUsageRepositoryImplTest {
    private val appBatteryUsageDao: AppBatteryUsageDao = mockk(relaxed = true)
    private val appUsageDataSource: AppUsageDataSource = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val repository =
        AppBatteryUsageRepositoryImpl(
            appBatteryUsageDao = appBatteryUsageDao,
            appUsageDataSource = appUsageDataSource,
            userPreferencesRepository = userPreferencesRepository,
            dispatchers = TestAppDispatchers(),
        )

    @Test
    fun `summary and sync queries map rows to domain models`() =
        runTest {
            every { appBatteryUsageDao.getUsageSummarySince(10L) } returns
                flowOf(AppBatteryUsageSummaryRow(totalForegroundTimeMs = 1_000L, maxForegroundTimeMs = 700L))
            coEvery { appBatteryUsageDao.getUsageSinceSync(10L) } returns listOf(appUsageEntity())

            assertEquals(
                AppUsageListSummary(totalForegroundTimeMs = 1_000L, maxForegroundTimeMs = 700L),
                repository.getUsageSummarySince(10L).first(),
            )
            assertEquals(listOf(appUsageDomain()), repository.getUsageSinceSync(10L))
        }

    @Test
    fun `collectUsageSnapshot skips data source when permission is missing`() =
        runTest {
            every { appUsageDataSource.hasUsageStatsPermission() } returns false

            repository.collectUsageSnapshot()

            coVerify(exactly = 0) { appUsageDataSource.getUsageSince(any(), any()) }
            coVerify(exactly = 0) { appBatteryUsageDao.insertAll(any()) }
        }

    @Test
    fun `collectUsageSnapshot stores snapshots and advances last collection time`() =
        runTest {
            val inserted = slot<List<AppBatteryUsageEntity>>()
            every { appUsageDataSource.hasUsageStatsPermission() } returns true
            coEvery { userPreferencesRepository.getAppUsageLastCollectedAt() } returns 100L
            coEvery { appUsageDataSource.getUsageSince(any(), any()) } returns
                listOf(
                    AppUsageSnapshot(
                        packageName = "com.example",
                        appLabel = "Example",
                        foregroundTimeMs = 1_200L,
                    ),
                )

            repository.collectUsageSnapshot()

            coVerify(exactly = 1) { appBatteryUsageDao.insertAll(capture(inserted)) }
            val stored = inserted.captured.single()
            assertEquals("com.example", stored.packageName)
            assertEquals("Example", stored.appLabel)
            assertEquals(1_200L, stored.foregroundTimeMs)
            assertEquals(null, stored.estimatedDrainMah)
            coVerify(exactly = 1) { userPreferencesRepository.setAppUsageLastCollectedAt(any()) }
        }

    @Test
    fun `collectUsageSnapshot limits stale collection window to 24 hours`() =
        runTest {
            val startTime = slot<Long>()
            val endTime = slot<Long>()
            every { appUsageDataSource.hasUsageStatsPermission() } returns true
            coEvery { userPreferencesRepository.getAppUsageLastCollectedAt() } returns 0L
            coEvery { appUsageDataSource.getUsageSince(capture(startTime), capture(endTime)) } returns emptyList()

            repository.collectUsageSnapshot()

            assertEquals(24L * 60L * 60L * 1000L, endTime.captured - startTime.captured)
        }

    @Test
    fun `delete methods delegate to dao`() =
        runTest {
            repository.deleteOlderThan(100L)
            repository.deleteAll()

            coVerify(exactly = 1) { appBatteryUsageDao.deleteOlderThan(100L) }
            coVerify(exactly = 1) { appBatteryUsageDao.deleteAll() }
        }

    private fun appUsageEntity(): AppBatteryUsageEntity =
        AppBatteryUsageEntity(
            id = 4L,
            timestamp = 1_234L,
            packageName = "com.example",
            appLabel = "Example",
            foregroundTimeMs = 1_200L,
            estimatedDrainMah = 1.5f,
        )

    private fun appUsageDomain(): AppBatteryUsage =
        AppBatteryUsage(
            id = 4L,
            timestamp = 1_234L,
            packageName = "com.example",
            appLabel = "Example",
            foregroundTimeMs = 1_200L,
            estimatedDrainMah = 1.5f,
        )
}
