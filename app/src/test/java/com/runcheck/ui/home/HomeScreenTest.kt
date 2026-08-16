package com.runcheck.ui.home

import androidx.compose.ui.unit.dp
import com.runcheck.domain.model.HealthStatus
import com.runcheck.ui.theme.UiTokens
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
    fun `status tiles sort all categories worst first`() {
        assertEquals(
            HomeStatusTileSlots(
                slotA = HomeStatusTileCategory.BATTERY,
                slotB = HomeStatusTileCategory.THERMAL,
                slotC = HomeStatusTileCategory.STORAGE,
                slotD = HomeStatusTileCategory.NETWORK,
            ),
            assignHomeStatusTileSlots(
                batteryStatus = HealthStatus.CRITICAL,
                networkStatus = HealthStatus.HEALTHY,
                thermalStatus = HealthStatus.POOR,
                storageStatus = HealthStatus.FAIR,
            ),
        )
    }

    @Test
    fun `status tile severity ties use battery network thermal storage order`() {
        assertEquals(
            HomeStatusTileSlots(
                slotA = HomeStatusTileCategory.BATTERY,
                slotB = HomeStatusTileCategory.NETWORK,
                slotC = HomeStatusTileCategory.THERMAL,
                slotD = HomeStatusTileCategory.STORAGE,
            ),
            assignHomeStatusTileSlots(
                batteryStatus = HealthStatus.FAIR,
                networkStatus = HealthStatus.FAIR,
                thermalStatus = HealthStatus.FAIR,
                storageStatus = HealthStatus.FAIR,
            ),
        )
    }

    @Test
    fun `status tiles share one fixed internal grid`() {
        val tokens = UiTokens()

        assertEquals(150.dp, tokens.homeStatusTileHeight)
        assertEquals(18.dp, tokens.homeStatusTileCategoryTop)
        assertEquals(62.dp, tokens.homeStatusTileValueTop)
        assertEquals(14.dp, tokens.homeStatusTileStatusGap)
        assertEquals(4.dp, tokens.homeStatusTileValueSuffixGap)
        assertEquals(17.dp, tokens.homeStatusTilePaddingHorizontal)
    }
}
