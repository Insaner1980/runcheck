package com.runcheck.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageAlertThresholdValuesTest {
    @Test
    fun `current canonical bounds preserve the visible options`() {
        assertEquals(
            listOf(70, 75, 80, 85, 90, 95, 99),
            LOW_STORAGE_THRESHOLD_VALUES,
        )
    }

    @Test
    fun `aligned maximum appears exactly once`() {
        assertEquals(
            listOf(70, 75, 80, 85, 90),
            storageAlertThresholdValues(min = 70, max = 90),
        )
    }

    @Test
    fun `non-aligned maximum is appended after the regular values`() {
        assertEquals(
            listOf(70, 75, 80, 85, 90, 95, 99),
            storageAlertThresholdValues(min = 70, max = 99),
        )
    }

    @Test
    fun `options remain sorted unique and within the requested range`() {
        val min = 70
        val max = 99
        val values = storageAlertThresholdValues(min = min, max = max)

        assertEquals(values.sorted(), values)
        assertEquals(values.distinct(), values)
        assertTrue(values.all { it in min..max })
        assertEquals(min, values.first())
        assertEquals(max, values.last())
    }
}
