package com.runcheck.domain.usecase

import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.model.StorageReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateFillRateUseCaseTest {
    private lateinit var useCase: CalculateFillRateUseCase

    @Before
    fun setup() {
        useCase = CalculateFillRateUseCase(StorageGrowthAnalyzer())
    }

    // --- invoke() tests ---

    @Test
    fun `steady growth over 5 readings returns correct positive bytes per day`() {
        val baseTime = 1_000_000_000L
        val dayMs = 24L * 60 * 60 * 1000
        val totalBytes = 128_000_000_000L

        // Each day, used storage grows by 1 GB (available decreases by 1 GB)
        val readings =
            (0..4).map { i ->
                StorageReading(
                    timestamp = baseTime + i * dayMs,
                    totalBytes = totalBytes,
                    availableBytes = 64_000_000_000L - i * 1_000_000_000L,
                    appsBytes = 30_000_000_000L,
                    mediaBytes = 20_000_000_000L,
                )
            }

        val bytesPerDay = useCase(readings)

        assertNotNull(bytesPerDay)
        // Linear regression on perfectly linear data should yield ~1 GB/day
        val expectedBytesPerDay = 1_000_000_000L
        val tolerance = 10_000L // tiny rounding tolerance
        val rate = requireNotNull(bytesPerDay)
        assertTrue(
            "Expected ~$expectedBytesPerDay bytes/day, got $bytesPerDay",
            kotlin.math.abs(rate - expectedBytesPerDay) < tolerance,
        )
    }

    @Test
    fun `less than 3 readings returns null`() {
        val reading =
            StorageReading(
                timestamp = 1_000_000_000L,
                totalBytes = 128_000_000_000L,
                availableBytes = 64_000_000_000L,
                appsBytes = 30_000_000_000L,
                mediaBytes = 20_000_000_000L,
            )

        assertNull("Empty list should return null", useCase(emptyList()))
        assertNull("Single reading should return null", useCase(listOf(reading)))
        assertNull(
            "Two readings should return null",
            useCase(listOf(reading, reading.copy(timestamp = 2_000_000_000L))),
        )
    }

    @Test
    fun `exactly 3 readings returns non-null result`() {
        val dayMs = 24L * 60 * 60 * 1000
        val readings =
            (0..2).map { i ->
                StorageReading(
                    timestamp = 1_000_000_000L + i * dayMs,
                    totalBytes = 128_000_000_000L,
                    availableBytes = 64_000_000_000L - i * 500_000_000L,
                    appsBytes = 30_000_000_000L,
                    mediaBytes = 20_000_000_000L,
                )
            }

        assertNotNull("3 readings should be sufficient", useCase(readings))
    }

    @Test
    fun `identical timestamps returns null without crash`() {
        val sameTime = 1_000_000_000L
        val readings =
            (0..4).map {
                StorageReading(
                    timestamp = sameTime,
                    totalBytes = 128_000_000_000L,
                    availableBytes = 64_000_000_000L,
                    appsBytes = 30_000_000_000L,
                    mediaBytes = 20_000_000_000L,
                )
            }

        // denominator is zero when all timestamps are identical
        val result = useCase(readings)
        assertNull("Identical timestamps should return null (zero denominator)", result)
    }

    @Test
    fun `decreasing storage usage returns negative rate`() {
        val baseTime = 1_000_000_000L
        val dayMs = 24L * 60 * 60 * 1000

        // Cleanup happened: available increases each day
        val readings =
            (0..4).map { i ->
                StorageReading(
                    timestamp = baseTime + i * dayMs,
                    totalBytes = 128_000_000_000L,
                    availableBytes = 50_000_000_000L + i * 2_000_000_000L,
                    appsBytes = 30_000_000_000L,
                    mediaBytes = 20_000_000_000L,
                )
            }

        val bytesPerDay = useCase(readings)

        assertNotNull(bytesPerDay)
        assertTrue(
            "Decreasing usage should yield negative rate, got $bytesPerDay",
            requireNotNull(bytesPerDay) < 0,
        )
    }

    @Test
    fun `flat usage returns approximately zero rate`() {
        val baseTime = 1_000_000_000L
        val dayMs = 24L * 60 * 60 * 1000

        val readings =
            (0..4).map { i ->
                StorageReading(
                    timestamp = baseTime + i * dayMs,
                    totalBytes = 128_000_000_000L,
                    availableBytes = 64_000_000_000L, // no change
                    appsBytes = 30_000_000_000L,
                    mediaBytes = 20_000_000_000L,
                )
            }

        val bytesPerDay = useCase(readings)

        assertNotNull(bytesPerDay)
        assertEquals("Flat usage should yield zero rate", 0L, requireNotNull(bytesPerDay))
    }

    // --- formatEstimate() tests ---

    @Test
    fun `formatEstimate returns days for under 7 days`() {
        assertEquals("0d", useCase.formatEstimate(500L, 1000L))
        assertEquals("1d", useCase.formatEstimate(1000L, 1000L))
        assertEquals("6d", useCase.formatEstimate(6000L, 1000L))
    }

    @Test
    fun `formatEstimate returns weeks for 7 to 59 days`() {
        // 7 days -> 1 week
        assertEquals("1w", useCase.formatEstimate(7000L, 1000L))
        // 14 days -> 2 weeks
        assertEquals("2w", useCase.formatEstimate(14_000L, 1000L))
        // 59 days -> 8 weeks (59/7 = 8)
        assertEquals("8w", useCase.formatEstimate(59_000L, 1000L))
    }

    @Test
    fun `formatEstimate returns months for 60 to 729 days`() {
        // 60 days -> 2 months
        assertEquals("2mo", useCase.formatEstimate(60_000L, 1000L))
        // 365 days -> 12 months
        assertEquals("12mo", useCase.formatEstimate(365_000L, 1000L))
        // 729 days -> 24 months
        assertEquals("24mo", useCase.formatEstimate(729_000L, 1000L))
    }

    @Test
    fun `formatEstimate returns years for 730 days and above`() {
        // 730 days -> 2 years
        assertEquals("2y", useCase.formatEstimate(730_000L, 1000L))
        // 1095 days -> 3 years
        assertEquals("3y", useCase.formatEstimate(1095_000L, 1000L))
    }

    @Test
    fun `formatEstimate returns null for zero or negative bytesPerDay`() {
        assertNull(useCase.formatEstimate(64_000_000_000L, 0L))
        assertNull(useCase.formatEstimate(64_000_000_000L, -100L))
    }

    @Test
    fun `formatEstimate returns null for zero or negative availableBytes`() {
        assertNull(useCase.formatEstimate(0L, 1000L))
        assertNull(useCase.formatEstimate(-100L, 1000L))
    }

    @Test
    fun `formatEstimate boundary at exactly 7 days`() {
        // 7 days exactly should use weeks path: 7/7 = 1w
        assertEquals("1w", useCase.formatEstimate(7L, 1L))
    }

    @Test
    fun `formatEstimate boundary at exactly 60 days`() {
        // 60 days exactly should use months path: 60/30 = 2mo
        assertEquals("2mo", useCase.formatEstimate(60L, 1L))
    }

    @Test
    fun `formatEstimate boundary at exactly 730 days`() {
        // 730 days exactly should use years path: 730/365 = 2y
        assertEquals("2y", useCase.formatEstimate(730L, 1L))
    }
}
