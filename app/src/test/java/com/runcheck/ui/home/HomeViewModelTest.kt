package com.runcheck.ui.home

import android.os.SystemClock
import com.runcheck.domain.insights.engine.InsightHomeRankingPolicy
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.MonitoringHeartbeat
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.InsightRepository
import com.runcheck.domain.repository.MonitoringStatusRepository
import com.runcheck.domain.scoring.HealthScoreCalculator
import com.runcheck.domain.usecase.ChargerSessionTracker
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetNetworkStateUseCase
import com.runcheck.domain.usecase.GetSpeedTestHistoryUseCase
import com.runcheck.domain.usecase.GetStorageStateUseCase
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.pro.ProState
import com.runcheck.pro.ProStateProvider
import com.runcheck.pro.ProStatus
import com.runcheck.testutil.insightFixture
import com.runcheck.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getBatteryState: GetBatteryStateUseCase = mockk()
    private val getNetworkState: GetNetworkStateUseCase = mockk()
    private val getThermalState: GetThermalStateUseCase = mockk()
    private val getStorageState: GetStorageStateUseCase = mockk()
    private val getSpeedTestHistory: GetSpeedTestHistoryUseCase = mockk()
    private val insightRepository: InsightRepository = mockk(relaxed = true)
    private val monitoringStatusRepository: MonitoringStatusRepository = mockk(relaxed = true)
    private val proStateProvider: ProStateProvider = mockk()
    private val chargerSessionTracker: ChargerSessionTracker = mockk(relaxed = true)
    private val healthScoreCalculator = HealthScoreCalculator()
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk(relaxed = true)

    private val proStateFlow = MutableStateFlow(ProState())
    private val proAccessReadyFlow = MutableStateFlow(true)

    private lateinit var viewModel: HomeViewModel

    // Test data
    private val testBattery =
        BatteryState(
            level = 85,
            voltageMv = 4100,
            temperatureC = 28f,
            currentMa = MeasuredValue(value = -350, confidence = Confidence.HIGH),
            chargingStatus = ChargingStatus.DISCHARGING,
            plugType = PlugType.NONE,
            health = BatteryHealth.GOOD,
            technology = "Li-ion",
        )

    private val testNetwork =
        NetworkState(
            connectionType = ConnectionType.WIFI,
            signalDbm = -55,
            signalQuality = SignalQuality.EXCELLENT,
            wifiSsid = "TestWiFi",
        )

    private val testThermal =
        ThermalState(
            batteryTempC = 28f,
            cpuTempC = 45f,
            thermalStatus = ThermalStatus.NONE,
            isThrottling = false,
        )

    private val testStorage =
        StorageState(
            totalBytes = 128_000_000_000L,
            availableBytes = 64_000_000_000L,
            usedBytes = 64_000_000_000L,
            usagePercent = 50f,
        )

    @Before
    fun setup() {
        mockkStatic(SystemClock::class)
        every { SystemClock.uptimeMillis() } returns 1_000L
        every { getBatteryState() } returns flowOf(testBattery)
        every { getNetworkState() } returns flowOf(testNetwork)
        every { getThermalState() } returns flowOf(testThermal)
        every { getStorageState() } returns flowOf(testStorage)
        every { getSpeedTestHistory.getLatest() } returns flowOf(null)
        every { insightRepository.getActiveInsights() } returns flowOf(emptyList())
        every { insightRepository.getUnseenCount() } returns flowOf(0)
        every { proStateProvider.proState } returns proStateFlow
        every { proStateProvider.proAccessReady } returns proAccessReadyFlow
        every { manageUserPreferences.observePreferences() } returns flowOf(UserPreferences())
        every { monitoringStatusRepository.observeLastWorkerHeartbeat() } returns
            flowOf(
                MonitoringHeartbeat(
                    recordedAtEpochMillis = System.currentTimeMillis(),
                    recordedAtUptimeMillis = 0L,
                    intervalMinutes = UserPreferences().monitoringInterval.minutes,
                ),
            )
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.stopObserving()
            advanceAll()
        }
        unmockkStatic(SystemClock::class)
    }

    private fun createViewModel(): HomeViewModel =
        HomeViewModel(
            getBatteryState = getBatteryState,
            getNetworkState = getNetworkState,
            getThermalState = getThermalState,
            getStorageState = getStorageState,
            getSpeedTestHistory = getSpeedTestHistory,
            insightRepository = insightRepository,
            insightHomeRankingPolicy = InsightHomeRankingPolicy(),
            monitoringStatusRepository = monitoringStatusRepository,
            proStateProvider = proStateProvider,
            chargerSessionTracker = chargerSessionTracker,
            healthScoreCalculator = healthScoreCalculator,
            manageUserPreferences = manageUserPreferences,
        )

    @Test
    fun `initial state is Loading`() {
        viewModel = createViewModel()
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `refresh reuses all four Home data flows and keeps the indicator for 900 milliseconds`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()
            every { SystemClock.uptimeMillis() } answers {
                1_000L + mainDispatcherRule.testDispatcher.scheduler.currentTime
            }

            viewModel.refresh()
            runCurrent()

            assertTrue(viewModel.isRefreshing.value)
            verify(exactly = 2) { getBatteryState() }
            verify(exactly = 2) { getNetworkState() }
            verify(exactly = 2) { getThermalState() }
            verify(exactly = 2) { getStorageState() }

            advanceTimeBy(899L)
            runCurrent()
            assertTrue(viewModel.isRefreshing.value)

            advanceTimeBy(1L)
            runCurrent()
            assertFalse(viewModel.isRefreshing.value)
            viewModel.stopObserving()
        }

    @Test
    fun `refresh indicator returns to idle after 12 seconds without a completed snapshot`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()
            every { getBatteryState() } returns flow { awaitCancellation() }

            viewModel.refresh()
            runCurrent()

            advanceTimeBy(11_999L)
            runCurrent()
            assertTrue(viewModel.isRefreshing.value)

            advanceTimeBy(1L)
            runCurrent()
            assertFalse(viewModel.isRefreshing.value)
            viewModel.stopObserving()
        }

    @Test
    fun `refresh failure returns to idle and uses the existing Home error state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()
            every { getNetworkState() } returns flow { error("refresh failed") }

            viewModel.refresh()
            runCurrent()

            assertFalse(viewModel.isRefreshing.value)
            assertTrue(viewModel.uiState.value is HomeUiState.Error)
            viewModel.stopObserving()
        }

    @Test
    fun `leaving Home clears a running refresh indicator`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()
            every { getBatteryState() } returns flow { awaitCancellation() }

            viewModel.refresh()
            runCurrent()
            assertTrue(viewModel.isRefreshing.value)

            viewModel.stopObserving()

            assertFalse(viewModel.isRefreshing.value)
        }

    /**
     * Advance just enough virtual time for one sampled UI emission without chasing
     * the repeating sample ticker forever.
     */
    private fun advanceAll() {
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(334L)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun `healthy data produces Success state with correct health score`() =
        runTest(mainDispatcherRule.testDispatcher) {
            proStateFlow.value = ProState()

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            val state = viewModel.uiState.value
            assertTrue("Expected Success but got $state", state is HomeUiState.Success)
            val success = state as HomeUiState.Success

            // HealthScoreCalculator produces a deterministic score for these inputs
            val expectedScore =
                healthScoreCalculator.calculate(
                    battery = testBattery,
                    network = testNetwork,
                    thermal = testThermal,
                    storage = testStorage,
                )
            assertEquals(expectedScore.overallScore, success.healthScore.overallScore)
            assertEquals(expectedScore.batteryScore, success.healthScore.batteryScore)
            assertEquals(expectedScore.networkScore, success.healthScore.networkScore)
            assertEquals(expectedScore.thermalScore, success.healthScore.thermalScore)
            assertEquals(expectedScore.storageScore, success.healthScore.storageScore)

            // Cancel the ongoing flow collection before runTest cleanup to prevent
            // sample(333L) ticker from causing infinite advanceUntilIdle() loop
            viewModel.stopObserving()
        }

    @Test
    fun `home stays Loading until pro access is initialized`() =
        runTest(mainDispatcherRule.testDispatcher) {
            proAccessReadyFlow.value = false
            proStateFlow.value = ProState(status = ProStatus.PRO_PURCHASED)

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            assertEquals(HomeUiState.Loading, viewModel.uiState.value)

            proAccessReadyFlow.value = true
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertEquals(ProStatus.PRO_PURCHASED, state.proState.status)
            viewModel.stopObserving()
        }

    @Test
    fun `display sampling does not drop charger tracking transitions`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val batteryFlow = MutableStateFlow(testBattery)
            every { getBatteryState() } returns batteryFlow

            viewModel = createViewModel()
            viewModel.startObserving()
            runCurrent()

            val chargingBattery = testBattery.copy(chargingStatus = ChargingStatus.CHARGING)
            batteryFlow.value = chargingBattery
            runCurrent()

            coVerify(exactly = 1) { chargerSessionTracker.onObservedBatteryState(testBattery, any()) }
            coVerify(exactly = 1) { chargerSessionTracker.onObservedBatteryState(chargingBattery, any()) }
            assertEquals(HomeUiState.Loading, viewModel.uiState.value)

            advanceAll()

            assertEquals(
                ChargingStatus.CHARGING,
                (viewModel.uiState.value as HomeUiState.Success).batteryState.chargingStatus,
            )
            viewModel.stopObserving()
        }

    @Test
    fun `upstream flow failure replaces visible state with Error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getNetworkState() } returns
                flow {
                    emit(testNetwork)
                    delay(334L)
                    error("network source failed")
                }

            viewModel = createViewModel()
            viewModel.startObserving()
            runCurrent()
            advanceTimeBy(333L)
            runCurrent()

            assertTrue(viewModel.uiState.value is HomeUiState.Success)

            advanceTimeBy(1L)
            runCurrent()

            assertTrue(viewModel.uiState.value is HomeUiState.Error)
            viewModel.stopObserving()
        }

    @Test
    fun `visible unseen insights are marked seen for free users too`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val insight = insightFixture(id = 7L, target = InsightTarget.BATTERY, seen = false)
            every { insightRepository.getActiveInsights() } returns flowOf(listOf(insight))
            every { insightRepository.getUnseenCount() } returns flowOf(1)
            coEvery { insightRepository.markSeen(any()) } returns Unit

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertEquals(1, state.insights.size)
            assertEquals(1, state.unseenInsightCount)
            coVerify(exactly = 1) { insightRepository.markSeen(setOf(7L)) }

            viewModel.stopObserving()
        }

    @Test
    fun `recent speed test contributes to the visible home network score`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val speedTest =
                SpeedTestResult(
                    timestamp = System.currentTimeMillis() - 60_000L,
                    downloadMbps = 1.0,
                    uploadMbps = 1.0,
                    pingMs = 600,
                    jitterMs = 60,
                    serverName = null,
                    serverLocation = null,
                    connectionType = ConnectionType.WIFI,
                    networkSubtype = null,
                    signalDbm = -55,
                )
            every { getSpeedTestHistory.getLatest() } returns flowOf(speedTest)

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            val success = viewModel.uiState.value as HomeUiState.Success
            val expected =
                healthScoreCalculator.calculate(
                    battery = testBattery,
                    network = testNetwork,
                    thermal = testThermal,
                    storage = testStorage,
                    recentSpeedTest = speedTest,
                    nowMillis = System.currentTimeMillis(),
                )
            assertEquals(expected.networkScore, success.healthScore.networkScore)
            viewModel.stopObserving()
        }

    @Test
    fun `home filters pro-only insights for free users`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val appUsageInsight = insightFixture(1L, InsightTarget.APP_USAGE, seen = false)
            val chargerInsight = insightFixture(2L, InsightTarget.CHARGER, seen = false)
            val batteryInsight = insightFixture(3L, InsightTarget.BATTERY, seen = false)
            val thermalInsight = insightFixture(4L, InsightTarget.THERMAL, seen = false)
            val networkInsight = insightFixture(5L, InsightTarget.NETWORK, seen = false)
            every { insightRepository.getActiveInsights() } returns
                flowOf(listOf(appUsageInsight, chargerInsight, batteryInsight, thermalInsight, networkInsight))
            every { insightRepository.getUnseenCount() } returns flowOf(5)
            proStateFlow.value = ProState(status = ProStatus.FREE)

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertEquals(
                listOf(InsightTarget.BATTERY, InsightTarget.THERMAL, InsightTarget.NETWORK),
                state.insights.map { it.target },
            )
            assertEquals(3, state.totalInsightCount)
            assertEquals(3, state.unseenInsightCount)
            coVerify(exactly = 1) { insightRepository.markSeen(setOf(3L, 4L, 5L)) }

            viewModel.stopObserving()
        }

    @Test
    fun `dismiss insight delegates to repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { insightRepository.dismiss(42L) } returns Unit

            viewModel = createViewModel()
            viewModel.dismissInsight(42L)
            advanceAll()

            coVerify(exactly = 1) { insightRepository.dismiss(42L) }
        }
}
