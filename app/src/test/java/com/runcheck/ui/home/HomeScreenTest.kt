package com.runcheck.ui.home

import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.domain.model.NetworkState
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
    fun `health gauge matches reference segment fills`() {
        assertEquals(22, filledHealthGaugeSegments(100))
        assertEquals(8, filledHealthGaugeSegments(38))
        assertEquals(16, filledHealthGaugeSegments(74))
        assertEquals(11, filledHealthGaugeSegments(48))
        assertEquals(5, filledHealthGaugeSegments(22))
    }

    @Test
    fun `health gauge clamps scores to its twenty two segments`() {
        assertEquals(0, filledHealthGaugeSegments(-1))
        assertEquals(0, filledHealthGaugeSegments(0))
        assertEquals(1, filledHealthGaugeSegments(1))
        assertEquals(1, filledHealthGaugeSegments(2))
        assertEquals(22, filledHealthGaugeSegments(98))
        assertEquals(22, filledHealthGaugeSegments(101))
    }

    @Test
    fun `home mosaics stack when effective width is narrow`() {
        assertEquals(false, homeUsesSingleColumn(320.dp, fontScale = 1f))
        assertEquals(true, homeUsesSingleColumn(319.dp, fontScale = 1f))
        assertEquals(true, homeUsesSingleColumn(320.dp, fontScale = 1.4f))
        assertEquals(true, homeUsesSingleColumn(400.dp, fontScale = 1.5f))
    }

    @Test
    fun `disconnected network tile is unrated`() {
        assertEquals(
            R.string.score_unrated,
            networkSignalStatusLabelRes(NetworkState.disconnected()),
        )
    }

    @Test
    fun `status tiles retain reference category order`() {
        assertEquals(
            listOf(
                HomeStatusTileCategory.BATTERY,
                HomeStatusTileCategory.THERMAL,
                HomeStatusTileCategory.STORAGE,
                HomeStatusTileCategory.NETWORK,
            ),
            HomeStatusTileCategory.entries,
        )
    }

    @Test
    fun `short home viewport uses compact layout`() {
        assertEquals(HomeLayoutMode.COMPACT, homeLayoutMode(839.dp, fontScale = 1f))
    }

    @Test
    fun `home viewport keeps regular layout at compact boundary`() {
        assertEquals(HomeLayoutMode.REGULAR, homeLayoutMode(840.dp, fontScale = 1f))
    }

    @Test
    fun `enlarged text keeps scrollable regular layout`() {
        assertEquals(HomeLayoutMode.REGULAR, homeLayoutMode(700.dp, fontScale = 1.5f))
    }
}
