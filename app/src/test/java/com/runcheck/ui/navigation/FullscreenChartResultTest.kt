package com.runcheck.ui.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import com.runcheck.ui.fullscreen.FullscreenChartResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FullscreenChartResultTest {
    @Test
    fun `consuming a result clears existing observers and keeps them connected for the next result`() {
        val handle = SavedStateHandle()
        val entry = mockk<NavBackStackEntry>()
        every { entry.savedStateHandle } returns handle
        val keys =
            listOf(
                FullscreenChartResult.KEY_SOURCE,
                FullscreenChartResult.KEY_METRIC,
                FullscreenChartResult.KEY_PERIOD,
            )
        val observers = keys.associateWith { handle.getStateFlow<String?>(it, null) }
        keys.forEach { handle[it] = "first" }

        entry.consumeFullscreenChartResult()

        keys.forEach {
            assertNull(handle.get<String>(it))
            assertNull(observers.getValue(it).value)
        }
        keys.forEach { handle[it] = "second" }
        keys.forEach { assertEquals("second", observers.getValue(it).value) }
    }
}
