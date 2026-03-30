package com.runcheck.domain.usecase

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.repository.BatteryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetBatteryStatisticsUseCaseTest {
    private lateinit var useCase: GetBatteryStatisticsUseCase
    private lateinit var batteryRepository: BatteryRepository

    @Before
    fun setup() {
        batteryRepository = mockk()
        useCase = GetBatteryStatisticsUseCase(batteryRepository, BatteryDrainAnalyzer())
    }

    private fun reading(
        timestamp: Long,
        level: Int,
        status: String = "DISCHARGING",
    ) = BatteryReading(
        timestamp = timestamp,
        level = level,
        voltageMv = 4000,
        temperatureC = 30f,
        currentMa = -400,
        currentConfidence = "HIGH",
        status = status,
        plugType = "NONE",
        health = "GOOD",
        cycleCount = null,
        healthPct = null,
    )

    @Test
    fun `empty readings returns null`() =
        runTest {
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns emptyList()

            val result = useCase(periodDays = 7)

            assertNull(result)
        }

    @Test
    fun `single reading returns null`() =
        runTest {
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns
                listOf(
                    reading(timestamp = 1000L, level = 80),
                )

            val result = useCase(periodDays = 7)

            assertNull(result)
        }

    @Test
    fun `normal discharge cycle calculates correct totals`() =
        runTest {
            // 80 -> 70 -> 60: discharged 20%, charged 0%
            val hourMs = 3_600_000L
            val readings =
                listOf(
                    reading(timestamp = 1000L, level = 80),
                    reading(timestamp = 1000L + hourMs, level = 70),
                    reading(timestamp = 1000L + 2 * hourMs, level = 60),
                )
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns readings

            val result = useCase(periodDays = 7)

            assertNotNull(result)
            val stats = requireNotNull(result)
            assertEquals(0f, stats.totalChargedPct, 0.01f)
            assertEquals(20f, stats.totalDischargedPct, 0.01f)
            assertEquals(3, stats.readingCount)
        }

    @Test
    fun `charge and discharge cycle calculates correctly`() =
        runTest {
            // 50 -> 60 -> 70 -> 65 -> 55
            // Charge: (60-50) + (70-60) = 20
            // Discharge: (70-65) + (65-55) = 15
            val hourMs = 3_600_000L
            val readings =
                listOf(
                    reading(timestamp = 1000L, level = 50, status = "CHARGING"),
                    reading(timestamp = 1000L + hourMs, level = 60, status = "CHARGING"),
                    reading(timestamp = 1000L + 2 * hourMs, level = 70, status = "CHARGING"),
                    reading(timestamp = 1000L + 3 * hourMs, level = 65, status = "DISCHARGING"),
                    reading(timestamp = 1000L + 4 * hourMs, level = 55, status = "DISCHARGING"),
                )
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns readings

            val result = useCase(periodDays = 7)

            assertNotNull(result)
            val stats = requireNotNull(result)
            assertEquals(20f, stats.totalChargedPct, 0.01f)
            assertEquals(15f, stats.totalDischargedPct, 0.01f)
        }

    @Test
    fun `session count detection counts charge session starts`() =
        runTest {
            val hourMs = 3_600_000L
            // Two separate charging sessions:
            // Session 1: DISCHARGING -> CHARGING -> CHARGING
            // Session 2: DISCHARGING -> CHARGING -> DISCHARGING
            val readings =
                listOf(
                    reading(timestamp = 1000L, level = 80, status = "DISCHARGING"),
                    reading(timestamp = 1000L + hourMs, level = 75, status = "DISCHARGING"),
                    reading(timestamp = 1000L + 2 * hourMs, level = 76, status = "CHARGING"), // session 1 start
                    reading(timestamp = 1000L + 3 * hourMs, level = 85, status = "CHARGING"),
                    reading(timestamp = 1000L + 4 * hourMs, level = 80, status = "DISCHARGING"),
                    reading(timestamp = 1000L + 5 * hourMs, level = 75, status = "DISCHARGING"),
                    reading(timestamp = 1000L + 6 * hourMs, level = 76, status = "CHARGING"), // session 2 start
                    reading(timestamp = 1000L + 7 * hourMs, level = 90, status = "DISCHARGING"),
                )
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns readings

            val result = useCase(periodDays = 7)

            assertNotNull(result)
            assertEquals(2, requireNotNull(result).chargeSessions)
        }

    @Test
    fun `average drain rate calculated from discharging pairs`() =
        runTest {
            val hourMs = 3_600_000L
            // 3 discharging readings, each 1 hour apart, draining 5% per hour
            val readings =
                listOf(
                    reading(timestamp = 1000L, level = 90, status = "DISCHARGING"),
                    reading(timestamp = 1000L + hourMs, level = 85, status = "DISCHARGING"),
                    reading(timestamp = 1000L + 2 * hourMs, level = 80, status = "DISCHARGING"),
                )
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns readings

            val result = useCase(periodDays = 7)

            assertNotNull(result)
            // Total drain = 5 + 5 = 10% over 2 hours = 5%/hr
            val stats = requireNotNull(result)
            assertNotNull(stats.avgDrainRatePctPerHour)
            assertEquals(5f, requireNotNull(stats.avgDrainRatePctPerHour), 0.1f)
        }

    @Test
    fun `full charge estimate hours calculated from drain rate`() =
        runTest {
            val hourMs = 3_600_000L
            // 10% per hour drain
            val readings =
                listOf(
                    reading(timestamp = 1000L, level = 100, status = "DISCHARGING"),
                    reading(timestamp = 1000L + hourMs, level = 90, status = "DISCHARGING"),
                    reading(timestamp = 1000L + 2 * hourMs, level = 80, status = "DISCHARGING"),
                )
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns readings

            val result = useCase(periodDays = 7)

            assertNotNull(result)
            // 10% per hour -> full charge estimate = 100/10 = 10 hours
            val stats = requireNotNull(result)
            assertNotNull(stats.fullChargeEstimateHours)
            assertEquals(10f, requireNotNull(stats.fullChargeEstimateHours), 0.5f)
        }

    @Test
    fun `no discharging readings produces null drain rate`() =
        runTest {
            val hourMs = 3_600_000L
            // Only charging readings
            val readings =
                listOf(
                    reading(timestamp = 1000L, level = 50, status = "CHARGING"),
                    reading(timestamp = 1000L + hourMs, level = 60, status = "CHARGING"),
                    reading(timestamp = 1000L + 2 * hourMs, level = 70, status = "CHARGING"),
                )
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns readings

            val result = useCase(periodDays = 7)

            assertNotNull(result)
            val stats = requireNotNull(result)
            assertNull(stats.avgDrainRatePctPerHour)
            assertNull(stats.fullChargeEstimateHours)
        }

    @Test
    fun `very low drain rate produces null full charge estimate`() =
        runTest {
            val hourMs = 3_600_000L
            // Drain rate near zero: 0 actual drain in discharging readings
            val readings =
                listOf(
                    reading(timestamp = 1000L, level = 80, status = "DISCHARGING"),
                    reading(timestamp = 1000L + hourMs, level = 80, status = "DISCHARGING"),
                    reading(timestamp = 1000L + 2 * hourMs, level = 80, status = "DISCHARGING"),
                )
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns readings

            val result = useCase(periodDays = 7)

            assertNotNull(result)
            // drain is 0%/hr which is < 0.1, so fullChargeEstimateHours should be null
            assertNull(requireNotNull(result).fullChargeEstimateHours)
        }

    @Test
    fun `readings are sorted by timestamp regardless of input order`() =
        runTest {
            val hourMs = 3_600_000L
            // Supply readings out of order
            val readings =
                listOf(
                    reading(timestamp = 1000L + 2 * hourMs, level = 60, status = "DISCHARGING"),
                    reading(timestamp = 1000L, level = 80, status = "DISCHARGING"),
                    reading(timestamp = 1000L + hourMs, level = 70, status = "DISCHARGING"),
                )
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns readings

            val result = useCase(periodDays = 7)

            assertNotNull(result)
            // Should still correctly compute: 80->70->60, discharged = 20
            assertEquals(20f, requireNotNull(result).totalDischargedPct, 0.01f)
        }

    @Test
    fun `default period is 10 days`() {
        assertEquals(10, GetBatteryStatisticsUseCase.DEFAULT_PERIOD_DAYS)
    }

    @Test
    fun `level increasing during discharge counts as charged`() =
        runTest {
            val hourMs = 3_600_000L
            // Level goes up by 1 even during "DISCHARGING" status (edge case)
            val readings =
                listOf(
                    reading(timestamp = 1000L, level = 50, status = "DISCHARGING"),
                    reading(timestamp = 1000L + hourMs, level = 51, status = "DISCHARGING"),
                    reading(timestamp = 1000L + 2 * hourMs, level = 48, status = "DISCHARGING"),
                )
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns readings

            val result = useCase(periodDays = 7)

            assertNotNull(result)
            // +1 from 50->51 is charged, -3 from 51->48 is discharged
            val stats = requireNotNull(result)
            assertEquals(1f, stats.totalChargedPct, 0.01f)
            assertEquals(3f, stats.totalDischargedPct, 0.01f)
        }

    @Test
    fun `NOT_CHARGING status counts as discharging pair for drain rate`() =
        runTest {
            val hourMs = 3_600_000L
            val readings =
                listOf(
                    reading(timestamp = 1000L, level = 80, status = "NOT_CHARGING"),
                    reading(timestamp = 1000L + hourMs, level = 75, status = "NOT_CHARGING"),
                    reading(timestamp = 1000L + 2 * hourMs, level = 70, status = "NOT_CHARGING"),
                )
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns readings

            val result = useCase(periodDays = 7)

            assertNotNull(result)
            val stats = requireNotNull(result)
            assertNotNull(stats.avgDrainRatePctPerHour)
            assertEquals(5f, requireNotNull(stats.avgDrainRatePctPerHour), 0.1f)
        }
}
