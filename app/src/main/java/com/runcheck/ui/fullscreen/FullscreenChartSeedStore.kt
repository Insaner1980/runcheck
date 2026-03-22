package com.runcheck.ui.fullscreen

import com.runcheck.ui.chart.FullscreenChartSource

object FullscreenChartSeedStore {
    private var seed: Seed? = null

    fun prime(source: FullscreenChartSource, state: FullscreenChartUiState) {
        if (state is FullscreenChartUiState.Success || state is FullscreenChartUiState.Empty) {
            seed = Seed(source = source, state = state)
        }
    }

    fun take(
        source: FullscreenChartSource,
        metric: String,
        period: String
    ): FullscreenChartUiState? {
        val current = seed
        seed = null
        if (current?.source != source) return null
        return when (val state = current.state) {
            is FullscreenChartUiState.Success ->
                state.takeIf { it.selectedMetric == metric && it.selectedPeriod == period }
            is FullscreenChartUiState.Empty ->
                state.takeIf { it.selectedMetric == metric && it.selectedPeriod == period }
            else -> null
        }
    }

    fun clear() {
        seed = null
    }

    private data class Seed(
        val source: FullscreenChartSource,
        val state: FullscreenChartUiState
    )
}
