package com.runcheck.ui.fullscreen

import com.runcheck.ui.chart.FullscreenChartSource

object FullscreenChartSeedStore {
    private var seed: Seed? = null

    fun prime(
        source: FullscreenChartSource,
        state: FullscreenChartUiState,
    ) {
        if (state is FullscreenChartUiState.Success || state is FullscreenChartUiState.Empty) {
            seed = Seed(source = source, state = state)
        }
    }

    fun take(selection: FullscreenChartSelection): FullscreenChartUiState? {
        val current = seed
        seed = null
        if (current?.source != selection.source) return null
        return when (val state = current.state) {
            is FullscreenChartUiState.Success -> {
                state.takeIf { it.selection == selection }
            }

            is FullscreenChartUiState.Empty -> {
                state.takeIf { it.selection == selection }
            }

            else -> {
                null
            }
        }
    }

    fun clear() {
        seed = null
    }

    private data class Seed(
        val source: FullscreenChartSource,
        val state: FullscreenChartUiState,
    )
}
