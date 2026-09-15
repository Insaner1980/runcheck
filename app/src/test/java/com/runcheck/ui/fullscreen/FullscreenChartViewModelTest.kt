package com.runcheck.ui.fullscreen

import android.text.format.DateFormat
import androidx.lifecycle.SavedStateHandle
import com.runcheck.R
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.usecase.GetBatteryHistoryUseCase
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetNetworkHistoryUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.chart.ChartRenderModel
import com.runcheck.ui.chart.FullscreenChartSource
import com.runcheck.ui.chart.MAX_FULLSCREEN_CHART_POINTS
import com.runcheck.ui.chart.MAX_FULLSCREEN_SESSION_POINTS
import com.runcheck.ui.chart.NetworkHistoryMetric
import com.runcheck.ui.chart.NetworkSignalContext
import com.runcheck.ui.chart.NetworkSignalFamily
import com.runcheck.ui.chart.SessionGraphMetric
import com.runcheck.ui.chart.SessionGraphWindow
import com.runcheck.ui.chart.buildBatteryHistoryChartModel
import com.runcheck.ui.chart.buildBatterySessionChartModel
import com.runcheck.ui.chart.buildNetworkHistoryChartModel
import com.runcheck.ui.chart.calculateChargingSessionSummary
import com.runcheck.ui.chart.formatChartTooltip
import com.runcheck.ui.chart.networkSignalHistoryContextResource
import com.runcheck.ui.components.ChartXLabel
import com.runcheck.ui.components.ChartYLabel
import com.runcheck.ui.navigation.Screen
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FullscreenChartViewModelTest {
    @Before
    fun setUpDateFormatting() {
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } returns "MMM d HH:mm"
    }

    @After
    fun tearDownDateFormatting() {
        unmockkStatic(DateFormat::class)
    }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getBatteryHistory: GetBatteryHistoryUseCase = mockk()
    private val getBatteryState: GetBatteryStateUseCase = mockk()
    private val getNetworkHistory: GetNetworkHistoryUseCase = mockk()
    private val isProUser: IsProUserUseCase = mockk()
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk()

    private val batteryHistory =
        listOf(
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
                healthPct = null,
            ),
        )

    private val networkHistory =
        listOf(
            NetworkReading(
                timestamp = 1_000L,
                type = "WIFI",
                signalDbm = -55,
                wifiSpeedMbps = 866,
                wifiFrequency = 5_180,
                carrier = null,
                networkSubtype = null,
                latencyMs = 28,
            ),
        )

    private val batteryState =
        BatteryState(
            level = 84,
            voltageMv = 4_120,
            temperatureC = 32f,
            currentMa = MeasuredValue(value = 1_250, confidence = Confidence.HIGH),
            chargingStatus = ChargingStatus.CHARGING,
            plugType = PlugType.USB,
            health = BatteryHealth.GOOD,
            technology = "Li-ion",
        )

    @Before
    fun setup() {
        every { getBatteryHistory(any()) } returns flowOf(batteryHistory)
        every { getBatteryState() } returns flowOf(batteryState)
        every { getNetworkHistory(any()) } returns flowOf(networkHistory)
        every { isProUser() } returns true
        every { observeProAccess() } returns flowOf(true)
        every { manageUserPreferences.observePreferences() } returns
            flowOf(
                com.runcheck.domain.model
                    .UserPreferences(),
            )
        FullscreenChartSeedStore.clear()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle): FullscreenChartViewModel =
        FullscreenChartViewModel(
            savedStateHandle = savedStateHandle,
            getBatteryHistory = getBatteryHistory,
            getBatteryState = getBatteryState,
            getNetworkHistory = getNetworkHistory,
            isProUser = isProUser,
            observeProAccess = observeProAccess,
            manageUserPreferences = manageUserPreferences,
        )

    private fun batteryHistorySavedState(): SavedStateHandle =
        SavedStateHandle(
            mapOf(
                Screen.FullscreenChart.ARG_SOURCE to FullscreenChartSource.BATTERY_HISTORY.name,
                Screen.FullscreenChart.ARG_METRIC to BatteryHistoryMetric.LEVEL.name,
                Screen.FullscreenChart.ARG_PERIOD to HistoryPeriod.DAY.name,
            ),
        )

    @Test
    fun `fullscreen route arguments restore source metric and period`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        Screen.FullscreenChart.ARG_SOURCE to FullscreenChartSource.BATTERY_HISTORY.name,
                        Screen.FullscreenChart.ARG_METRIC to BatteryHistoryMetric.VOLTAGE.name,
                        Screen.FullscreenChart.ARG_PERIOD to HistoryPeriod.WEEK.name,
                    ),
                )
            val viewModel = createViewModel(savedStateHandle)

            assertEquals(
                FullscreenChartSelection.BatteryHistory(
                    BatteryHistoryMetric.VOLTAGE,
                    HistoryPeriod.WEEK,
                ),
                viewModel.selection,
            )
        }

    @Test
    fun `selected metric and period are written back to saved state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val savedStateHandle = batteryHistorySavedState()

            val viewModel = createViewModel(savedStateHandle)
            advanceUntilIdle()

            viewModel.setMetric(BatteryHistoryMetric.VOLTAGE)
            viewModel.setPeriod(HistoryPeriod.WEEK)
            advanceUntilIdle()

            assertEquals(
                FullscreenChartSource.BATTERY_HISTORY.name,
                savedStateHandle.get<String>(Screen.FullscreenChart.ARG_SOURCE),
            )
            assertEquals(
                BatteryHistoryMetric.VOLTAGE.name,
                savedStateHandle.get<String>(Screen.FullscreenChart.ARG_METRIC),
            )
            assertEquals(
                HistoryPeriod.WEEK.name,
                savedStateHandle.get<String>(Screen.FullscreenChart.ARG_PERIOD),
            )
        }

    @Test
    fun `new ViewModel from same SavedStateHandle restores battery history selections after process death`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val savedStateHandle = batteryHistorySavedState()

            val firstViewModel = createViewModel(savedStateHandle)
            advanceUntilIdle()

            firstViewModel.setMetric(BatteryHistoryMetric.VOLTAGE)
            firstViewModel.setPeriod(HistoryPeriod.WEEK)
            advanceUntilIdle()

            val restoredViewModel = createViewModel(savedStateHandle)
            advanceUntilIdle()

            val state = restoredViewModel.uiState.value
            assertTrue("Expected Empty state after restore, got $state", state is FullscreenChartUiState.Empty)
            state as FullscreenChartUiState.Empty
            assertEquals(FullscreenChartSource.BATTERY_HISTORY, restoredViewModel.source)
            assertEquals(
                FullscreenChartSelection.BatteryHistory(BatteryHistoryMetric.VOLTAGE, HistoryPeriod.WEEK),
                state.selection,
            )
        }

    @Test
    fun `battery session source metric and period restore from saved state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        Screen.FullscreenChart.ARG_SOURCE to FullscreenChartSource.BATTERY_SESSION.name,
                        Screen.FullscreenChart.ARG_METRIC to SessionGraphMetric.POWER.name,
                        Screen.FullscreenChart.ARG_PERIOD to SessionGraphWindow.THIRTY_MINUTES.name,
                    ),
                )

            val restoredViewModel = createViewModel(savedStateHandle)
            advanceUntilIdle()

            val state = restoredViewModel.uiState.value
            assertTrue("Expected Empty state after restore, got $state", state is FullscreenChartUiState.Empty)
            state as FullscreenChartUiState.Empty
            assertEquals(FullscreenChartSource.BATTERY_SESSION, restoredViewModel.source)
            assertEquals(
                FullscreenChartSelection.BatterySession(SessionGraphMetric.POWER, SessionGraphWindow.THIRTY_MINUTES),
                state.selection,
            )
        }

    @Test
    fun `network history source metric and period restore from saved state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        Screen.FullscreenChart.ARG_SOURCE to FullscreenChartSource.NETWORK_HISTORY.name,
                        Screen.FullscreenChart.ARG_METRIC to NetworkHistoryMetric.LATENCY.name,
                        Screen.FullscreenChart.ARG_PERIOD to HistoryPeriod.MONTH.name,
                    ),
                )

            val restoredViewModel = createViewModel(savedStateHandle)
            advanceUntilIdle()

            val state = restoredViewModel.uiState.value
            assertTrue("Expected Empty state after restore, got $state", state is FullscreenChartUiState.Empty)
            state as FullscreenChartUiState.Empty
            assertEquals(FullscreenChartSource.NETWORK_HISTORY, restoredViewModel.source)
            assertEquals(
                FullscreenChartSelection.NetworkHistory(NetworkHistoryMetric.LATENCY, HistoryPeriod.MONTH),
                state.selection,
            )
        }

    @Test
    fun `pro unlock while screen is visible reloads locked chart without manual retry`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val proAccess = MutableSharedFlow<Boolean>(replay = 1)
            proAccess.tryEmit(false)

            every { isProUser() } returns false
            every { observeProAccess() } returns proAccess

            val savedStateHandle = batteryHistorySavedState()

            val viewModel = createViewModel(savedStateHandle)
            advanceUntilIdle()
            assertEquals(FullscreenChartUiState.Locked, viewModel.uiState.value)

            proAccess.emit(true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("Expected Empty state after unlock, got $state", state is FullscreenChartUiState.Empty)
        }

    @Test
    fun `missing source falls back to battery session and persists sanitized source`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        Screen.FullscreenChart.ARG_METRIC to SessionGraphMetric.CURRENT.name,
                        Screen.FullscreenChart.ARG_PERIOD to SessionGraphWindow.ALL.name,
                    ),
                )

            val viewModel = createViewModel(savedStateHandle)
            advanceUntilIdle()

            assertEquals(FullscreenChartSource.BATTERY_SESSION, viewModel.source)
            assertEquals(
                FullscreenChartSource.BATTERY_SESSION.name,
                savedStateHandle.get<String>(Screen.FullscreenChart.ARG_SOURCE),
            )
        }

    @Test
    fun `matching seed is shown immediately before background refresh`() =
        runTest(mainDispatcherRule.testDispatcher) {
            FullscreenChartSeedStore.prime(
                source = FullscreenChartSource.NETWORK_HISTORY,
                state =
                    FullscreenChartUiState.Success(
                        chartData = listOf(-80f, -70f, -65f),
                        chartTimestamps = listOf(1_000L, 2_000L, 3_000L),
                        unit = " dBm",
                        selection =
                            FullscreenChartSelection.NetworkHistory(
                                NetworkHistoryMetric.SIGNAL,
                                HistoryPeriod.DAY,
                            ),
                        yLabels = emptyList(),
                        xLabels = emptyList(),
                    ),
            )
            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        Screen.FullscreenChart.ARG_SOURCE to FullscreenChartSource.NETWORK_HISTORY.name,
                        Screen.FullscreenChart.ARG_METRIC to NetworkHistoryMetric.SIGNAL.name,
                        Screen.FullscreenChart.ARG_PERIOD to HistoryPeriod.DAY.name,
                    ),
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
    fun `battery history fullscreen chart keeps updating while visible`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val liveHistory =
                MutableStateFlow(
                    listOf(
                        batteryHistory.first(),
                    ),
                )
            every { getBatteryHistory(any()) } returns liveHistory

            val savedStateHandle = batteryHistorySavedState()

            val viewModel = createViewModel(savedStateHandle)
            advanceUntilIdle()
            assertTrue(
                "Expected Empty after initial single-point battery history, got ${viewModel.uiState.value}",
                viewModel.uiState.value is FullscreenChartUiState.Empty,
            )

            liveHistory.value =
                listOf(
                    batteryHistory.first(),
                    batteryHistory.first().copy(
                        id = 2,
                        timestamp = 1_000L,
                        level = 82,
                    ),
                )
            advanceUntilIdle()

            val updatedState = viewModel.uiState.value
            assertTrue(
                "Expected Success after live history update, got $updatedState",
                updatedState is FullscreenChartUiState.Success,
            )
            updatedState as FullscreenChartUiState.Success
            assertEquals(listOf(80f, 82f), updatedState.chartData)
        }

    @Test
    fun `network history fullscreen chart keeps updating while visible`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val liveHistory =
                MutableStateFlow(
                    listOf(
                        networkHistory.first(),
                    ),
                )
            every { getNetworkHistory(any()) } returns liveHistory

            val savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        Screen.FullscreenChart.ARG_SOURCE to FullscreenChartSource.NETWORK_HISTORY.name,
                        Screen.FullscreenChart.ARG_METRIC to NetworkHistoryMetric.SIGNAL.name,
                        Screen.FullscreenChart.ARG_PERIOD to HistoryPeriod.DAY.name,
                    ),
                )

            val viewModel = createViewModel(savedStateHandle)
            advanceUntilIdle()
            assertTrue(
                "Expected Empty after initial single-point network history, got ${viewModel.uiState.value}",
                viewModel.uiState.value is FullscreenChartUiState.Empty,
            )

            liveHistory.value =
                listOf(
                    networkHistory.first(),
                    networkHistory.first().copy(
                        timestamp = 1_000L,
                        signalDbm = -61,
                    ),
                )
            advanceUntilIdle()

            val updatedState = viewModel.uiState.value
            assertTrue(
                "Expected Success after live network update, got $updatedState",
                updatedState is FullscreenChartUiState.Success,
            )
            updatedState as FullscreenChartUiState.Success
            assertEquals(listOf(-55f, -61f), updatedState.chartData)
        }

    @Test
    fun `network fullscreen preserves gaps and context at its point budget`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val history =
                (0..1200).map { index ->
                    networkHistory.first().copy(
                        timestamp = index.toLong(),
                        type = if (index < 600) "WIFI" else "CELLULAR",
                        networkSubtype = if (index < 600) null else "5G",
                        signalDbm = if (index == 300) null else -95,
                    )
                }
            every { getNetworkHistory(any()) } returns flowOf(history)
            val viewModel =
                createViewModel(
                    SavedStateHandle(
                        mapOf(
                            Screen.FullscreenChart.ARG_SOURCE to FullscreenChartSource.NETWORK_HISTORY.name,
                            Screen.FullscreenChart.ARG_METRIC to NetworkHistoryMetric.SIGNAL.name,
                            Screen.FullscreenChart.ARG_PERIOD to HistoryPeriod.DAY.name,
                        ),
                    ),
                )
            advanceUntilIdle()
            val state = viewModel.uiState.value as FullscreenChartUiState.Success
            val expected =
                com.runcheck.ui.chart.buildNetworkHistoryChartModel(
                    history,
                    NetworkHistoryMetric.SIGNAL,
                    HistoryPeriod.DAY,
                    com.runcheck.ui.chart.MAX_FULLSCREEN_CHART_POINTS,
                )
            assertEquals(expected.chartData, state.chartData)
            assertEquals(expected.lineBreakIndices, state.lineBreakIndices)
            assertEquals(expected.networkSignalContexts, state.networkSignalContexts)
            assertEquals(expected.networkSignalFamilies, state.networkSignalFamilies)
            assertEquals(2, state.lineBreakIndices.size)
        }

    @Test
    fun `canonical success mapping preserves every render field and selections`() {
        val model =
            ChartRenderModel(
                chartData = listOf(12.5f, 38.75f),
                chartTimestamps = listOf(123_000L, 987_000L),
                unit = " custom",
                yLabels = listOf(ChartYLabel(12.5f, "low")),
                xLabels = listOf(ChartXLabel(0.75f, "later")),
                tooltipDecimals = 3,
                tooltipTimeSkeleton = "Hms",
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
                lineBreakIndices = setOf(1),
                networkSignalContexts =
                    listOf(
                        NetworkSignalContext(ConnectionType.WIFI, null),
                        NetworkSignalContext(ConnectionType.CELLULAR, "5G"),
                    ),
                networkSignalFamilies = setOf(NetworkSignalFamily.WIFI, NetworkSignalFamily.FIVE_G),
            )
        val selection = FullscreenChartSelection.BatteryHistory(BatteryHistoryMetric.TEMPERATURE, HistoryPeriod.MONTH)
        val state = model.toFullscreenSuccess(selection)
        assertRenderParity(model, state)
        assertEquals(selection, state.selection)
    }

    @Test
    fun `battery temperature seed and loaded history have identical render content`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val history =
                listOf(batteryHistory.first(), batteryHistory.first().copy(timestamp = 61_000L, temperatureC = 41f))
            every { getBatteryHistory(any()) } returns flowOf(history)
            val model =
                buildBatteryHistoryChartModel(
                    history,
                    BatteryHistoryMetric.TEMPERATURE,
                    HistoryPeriod.WEEK,
                    TemperatureUnit.CELSIUS,
                    MAX_FULLSCREEN_CHART_POINTS,
                )
            assertSeedAndLoadParity(
                model,
                FullscreenChartSelection.BatteryHistory(BatteryHistoryMetric.TEMPERATURE, HistoryPeriod.WEEK),
            )
            assertEquals("°C", model.unit)
            assertEquals(TemperatureUnit.CELSIUS, model.temperatureUnit)
        }

    @Test
    fun `free battery power session seed and loaded data preserve gaps and selections`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns false
            val history =
                listOf(
                    batteryHistory.first(),
                    batteryHistory.first().copy(timestamp = 901_000L, level = 82),
                    batteryHistory.first().copy(timestamp = 3_601_000L, level = 84),
                )
            every { getBatteryHistory(any()) } returns flowOf(history)
            val summary =
                requireNotNull(
                    calculateChargingSessionSummary(history, batteryState.level, batteryState.chargingStatus),
                )
            val model =
                buildBatterySessionChartModel(
                    summary,
                    SessionGraphMetric.POWER,
                    SessionGraphWindow.ALL,
                    MAX_FULLSCREEN_SESSION_POINTS,
                )
            assertSeedAndLoadParity(
                model,
                FullscreenChartSelection.BatterySession(SessionGraphMetric.POWER, SessionGraphWindow.ALL),
            )
            assertEquals(" W", model.unit)
            assertEquals(setOf(2), model.lineBreakIndices)
            assertEquals(1, model.tooltipDecimals)
        }

    @Test
    fun `network signal and latency seeds match loading without mixing their contexts`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val history =
                listOf(
                    networkHistory.first(),
                    networkHistory.first().copy(timestamp = 2_000L, signalDbm = null),
                    networkHistory.first().copy(
                        timestamp = 3_000L,
                        type = "CELLULAR",
                        networkSubtype = "5G",
                        signalDbm = -95,
                        latencyMs = 71,
                    ),
                )
            every { getNetworkHistory(any()) } returns flowOf(history)
            for (metric in NetworkHistoryMetric.entries) {
                val model =
                    buildNetworkHistoryChartModel(history, metric, HistoryPeriod.DAY, MAX_FULLSCREEN_CHART_POINTS)
                assertSeedAndLoadParity(
                    model,
                    FullscreenChartSelection.NetworkHistory(metric, HistoryPeriod.DAY),
                )
                if (metric == NetworkHistoryMetric.SIGNAL) {
                    assertEquals(setOf(1), model.lineBreakIndices)
                    assertEquals(
                        R.string.network_signal_history_mixed,
                        networkSignalHistoryContextResource(model.networkSignalFamilies),
                    )
                    assertEquals("5G", model.networkSignalContexts.last().networkSubtype)
                    assertTrue(formatChartTooltip(model, 1, " | ", "5G").endsWith(" | 5G"))
                    assertEquals(" dBm", model.unit)
                } else {
                    assertEquals(listOf(28f, 28f, 71f), model.chartData)
                    assertEquals(" ms", model.unit)
                    assertTrue(model.lineBreakIndices.isEmpty())
                    assertTrue(model.networkSignalContexts.isEmpty())
                    assertTrue(model.networkSignalFamilies.isEmpty())
                }
            }
        }

    private suspend fun kotlinx.coroutines.test.TestScope.assertSeedAndLoadParity(
        model: ChartRenderModel,
        selection: FullscreenChartSelection,
    ) {
        val seed = model.toFullscreenSuccess(selection)
        FullscreenChartSeedStore.prime(selection.source, seed)
        val viewModel =
            createViewModel(
                SavedStateHandle(
                    mapOf(
                        Screen.FullscreenChart.ARG_SOURCE to selection.source.name,
                        Screen.FullscreenChart.ARG_METRIC to selection.metricArgument(),
                        Screen.FullscreenChart.ARG_PERIOD to selection.periodArgument(),
                    ),
                ),
            )
        assertEquals(seed, viewModel.uiState.value)
        assertNull(FullscreenChartSeedStore.take(selection))
        advanceUntilIdle()
        val loaded = viewModel.uiState.value as FullscreenChartUiState.Success
        assertEquals(seed, loaded)
        assertRenderParity(model, loaded)
    }

    @Test
    fun `all source families persist exact strings immediately and restore typed selections`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val cases =
                listOf(
                    Triple("BATTERY_HISTORY", "TEMPERATURE", "MONTH"),
                    Triple("BATTERY_SESSION", "POWER", "THIRTY_MINUTES"),
                    Triple("NETWORK_HISTORY", "LATENCY", "MONTH"),
                )
            for ((source, metric, period) in cases) {
                val handle = SavedStateHandle(mapOf(Screen.FullscreenChart.ARG_SOURCE to source))
                val vm = createViewModel(handle)
                val initial = parseFullscreenChartSelection(source, null, null)
                assertEquals(initial, vm.selection)
                when (vm.selection) {
                    is FullscreenChartSelection.BatteryHistory -> vm.setMetric(BatteryHistoryMetric.TEMPERATURE)
                    is FullscreenChartSelection.BatterySession -> vm.setMetric(SessionGraphMetric.POWER)
                    is FullscreenChartSelection.NetworkHistory -> vm.setMetric(NetworkHistoryMetric.LATENCY)
                }
                assertEquals(metric, handle.get<String>(Screen.FullscreenChart.ARG_METRIC))
                assertEquals(
                    initial.periodArgument(),
                    handle.get<String>(Screen.FullscreenChart.ARG_PERIOD),
                )
                when (vm.selection) {
                    is FullscreenChartSelection.BatteryHistory -> vm.setPeriod(HistoryPeriod.MONTH)
                    is FullscreenChartSelection.BatterySession -> vm.setPeriod(SessionGraphWindow.THIRTY_MINUTES)
                    is FullscreenChartSelection.NetworkHistory -> vm.setPeriod(HistoryPeriod.MONTH)
                }
                val periodChange = vm.selection
                assertEquals(source, handle.get<String>(Screen.FullscreenChart.ARG_SOURCE))
                assertEquals(metric, handle.get<String>(Screen.FullscreenChart.ARG_METRIC))
                assertEquals(period, handle.get<String>(Screen.FullscreenChart.ARG_PERIOD))
                val restoredHandle = SavedStateHandle(handle.keys().associateWith { handle.get<String>(it) })
                val restored = createViewModel(restoredHandle)
                assertEquals(periodChange, restored.selection)
                advanceUntilIdle()
                assertEquals(periodChange, (restored.uiState.value as FullscreenChartUiState.HasSelections).selection)
            }
        }

    private fun assertRenderParity(
        model: ChartRenderModel,
        state: FullscreenChartUiState.Success,
    ) {
        assertEquals(model.chartData, state.chartData)
        assertEquals(model.chartTimestamps, state.chartTimestamps)
        assertEquals(model.unit, state.unit)
        assertEquals(model.yLabels, state.yLabels)
        assertEquals(model.xLabels, state.xLabels)
        assertEquals(model.tooltipDecimals, state.tooltipDecimals)
        assertEquals(model.tooltipTimeSkeleton, state.tooltipTimeSkeleton)
        assertEquals(model.temperatureUnit, state.temperatureUnit)
        assertEquals(model.lineBreakIndices, state.lineBreakIndices)
        assertEquals(model.networkSignalContexts, state.networkSignalContexts)
        assertEquals(model.networkSignalFamilies, state.networkSignalFamilies)
    }
}
