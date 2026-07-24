package com.runcheck.data.weekly

import com.runcheck.data.db.dao.AppBatteryUsageDao
import com.runcheck.data.db.dao.BatteryReadingDao
import com.runcheck.data.db.dao.SpeedTestResultDao
import com.runcheck.data.db.dao.StorageReadingDao
import com.runcheck.data.db.dao.ThermalReadingDao
import com.runcheck.data.db.dao.ThrottlingEventDao
import com.runcheck.domain.model.WeeklyReportPeriod
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WeeklyReportRepositoryImplTest {
    private val batteryDao = mockk<BatteryReadingDao>()
    private val storageDao = mockk<StorageReadingDao>()
    private val thermalDao = mockk<ThermalReadingDao>()
    private val throttlingDao = mockk<ThrottlingEventDao>()
    private val speedDao = mockk<SpeedTestResultDao>()
    private val appUsageDao = mockk<AppBatteryUsageDao>()

    @Test
    fun `loads every history with inclusive start and exclusive end`() =
        runTest {
            val period =
                WeeklyReportPeriod(
                    Instant.ofEpochMilli(100),
                    Instant.ofEpochMilli(200),
                    ZoneId.of("UTC"),
                )
            coEvery { batteryDao.getReadingsInPeriod(100, 200) } returns emptyList()
            coEvery { storageDao.getReadingsInPeriod(100, 200) } returns emptyList()
            coEvery { thermalDao.getReadingsInPeriod(100, 200) } returns emptyList()
            coEvery { throttlingDao.getEventsInPeriod(100, 200) } returns emptyList()
            coEvery { speedDao.getResultsInPeriod(100, 200) } returns emptyList()
            coEvery { appUsageDao.getUsageInPeriod(100, 200) } returns emptyList()
            val repository =
                WeeklyReportRepositoryImpl(
                    batteryDao,
                    storageDao,
                    thermalDao,
                    throttlingDao,
                    speedDao,
                    appUsageDao,
                )

            val source = repository.loadPeriod(period)

            assertTrue(source.batteryReadings.isEmpty())
            coVerify(exactly = 1) {
                batteryDao.getReadingsInPeriod(100, 200)
                storageDao.getReadingsInPeriod(100, 200)
                thermalDao.getReadingsInPeriod(100, 200)
                throttlingDao.getEventsInPeriod(100, 200)
                speedDao.getResultsInPeriod(100, 200)
                appUsageDao.getUsageInPeriod(100, 200)
            }
        }
}
