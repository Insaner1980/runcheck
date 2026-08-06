package com.runcheck.ui.home

import com.runcheck.domain.model.HealthStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenTest {
    @Test
    fun `elapsed update time uses complete minutes`() {
        assertEquals(
            3,
            elapsedWholeMinutes(
                lastUpdatedAtEpochMillis = 1_000L,
                currentEpochMillis = 181_999L,
            ),
        )
    }

    @Test
    fun `future update time is clamped to zero minutes`() {
        assertEquals(
            0,
            elapsedWholeMinutes(
                lastUpdatedAtEpochMillis = 2_000L,
                currentEpochMillis = 1_000L,
            ),
        )
    }

    @Test
    fun `status tiles put battery in slot B and sort other categories worst first`() {
        assertEquals(
            HomeStatusTileSlots(
                slotA = HomeStatusTileCategory.THERMAL,
                slotB = HomeStatusTileCategory.BATTERY,
                slotC = HomeStatusTileCategory.STORAGE,
                slotD = HomeStatusTileCategory.NETWORK,
            ),
            assignHomeStatusTileSlots(
                networkStatus = HealthStatus.HEALTHY,
                thermalStatus = HealthStatus.CRITICAL,
                storageStatus = HealthStatus.POOR,
            ),
        )
    }

    @Test
    fun `status tile severity ties use network thermal storage order`() {
        assertEquals(
            HomeStatusTileSlots(
                slotA = HomeStatusTileCategory.NETWORK,
                slotB = HomeStatusTileCategory.BATTERY,
                slotC = HomeStatusTileCategory.THERMAL,
                slotD = HomeStatusTileCategory.STORAGE,
            ),
            assignHomeStatusTileSlots(
                networkStatus = HealthStatus.FAIR,
                thermalStatus = HealthStatus.FAIR,
                storageStatus = HealthStatus.FAIR,
            ),
        )
    }
}
