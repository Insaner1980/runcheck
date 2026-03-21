package com.runcheck.ui.fullscreen

import androidx.lifecycle.SavedStateHandle
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.usecase.GetBatteryHistoryUseCase
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetNetworkHistoryUseCase
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.FullscreenChartSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FullscreenChartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getBatteryHistory: GetBatteryHistoryUseCase = mockk()
    private val getBatteryState: GetBatteryStateUseCase = mockk()
    private val getNetworkHistory: GetNetworkHistoryUseCase = mockk()

    private val batteryHistory = listOf(
        BatteryReading(
            id = 1,
            timestamp = 1_000L,
            level = 80,
            voltageMv = 4_100,
            temperatureC = 30f,
            currentMa = -300,
            currentConfidence = "HIGH",
            status = "DISCHARGING",
            plugType = "NONE",
            health = "GOOD",
            cycleCount = null,
            healthPct = null
        ),
        BatteryReading(
            id = 2,
            timestamp = 2_000L,
            level = 78,
            voltageMv = 4_050,
            temperatureC = 31f,
            currentMa = -350,
            currentConfidence = "HIGH",
            status = "DISCHARGING",
            plugType = "NONE",
            health = "GOOD",
            cycleCount = null,
            healthPct = null
        )
    )

    @Test
    fun `selected metric and period are written back to saved state`() = runTest(mainDispatcherRule.testDispatcher) {
        every { getBatteryHistory(any()) } returns flowOf(batteryHistory)
        every { getBatteryState() } returns emptyFlow()
        every { getNetworkHistory(any()) } returns flowOf(emptyList())

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "source" to FullscreenChartSource.BATTERY_HISTORY.name,
                "metric" to BatteryHistoryMetric.LEVEL.name,
                "period" to com.runcheck.domain.model.HistoryPeriod.DAY.name
            )
        )

        val firstViewModel = FullscreenChartViewModel(
            savedStateHandle = savedStateHandle,
            getBatteryHistory = getBatteryHistory,
            getBatteryState = getBatteryState,
            getNetworkHistory = getNetworkHistory
        )
        advanceUntilIdle()

        firstViewModel.setMetric(BatteryHistoryMetric.VOLTAGE.name)
        firstViewModel.setPeriod(com.runcheck.domain.model.HistoryPeriod.WEEK.name)
        advanceUntilIdle()

        assertEquals(BatteryHistoryMetric.VOLTAGE.name, savedStateHandle.get<String>("metric"))
        assertEquals(com.runcheck.domain.model.HistoryPeriod.WEEK.name, savedStateHandle.get<String>("period"))
    }
}
