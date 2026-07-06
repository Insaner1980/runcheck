package com.runcheck.ui.thermal

import androidx.lifecycle.SavedStateHandle
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.usecase.GetThermalHistoryUseCase
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.domain.usecase.GetThrottlingHistoryUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.ui.common.UiText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThermalViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getThermalState: GetThermalStateUseCase = mockk()
    private val getThrottlingHistory: GetThrottlingHistoryUseCase = mockk()
    private val getThermalHistory: GetThermalHistoryUseCase = mockk()
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk(relaxed = true)
    private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase = mockk(relaxed = true)
    private lateinit var viewModel: ThermalViewModel

    @Before
    fun setup() {
        every { getThrottlingHistory() } returns flowOf(emptyList())
        every { getThermalHistory(any()) } returns flowOf(emptyList())
        every { observeProAccess() } returns flowOf(true)
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

    @Test
    fun `startObserving emits success state with live thermal buffers and session bounds`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val thermalFlow = MutableStateFlow(thermalState(tempC = 34f, headroom = 0.6f))
            every { getThermalState() } returns thermalFlow
            viewModel = createViewModel()

            try {
                viewModel.startObserving()
                advanceThermalSample()
                thermalFlow.value = thermalState(tempC = 38f, headroom = 0.4f)
                advanceThermalSample()

                val state = viewModel.uiState.value
                assertTrue("Expected Success but got $state", state is ThermalUiState.Success)
                val success = state as ThermalUiState.Success
                assertEquals(34f, success.sessionMinTemp ?: 0f, 0.01f)
                assertEquals(38f, success.sessionMaxTemp ?: 0f, 0.01f)
                assertEquals(listOf(34f, 38f), success.liveTempC)
                assertEquals(listOf(0.6f, 0.4f), success.liveHeadroom)
                assertTrue(success.isPro)
            } finally {
                viewModel.stopObserving()
            }
        }

    @Test
    fun `history period selection persists and reloads history`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val thermalFlow = MutableStateFlow(thermalState(tempC = 35f))
            val weekReadings = listOf(thermalReading(timestamp = 1L, tempC = 36f))
            every { getThermalState() } returns thermalFlow
            every { getThermalHistory(HistoryPeriod.WEEK) } returns flowOf(weekReadings)
            val savedStateHandle = SavedStateHandle()
            viewModel = createViewModel(savedStateHandle)

            try {
                viewModel.startObserving()
                advanceThermalSample()
                viewModel.setHistoryPeriod(HistoryPeriod.WEEK)
                runCurrent()

                val success = viewModel.uiState.value as ThermalUiState.Success
                assertEquals(HistoryPeriod.WEEK, success.selectedHistoryPeriod)
                assertEquals(HistoryPeriod.WEEK.name, savedStateHandle["thermal_selected_history_period"])
                assertEquals(weekReadings, success.thermalHistory)
            } finally {
                viewModel.stopObserving()
            }
        }

    @Test
    fun `thermal state collection failure emits error state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getThermalState() } returns flow { throw IllegalStateException("thermal failed") }
            viewModel = createViewModel()

            try {
                viewModel.startObserving()
                runCurrent()

                assertEquals(ThermalUiState.Error(UiText.Dynamic("thermal failed")), viewModel.uiState.value)
            } finally {
                viewModel.stopObserving()
            }
        }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): ThermalViewModel =
        ThermalViewModel(
            savedStateHandle = savedStateHandle,
            getThermalState = getThermalState,
            getThrottlingHistory = getThrottlingHistory,
            getThermalHistory = getThermalHistory,
            observeProAccess = observeProAccess,
            manageUserPreferences = manageUserPreferences,
            manageInfoCardDismissals = manageInfoCardDismissals,
        )

    private fun advanceThermalSample() {
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(334L)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
    }

    private fun thermalState(
        tempC: Float,
        headroom: Float? = null,
    ): ThermalState =
        ThermalState(
            batteryTempC = tempC,
            cpuTempC = null,
            thermalHeadroom = headroom,
            thermalStatus = ThermalStatus.NONE,
            isThrottling = false,
        )

    private fun thermalReading(
        timestamp: Long,
        tempC: Float,
    ): ThermalReading =
        ThermalReading(
            timestamp = timestamp,
            batteryTempC = tempC,
            cpuTempC = null,
            thermalStatus = 0,
            throttling = false,
        )
}
