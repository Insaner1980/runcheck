package com.runcheck.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshableViewModelStateTest {
    @Test
    fun `updateUiState publishes transformed state`() {
        val state = RefreshableViewModelState(initialState = 1)

        state.updateUiState { current -> current + 1 }

        assertEquals(2, state.uiState.value)
    }
}
