package com.runcheck.domain.usecase

import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.WeeklyReportAvailability
import com.runcheck.domain.model.WeeklyReportPeriod
import com.runcheck.domain.model.WeeklyReportSourceData
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.WeeklyReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class GenerateWeeklyReportUseCaseTest {
    private val period =
        WeeklyReportPeriod(
            startInclusive = Instant.parse("2026-02-02T00:00:00Z"),
            endExclusive = Instant.parse("2026-02-09T00:00:00Z"),
            zoneId = ZoneId.of("UTC"),
        )

    @Test
    fun `free access never loads seven day aggregates`() =
        runTest {
            val repository = FakeWeeklyReportRepository(WeeklyReportSourceData())
            val useCase = GenerateWeeklyReportUseCase(repository, FakeWeeklyReportProStatus(false))

            val result = useCase(period)

            assertTrue(result is WeeklyReportGenerationResult.Locked)
            assertEquals(0, repository.loadCount)
        }

    @Test
    fun `report aggregates only defensible period metrics`() =
        runTest {
            val start = period.startInclusive.toEpochMilli()
            val repository =
                FakeWeeklyReportRepository(
                    WeeklyReportSourceData(
                        batteryReadings =
                            listOf(
                                battery(start, 80, 90),
                                battery(start + 60 * 60 * 1000, 75, 89),
                                battery(start + 2 * 60 * 60 * 1000, 79, null),
                            ),
                        storageReadings =
                            listOf(
                                storage(start, 60_000),
                                storage(start + 1_000, 54_000),
                            ),
                        thermalReadings =
                            listOf(
                                thermal(start, status = 1, throttling = false),
                                thermal(start + 1_000, status = 4, throttling = true),
                            ),
                        speedTests =
                            listOf(
                                speed(start, 100.0, 20.0, 30),
                                speed(start + 1_000, 40.0, 10.0, 50),
                            ),
                        appUsage =
                            listOf(
                                app(start, "example.one", "One", 8_000),
                                app(start, "example.two", "Two", 3_000),
                            ),
                    ),
                )
            val useCase = GenerateWeeklyReportUseCase(repository, FakeWeeklyReportProStatus(true))

            val result = useCase(period) as WeeklyReportGenerationResult.Available

            assertEquals(-5.0, result.report.battery.dischargePercentChange, 0.001)
            assertEquals(4.0, result.report.battery.chargePercentChange, 0.001)
            assertEquals(-1, result.report.battery.healthPercentChange)
            assertEquals(-6_000L, result.report.storage.availableBytesChange)
            assertEquals(0, result.report.thermal.throttlingEventCount)
            assertEquals(4, result.report.thermal.highestThermalStatus)
            assertEquals(70.0, requireNotNull(result.report.speed.medianDownloadMbps), 0.001)
            assertEquals(15.0, requireNotNull(result.report.speed.medianUploadMbps), 0.001)
            assertEquals(40.0, requireNotNull(result.report.speed.medianLatencyMs), 0.001)
            assertEquals("example.one", result.report.topApps.first().packageName)
            assertEquals(WeeklyReportAvailability.ESTIMATED, result.report.coverage.availability)
        }

    @Test
    fun `large battery sample gaps are excluded`() =
        runTest {
            val start = period.startInclusive.toEpochMilli()
            val repository =
                FakeWeeklyReportRepository(
                    WeeklyReportSourceData(
                        batteryReadings =
                            listOf(
                                battery(start, 80, null),
                                battery(start + 12 * 60 * 60 * 1000, 20, null),
                            ),
                    ),
                )
            val result =
                GenerateWeeklyReportUseCase(repository, FakeWeeklyReportProStatus(true))(period)
                    as WeeklyReportGenerationResult.Available

            assertEquals(0.0, result.report.battery.dischargePercentChange, 0.001)
            assertEquals(WeeklyReportAvailability.UNAVAILABLE, result.report.battery.availability)
        }

    @Test
    fun `zero source data is unavailable`() =
        runTest {
            val result =
                GenerateWeeklyReportUseCase(
                    FakeWeeklyReportRepository(WeeklyReportSourceData()),
                    FakeWeeklyReportProStatus(true),
                )(period) as WeeklyReportGenerationResult.Available

            assertEquals(WeeklyReportAvailability.UNAVAILABLE, result.report.coverage.availability)
            assertEquals(0, result.report.coverage.sampleCount)
            assertEquals(WeeklyReportAvailability.UNAVAILABLE, result.report.battery.availability)
        }

    @Test
    fun `one source sample does not overstate metric availability`() =
        runTest {
            val result =
                GenerateWeeklyReportUseCase(
                    FakeWeeklyReportRepository(
                        WeeklyReportSourceData(
                            batteryReadings = listOf(battery(period.startInclusive.toEpochMilli(), 80, null)),
                        ),
                    ),
                    FakeWeeklyReportProStatus(true),
                )(period) as WeeklyReportGenerationResult.Available

            assertEquals(WeeklyReportAvailability.ESTIMATED, result.report.coverage.availability)
            assertEquals(WeeklyReportAvailability.UNAVAILABLE, result.report.battery.availability)
        }

    @Test
    fun `seven local sampled days provide full coverage`() =
        runTest {
            val start = period.startInclusive.toEpochMilli()
            val result =
                GenerateWeeklyReportUseCase(
                    FakeWeeklyReportRepository(
                        WeeklyReportSourceData(
                            batteryReadings =
                                (0 until 7).map { day ->
                                    battery(start + day * 24L * 60L * 60L * 1000L, 80, null)
                                },
                        ),
                    ),
                    FakeWeeklyReportProStatus(true),
                )(period) as WeeklyReportGenerationResult.Available

            assertEquals(7, result.report.coverage.monitoredDays)
            assertEquals(WeeklyReportAvailability.AVAILABLE, result.report.coverage.availability)
        }

    @Test
    fun `app usage crossing the period boundary is explicitly estimated`() =
        runTest {
            val start = period.startInclusive.toEpochMilli()
            val result =
                GenerateWeeklyReportUseCase(
                    FakeWeeklyReportRepository(
                        WeeklyReportSourceData(
                            batteryReadings =
                                (0 until 7).map { day ->
                                    battery(start + day * 24L * 60L * 60L * 1000L, 80, null)
                                },
                            appUsage = listOf(app(start + 60_000L, "example.crossing", "Crossing", 120_000L)),
                        ),
                    ),
                    FakeWeeklyReportProStatus(true),
                )(period) as WeeklyReportGenerationResult.Available

            assertEquals(WeeklyReportAvailability.ESTIMATED, result.report.coverage.availability)
            assertEquals(WeeklyReportAvailability.ESTIMATED, result.report.topApps.single().availability)
        }

    private fun battery(
        timestamp: Long,
        level: Int,
        healthPct: Int?,
    ) = BatteryReading(
        timestamp = timestamp,
        level = level,
        voltageMv = 4_000,
        temperatureC = 30f,
        currentMa = null,
        currentConfidence = "UNAVAILABLE",
        status = "DISCHARGING",
        plugType = "NONE",
        health = "GOOD",
        cycleCount = null,
        healthPct = healthPct,
    )

    private fun storage(
        timestamp: Long,
        available: Long,
    ) = StorageReading(timestamp, 128_000, available, null, 0)

    private fun thermal(
        timestamp: Long,
        status: Int,
        throttling: Boolean,
    ) = ThermalReading(timestamp, 35f, null, status, throttling)

    private fun speed(
        timestamp: Long,
        download: Double,
        upload: Double,
        ping: Int,
    ) = SpeedTestResult(
        timestamp = timestamp,
        downloadMbps = download,
        uploadMbps = upload,
        pingMs = ping,
        jitterMs = null,
        serverName = null,
        serverLocation = null,
        connectionType = ConnectionType.WIFI,
        networkSubtype = null,
        signalDbm = null,
    )

    private fun app(
        timestamp: Long,
        packageName: String,
        label: String,
        foregroundMs: Long,
    ) = AppBatteryUsage(
        timestamp = timestamp,
        packageName = packageName,
        appLabel = label,
        foregroundTimeMs = foregroundMs,
        estimatedDrainMah = null,
    )
}

private class FakeWeeklyReportRepository(
    private val source: WeeklyReportSourceData,
) : WeeklyReportRepository {
    var loadCount = 0

    override suspend fun loadPeriod(period: WeeklyReportPeriod): WeeklyReportSourceData {
        loadCount++
        return source
    }
}

private class FakeWeeklyReportProStatus(
    private val isPro: Boolean,
) : ProStatusProvider {
    override val isProUser: Flow<Boolean> = flowOf(isPro)
    override val isProStatusReady: Boolean = true

    override fun isPro(): Boolean = isPro
}
