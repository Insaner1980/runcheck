package com.runcheck.ui.chart

import com.runcheck.ui.common.StorageUsagePresentation
import com.runcheck.ui.theme.RuncheckStatusColors
import com.runcheck.ui.theme.forHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StorageQualityZonesTest {
    private val colors = RuncheckStatusColors

    @Test
    fun `storage zones match canonical severity including exact boundaries and old gaps`() {
        val zones = requireNotNull(storageQualityZones(StorageHistoryMetric.USED_SPACE, colors))
        val cases =
            listOf(
                0f to colors.healthy,
                25f to colors.healthy,
                Math.nextDown(75f) to colors.healthy,
                75f to colors.fair,
                80f to colors.fair,
                Math.nextDown(85f) to colors.fair,
                85f to colors.poor,
                90f to colors.poor,
                Math.nextDown(95f) to colors.poor,
                95f to colors.critical,
                100f to colors.critical,
            )
        cases.forEach { (value, expected) ->
            assertEquals("zone at $value", expected, qualityZoneColorForValue(value, zones, colors.neutral))
            assertEquals(expected, colors.forHealthStatus(StorageUsagePresentation.classify(value)))
        }
        assertEquals(listOf(0f, 75f, 85f, 95f), zones.map { it.minValue })
        assertEquals(listOf(75f, 85f, 95f, 100f), zones.map { it.maxValue })
        zones.forEach { assertEquals(0.08f, it.color.alpha, 0.005f) }
    }

    @Test
    fun `visual scope and available space behavior remain unchanged`() {
        val zones = requireNotNull(storageQualityZones(StorageHistoryMetric.USED_SPACE, colors))
        listOf(-1f, 101f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY).forEach {
            assertEquals(colors.neutral, qualityZoneColorForValue(it, zones, colors.neutral))
        }
        assertNotNull(storageQualityZones(StorageHistoryMetric.USED_SPACE, colors))
        assertNull(storageQualityZones(StorageHistoryMetric.AVAILABLE_SPACE, colors))
    }
}
