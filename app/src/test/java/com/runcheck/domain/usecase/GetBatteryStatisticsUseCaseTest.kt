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

    private suspend fun calculate(readings: List<BatteryReading>) =
        run {
            coEvery { batteryRepository.getReadingsSinceSync(any()) } returns readings
            useCase(periodDays = 7)
        }

    private fun readings(
        levels: List<Int>,
        statuses: List<String> = List(levels.size) { "DISCHARGING" },
    ): List<BatteryReading> {
        require(levels.size == statuses.size)
        return levels.mapIndexed { index, level ->
            reading(timestamp = 1000L + index * HOUR_MS, level = level, status = statuses[index])
        }
    }

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
            val result = calculate(readings(listOf(80, 70, 60)))

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
            val result =
                calculate(
                    readings(
                        levels = listOf(50, 60, 70, 65, 55),
                        statuses = listOf("CHARGING", "CHARGING", "CHARGING", "DISCHARGING", "DISCHARGING"),
                    ),
                )

            assertNotNull(result)
            val stats = requireNotNull(result)
            assertEquals(20f, stats.totalChargedPct, 0.01f)
            assertEquals(15f, stats.totalDischargedPct, 0.01f)
        }

    @Test
    fun `session count detection counts charge session starts`() =
        runTest {
            // Two separate charging sessions:
            // Session 1: DISCHARGING -> CHARGING -> CHARGING
            // Session 2: DISCHARGING -> CHARGING -> DISCHARGING
            val readings =
                readings(
                    levels = listOf(80, 75, 76, 85, 80, 75, 76, 90),
                    statuses =
                        listOf(
                            "DISCHARGING",
                            "DISCHARGING",
                            "CHARGING",
                            "CHARGING",
                            "DISCHARGING",
                            "DISCHARGING",
                            "CHARGING",
                            "DISCHARGING",
                        ),
                )
            val result = calculate(readings)

            assertNotNull(result)
            assertEquals(2, requireNotNull(result).chargeSessions)
        }

    @Test
    fun `average drain rate calculated from discharging pairs`() =
        runTest {
            // 3 discharging readings, each 1 hour apart, draining 5% per hour
            val result = calculate(readings(listOf(90, 85, 80)))

            assertNotNull(result)
            // Total drain = 5 + 5 = 10% over 2 hours = 5%/hr
            val stats = requireNotNull(result)
            assertNotNull(stats.avgDrainRatePctPerHour)
            assertEquals(5f, requireNotNull(stats.avgDrainRatePctPerHour), 0.1f)
        }

    @Test
    fun `full charge estimate hours calculated from drain rate`() =
        runTest {
            // 10% per hour drain
            val result = calculate(readings(listOf(100, 90, 80)))

            assertNotNull(result)
            // 10% per hour -> full charge estimate = 100/10 = 10 hours
            val stats = requireNotNull(result)
            assertNotNull(stats.fullChargeEstimateHours)
            assertEquals(10f, requireNotNull(stats.fullChargeEstimateHours), 0.5f)
        }

    @Test
    fun `no discharging readings produces null drain rate`() =
        runTest {
            // Only charging readings
            val result = calculate(readings(listOf(50, 60, 70), List(3) { "CHARGING" }))

            assertNotNull(result)
            val stats = requireNotNull(result)
            assertNull(stats.avgDrainRatePctPerHour)
            assertNull(stats.fullChargeEstimateHours)
        }

    @Test
    fun `very low drain rate produces null full charge estimate`() =
        runTest {
            // Drain rate near zero: 0 actual drain in discharging readings
            val result = calculate(readings(listOf(80, 80, 80)))

            assertNotNull(result)
            // drain is 0%/hr which is < 0.1, so fullChargeEstimateHours should be null
            assertNull(requireNotNull(result).fullChargeEstimateHours)
        }

    @Test
    fun `readings are sorted by timestamp regardless of input order`() =
        runTest {
            // Supply readings out of order
            val result = calculate(readings(listOf(80, 70, 60)).reversed())

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
            // Level goes up by 1 even during "DISCHARGING" status (edge case)
            val result = calculate(readings(listOf(50, 51, 48)))

            assertNotNull(result)
            // +1 from 50->51 is charged, -3 from 51->48 is discharged
            val stats = requireNotNull(result)
            assertEquals(1f, stats.totalChargedPct, 0.01f)
            assertEquals(3f, stats.totalDischargedPct, 0.01f)
        }

    @Test
    fun `NOT_CHARGING status counts as discharging pair for drain rate`() =
        runTest {
            val result = calculate(readings(listOf(80, 75, 70), List(3) { "NOT_CHARGING" }))

            assertNotNull(result)
            val stats = requireNotNull(result)
            assertNotNull(stats.avgDrainRatePctPerHour)
            assertEquals(5f, requireNotNull(stats.avgDrainRatePctPerHour), 0.1f)
        }

    private companion object {
        const val HOUR_MS = 3_600_000L
    }
}
