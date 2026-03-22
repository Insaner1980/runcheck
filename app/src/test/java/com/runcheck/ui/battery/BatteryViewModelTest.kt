package com.runcheck.ui.battery

import androidx.lifecycle.SavedStateHandle
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.usecase.BatteryScreenInsightsUseCase
import com.runcheck.domain.usecase.ChargerSessionTracker
import com.runcheck.domain.usecase.BatteryStatistics
import com.runcheck.domain.usecase.GetBatteryHistoryUseCase
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetBatteryStatisticsUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BatteryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getBatteryState: GetBatteryStateUseCase = mockk()
    private val getBatteryHistory: GetBatteryHistoryUseCase = mockk()
    private val getBatteryStatistics: GetBatteryStatisticsUseCase = mockk()
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private val chargerSessionTracker: ChargerSessionTracker = mockk(relaxed = true)
    private val batteryScreenInsights: BatteryScreenInsightsUseCase = mockk(relaxed = true)
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk(relaxed = true)
    private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase = mockk(relaxed = true)
    private lateinit var viewModel: BatteryViewModel

    private fun makeBatteryState(
        level: Int = 75,
        currentMa: Int = -400,
        chargingStatus: ChargingStatus = ChargingStatus.DISCHARGING,
        confidence: Confidence = Confidence.HIGH
    ) = BatteryState(
        level = level,
        voltageMv = 3900,
        temperatureC = 30f,
        currentMa = MeasuredValue(value = currentMa, confidence = confidence),
        chargingStatus = chargingStatus,
        plugType = if (chargingStatus == ChargingStatus.CHARGING) PlugType.USB else PlugType.NONE,
        health = BatteryHealth.GOOD,
        technology = "Li-ion"
    )

    private val testHistory = listOf(
        BatteryReading(
            id = 1,
            timestamp = System.currentTimeMillis() - 3_600_000,
            level = 80,
            voltageMv = 4000,
            temperatureC = 29f,
            currentMa = -350,
            currentConfidence = "HIGH",
            status = "DISCHARGING",
            plugType = "NONE",
            health = "GOOD",
            cycleCount = null,
            healthPct = null
        )
    )

    @Before
    fun setup() {
        every { getBatteryHistory(any()) } returns flowOf(testHistory)
        coEvery { getBatteryStatistics(any()) } returns BatteryStatistics(
            periodDays = 10,
            totalChargedPct = 200f,
            totalDischargedPct = 180f,
            chargeSessions = 5,
            avgDrainRatePctPerHour = 3.5f,
            fullChargeEstimateHours = 28.5f,
            readingCount = 100
        )
        every { observeProAccess() } returns flowOf(false)
        every { manageUserPreferences.observePreferences() } returns flowOf(UserPreferences())
        every { manageInfoCardDismissals.observeDismissedCardIds() } returns flowOf(emptySet())
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.stopObserving()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): BatteryViewModel {
        return BatteryViewModel(
            savedStateHandle = savedStateHandle,
            getBatteryState = getBatteryState,
            getBatteryHistory = getBatteryHistory,
            getBatteryStatistics = getBatteryStatistics,
            observeProAccess = observeProAccess,
            chargerSessionTracker = chargerSessionTracker,
            batteryScreenInsights = batteryScreenInsights,
            manageUserPreferences = manageUserPreferences,
            manageInfoCardDismissals = manageInfoCardDismissals
        )
    }

    @Test
    fun `initial state is Loading`() {
        every { getBatteryState() } returns flowOf(makeBatteryState())
        viewModel = createViewModel()
        assertEquals(BatteryUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `battery data loads into Success with correct values`() = runTest(mainDispatcherRule.testDispatcher) {
        val testState = makeBatteryState(level = 72, currentMa = -500)
        every { getBatteryState() } returns flowOf(testState)

        viewModel = createViewModel()
        viewModel.startObserving()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success but got $state", state is BatteryUiState.Success)
        val success = state as BatteryUiState.Success

        assertEquals(72, success.batteryState.level)
        assertEquals(-500, success.batteryState.currentMa.value)
        assertEquals(ChargingStatus.DISCHARGING, success.batteryState.chargingStatus)
        assertEquals(testHistory, success.history)
        assertEquals(HistoryPeriod.DAY, success.selectedPeriod)
        assertNotNull("Statistics should be loaded", success.statistics)

        verify { batteryScreenInsights.updateChargingStatus(ChargingStatus.DISCHARGING) }
    }

    @Test
    fun `current stats accumulate correctly over multiple emissions`() = runTest(mainDispatcherRule.testDispatcher) {
        val batteryFlow = MutableSharedFlow<BatteryState>()
        every { getBatteryState() } returns batteryFlow

        viewModel = createViewModel()
        viewModel.startObserving()
        advanceUntilIdle()

        // First emission: no stats yet (sampleCount < 2)
        batteryFlow.emit(makeBatteryState(currentMa = -300))
        advanceUntilIdle()

        val state1 = viewModel.uiState.value as BatteryUiState.Success
        assertNull("Stats should be null with only 1 sample", state1.currentStats)

        // Second emission: stats should appear
        batteryFlow.emit(makeBatteryState(currentMa = -500))
        advanceUntilIdle()

        val state2 = viewModel.uiState.value as BatteryUiState.Success
        assertNotNull("Stats should exist after 2 samples", state2.currentStats)
        val stats = state2.currentStats
        requireNotNull(stats)
        assertEquals(-400, stats.avg) // (-300 + -500) / 2
        assertEquals(-500, stats.min) // min of -300 and -500
        assertEquals(-300, stats.max) // max of -300 and -500
        assertEquals(2, stats.sampleCount)

        // Third emission: verify accumulation continues
        batteryFlow.emit(makeBatteryState(currentMa = -200))
        advanceUntilIdle()

        val state3 = viewModel.uiState.value as BatteryUiState.Success
        val stats3 = state3.currentStats
        requireNotNull(stats3)
        assertEquals(3, stats3.sampleCount)
        assertEquals(-500, stats3.min)
        assertEquals(-200, stats3.max)
        // avg = (-300 + -500 + -200) / 3 = -333
        assertEquals(-333, stats3.avg)
    }

    @Test
    fun `stats reset on charging status change`() = runTest(mainDispatcherRule.testDispatcher) {
        val batteryFlow = MutableSharedFlow<BatteryState>()
        every { getBatteryState() } returns batteryFlow

        viewModel = createViewModel()
        viewModel.startObserving()
        advanceUntilIdle()

        // Emit several discharging readings
        batteryFlow.emit(makeBatteryState(currentMa = -300, chargingStatus = ChargingStatus.DISCHARGING))
        advanceUntilIdle()
        batteryFlow.emit(makeBatteryState(currentMa = -500, chargingStatus = ChargingStatus.DISCHARGING))
        advanceUntilIdle()

        val stateBefore = viewModel.uiState.value as BatteryUiState.Success
        assertNotNull("Stats should exist before reset", stateBefore.currentStats)

        // Switch to charging: stats should reset
        batteryFlow.emit(makeBatteryState(currentMa = 1500, chargingStatus = ChargingStatus.CHARGING))
        advanceUntilIdle()

        val stateAfterSwitch = viewModel.uiState.value as BatteryUiState.Success
        assertNull(
            "Stats should be null right after status change (only 1 sample since reset)",
            stateAfterSwitch.currentStats
        )

        // Second charging sample: stats should reappear with only charging data
        batteryFlow.emit(makeBatteryState(currentMa = 1800, chargingStatus = ChargingStatus.CHARGING))
        advanceUntilIdle()

        val stateCharging = viewModel.uiState.value as BatteryUiState.Success
        assertNotNull("Stats should exist after 2 charging samples", stateCharging.currentStats)
        val stats = stateCharging.currentStats
        requireNotNull(stats)
        assertEquals(1500, stats.min)
        assertEquals(1800, stats.max)
        assertEquals(2, stats.sampleCount)
    }

    @Test
    fun `setHistoryPeriod triggers reload with new period`() = runTest(mainDispatcherRule.testDispatcher) {
        every { getBatteryState() } returns flowOf(makeBatteryState())

        viewModel = createViewModel()
        viewModel.startObserving()
        advanceUntilIdle()

        // Default period is DAY
        val state1 = viewModel.uiState.value as BatteryUiState.Success
        assertEquals(HistoryPeriod.DAY, state1.selectedPeriod)

        // Change to WEEK
        viewModel.setHistoryPeriod(HistoryPeriod.WEEK)
        advanceUntilIdle()

        val state2 = viewModel.uiState.value as BatteryUiState.Success
        assertEquals(HistoryPeriod.WEEK, state2.selectedPeriod)

        // Verify that getBatteryHistory was called with WEEK
        verify { getBatteryHistory(HistoryPeriod.WEEK) }
    }

    @Test
    fun `setHistoryPeriod writes selected period to saved state`() = runTest(mainDispatcherRule.testDispatcher) {
        every { getBatteryState() } returns flowOf(makeBatteryState())

        val savedStateHandle = SavedStateHandle()
        viewModel = createViewModel(savedStateHandle = savedStateHandle)
        viewModel.startObserving()
        advanceUntilIdle()

        viewModel.setHistoryPeriod(HistoryPeriod.MONTH)

        assertEquals(HistoryPeriod.MONTH.name, savedStateHandle.get<String>("battery_selected_period"))
    }
}
