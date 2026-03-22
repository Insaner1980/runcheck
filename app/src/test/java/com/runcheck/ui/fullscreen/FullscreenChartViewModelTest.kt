package com.runcheck.ui.fullscreen

import androidx.lifecycle.SavedStateHandle
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.usecase.GetBatteryHistoryUseCase
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetNetworkHistoryUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FullscreenChartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getBatteryHistory: GetBatteryHistoryUseCase = mockk()
    private val getBatteryState: GetBatteryStateUseCase = mockk()
    private val getNetworkHistory: GetNetworkHistoryUseCase = mockk()
    private val isProUser: IsProUserUseCase = mockk()
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk()

    private val batteryHistory = listOf(
        BatteryReading(
            id = 1,
            timestamp = 1_000L,
            level = 80,
            voltageMv = 4_100,
            temperatureC = 30f,
            currentMa = 900,
            currentConfidence = "HIGH",
            status = "CHARGING",
            plugType = "USB",
            health = "GOOD",
            cycleCount = null,
            healthPct = null
        )
    )

    private val networkHistory = listOf(
        NetworkReading(
            timestamp = 1_000L,
            type = "WIFI",
            signalDbm = -55,
            wifiSpeedMbps = 866,
            wifiFrequency = 5_180,
            carrier = null,
            networkSubtype = null,
            latencyMs = 28
        )
    )

    private val batteryState = BatteryState(
        level = 84,
        voltageMv = 4_120,
        temperatureC = 32f,
        currentMa = MeasuredValue(value = 1_250, confidence = Confidence.HIGH),
        chargingStatus = ChargingStatus.CHARGING,
        plugType = PlugType.USB,
        health = BatteryHealth.GOOD,
        technology = "Li-ion"
    )

    @Before
    fun setup() {
        every { getBatteryHistory(any()) } returns flowOf(batteryHistory)
        every { getBatteryState() } returns flowOf(batteryState)
        every { getNetworkHistory(any()) } returns flowOf(networkHistory)
        every { isProUser() } returns true
        every { observeProAccess() } returns flowOf(true)
        every { manageUserPreferences.observePreferences() } returns flowOf(
            com.runcheck.domain.model.UserPreferences()
        )
        FullscreenChartSeedStore.clear()
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle
    ): FullscreenChartViewModel {
        return FullscreenChartViewModel(
            savedStateHandle = savedStateHandle,
            getBatteryHistory = getBatteryHistory,
            getBatteryState = getBatteryState,
            getNetworkHistory = getNetworkHistory,
            isProUser = isProUser,
            observeProAccess = observeProAccess,
            manageUserPreferences = manageUserPreferences
        )
    }

    @Test
    fun `selected metric and period are written back to saved state`() = runTest(mainDispatcherRule.testDispatcher) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "source" to FullscreenChartSource.BATTERY_HISTORY.name,
                "metric" to BatteryHistoryMetric.LEVEL.name,
                "period" to HistoryPeriod.DAY.name
            )
        )

        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        viewModel.setMetric(BatteryHistoryMetric.VOLTAGE.name)
        viewModel.setPeriod(HistoryPeriod.WEEK.name)
        advanceUntilIdle()

        assertEquals(FullscreenChartSource.BATTERY_HISTORY.name, savedStateHandle.get<String>("source"))
        assertEquals(BatteryHistoryMetric.VOLTAGE.name, savedStateHandle.get<String>("metric"))
        assertEquals(HistoryPeriod.WEEK.name, savedStateHandle.get<String>("period"))
    }

    @Test
    fun `new ViewModel from same SavedStateHandle restores battery history selections after process death`() = runTest(mainDispatcherRule.testDispatcher) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "source" to FullscreenChartSource.BATTERY_HISTORY.name,
                "metric" to BatteryHistoryMetric.LEVEL.name,
                "period" to HistoryPeriod.DAY.name
            )
        )

        val firstViewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        firstViewModel.setMetric(BatteryHistoryMetric.VOLTAGE.name)
        firstViewModel.setPeriod(HistoryPeriod.WEEK.name)
        advanceUntilIdle()

        val restoredViewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        val state = restoredViewModel.uiState.value
        assertTrue("Expected Empty state after restore, got $state", state is FullscreenChartUiState.Empty)
        state as FullscreenChartUiState.Empty
        assertEquals(FullscreenChartSource.BATTERY_HISTORY, restoredViewModel.source)
        assertEquals(BatteryHistoryMetric.VOLTAGE.name, state.selectedMetric)
        assertEquals(HistoryPeriod.WEEK.name, state.selectedPeriod)
    }

    @Test
    fun `battery session source metric and period restore from saved state`() = runTest(mainDispatcherRule.testDispatcher) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "source" to FullscreenChartSource.BATTERY_SESSION.name,
                "metric" to SessionGraphMetric.POWER.name,
                "period" to SessionGraphWindow.THIRTY_MINUTES.name
            )
        )

        val restoredViewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        val state = restoredViewModel.uiState.value
        assertTrue("Expected Empty state after restore, got $state", state is FullscreenChartUiState.Empty)
        state as FullscreenChartUiState.Empty
        assertEquals(FullscreenChartSource.BATTERY_SESSION, restoredViewModel.source)
        assertEquals(SessionGraphMetric.POWER.name, state.selectedMetric)
        assertEquals(SessionGraphWindow.THIRTY_MINUTES.name, state.selectedPeriod)
    }

    @Test
    fun `network history source metric and period restore from saved state`() = runTest(mainDispatcherRule.testDispatcher) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "source" to FullscreenChartSource.NETWORK_HISTORY.name,
                "metric" to NetworkHistoryMetric.LATENCY.name,
                "period" to HistoryPeriod.MONTH.name
            )
        )

        val restoredViewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        val state = restoredViewModel.uiState.value
        assertTrue("Expected Empty state after restore, got $state", state is FullscreenChartUiState.Empty)
        state as FullscreenChartUiState.Empty
        assertEquals(FullscreenChartSource.NETWORK_HISTORY, restoredViewModel.source)
        assertEquals(NetworkHistoryMetric.LATENCY.name, state.selectedMetric)
        assertEquals(HistoryPeriod.MONTH.name, state.selectedPeriod)
    }

    @Test
    fun `pro unlock while screen is visible reloads locked chart without manual retry`() = runTest(mainDispatcherRule.testDispatcher) {
        val proAccess = MutableSharedFlow<Boolean>(replay = 1)
        proAccess.tryEmit(false)

        every { isProUser() } returns false
        every { observeProAccess() } returns proAccess

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "source" to FullscreenChartSource.BATTERY_HISTORY.name,
                "metric" to BatteryHistoryMetric.LEVEL.name,
                "period" to HistoryPeriod.DAY.name
            )
        )

        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()
        assertEquals(FullscreenChartUiState.Locked, viewModel.uiState.value)

        proAccess.emit(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Empty state after unlock, got $state", state is FullscreenChartUiState.Empty)
    }

    @Test
    fun `missing source falls back to battery session and persists sanitized source`() = runTest(mainDispatcherRule.testDispatcher) {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "metric" to SessionGraphMetric.CURRENT.name,
                "period" to SessionGraphWindow.ALL.name
            )
        )

        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()

        assertEquals(FullscreenChartSource.BATTERY_SESSION, viewModel.source)
        assertEquals(FullscreenChartSource.BATTERY_SESSION.name, savedStateHandle.get<String>("source"))
    }

    @Test
    fun `matching seed is shown immediately before background refresh`() = runTest(mainDispatcherRule.testDispatcher) {
        FullscreenChartSeedStore.prime(
            source = FullscreenChartSource.NETWORK_HISTORY,
            state = FullscreenChartUiState.Success(
                chartData = listOf(-80f, -70f, -65f),
                chartTimestamps = listOf(1_000L, 2_000L, 3_000L),
                unit = " dBm",
                selectedMetric = NetworkHistoryMetric.SIGNAL.name,
                selectedPeriod = HistoryPeriod.DAY.name,
                metricOptions = NetworkHistoryMetric.entries.map { it.name },
                periodOptions = listOf(HistoryPeriod.DAY.name, HistoryPeriod.WEEK.name),
                yLabels = emptyList(),
                xLabels = emptyList()
            )
        )
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "source" to FullscreenChartSource.NETWORK_HISTORY.name,
                "metric" to NetworkHistoryMetric.SIGNAL.name,
                "period" to HistoryPeriod.DAY.name
            )
        )

        val viewModel = createViewModel(savedStateHandle)

        val immediateState = viewModel.uiState.value
        assertTrue(immediateState is FullscreenChartUiState.Success)
        immediateState as FullscreenChartUiState.Success
        assertEquals(listOf(-80f, -70f, -65f), immediateState.chartData)

        advanceUntilIdle()

        val refreshedState = viewModel.uiState.value
        assertTrue(refreshedState is FullscreenChartUiState.Empty)
    }

    @Test
    fun `battery history fullscreen chart keeps updating while visible`() = runTest(mainDispatcherRule.testDispatcher) {
        val liveHistory = MutableStateFlow(
            listOf(
                batteryHistory.first()
            )
        )
        every { getBatteryHistory(any()) } returns liveHistory

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "source" to FullscreenChartSource.BATTERY_HISTORY.name,
                "metric" to BatteryHistoryMetric.LEVEL.name,
                "period" to HistoryPeriod.DAY.name
            )
        )

        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()
        assertTrue(
            "Expected Empty after initial single-point battery history, got ${viewModel.uiState.value}",
            viewModel.uiState.value is FullscreenChartUiState.Empty
        )

        liveHistory.value = listOf(
            batteryHistory.first(),
            batteryHistory.first().copy(
                id = 2,
                timestamp = 1_000L,
                level = 82
            )
        )
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertTrue("Expected Success after live history update, got $updatedState", updatedState is FullscreenChartUiState.Success)
        updatedState as FullscreenChartUiState.Success
        assertEquals(listOf(80f, 82f), updatedState.chartData)
    }

    @Test
    fun `network history fullscreen chart keeps updating while visible`() = runTest(mainDispatcherRule.testDispatcher) {
        val liveHistory = MutableStateFlow(
            listOf(
                networkHistory.first()
            )
        )
        every { getNetworkHistory(any()) } returns liveHistory

        val savedStateHandle = SavedStateHandle(
            mapOf(
                "source" to FullscreenChartSource.NETWORK_HISTORY.name,
                "metric" to NetworkHistoryMetric.SIGNAL.name,
                "period" to HistoryPeriod.DAY.name
            )
        )

        val viewModel = createViewModel(savedStateHandle)
        advanceUntilIdle()
        assertTrue(
            "Expected Empty after initial single-point network history, got ${viewModel.uiState.value}",
            viewModel.uiState.value is FullscreenChartUiState.Empty
        )

        liveHistory.value = listOf(
            networkHistory.first(),
            networkHistory.first().copy(
                timestamp = 1_000L,
                signalDbm = -61
            )
        )
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertTrue("Expected Success after live network update, got $updatedState", updatedState is FullscreenChartUiState.Success)
        updatedState as FullscreenChartUiState.Success
        assertEquals(listOf(-55f, -61f), updatedState.chartData)
    }
}
