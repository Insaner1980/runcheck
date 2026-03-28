package com.runcheck.ui.home

import androidx.lifecycle.SavedStateHandle
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.scoring.HealthScoreCalculator
import com.runcheck.domain.usecase.ChargerSessionTracker
import com.runcheck.domain.usecase.GetBatteryStateUseCase
import com.runcheck.domain.usecase.GetNetworkStateUseCase
import com.runcheck.domain.usecase.GetStorageStateUseCase
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.pro.ProManager
import com.runcheck.pro.ProState
import com.runcheck.pro.ProStatus
import com.runcheck.pro.TrialManager
import com.runcheck.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val batteryRepository: BatteryRepository = mockk(relaxed = true)
    private val proManager: ProManager = mockk()
    private val trialManager: TrialManager = mockk(relaxed = true)
    private val chargerSessionTracker: ChargerSessionTracker = mockk(relaxed = true)
    private val healthScoreCalculator = HealthScoreCalculator()
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk(relaxed = true)

    private val proStateFlow = MutableStateFlow(ProState())

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
        every { getBatteryState() } returns flowOf(testBattery)
        every { getNetworkState() } returns flowOf(testNetwork)
        every { getThermalState() } returns flowOf(testThermal)
        every { getStorageState() } returns flowOf(testStorage)
        every { proManager.proState } returns proStateFlow
        every { manageUserPreferences.observePreferences() } returns flowOf(UserPreferences())
        coEvery { batteryRepository.getLatestReadingTimestamp() } returns null
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.stopObserving()
            advanceAll()
        }
    }

    private fun createViewModel(): HomeViewModel =
        HomeViewModel(
            savedStateHandle = SavedStateHandle(),
            getBatteryState = getBatteryState,
            getNetworkState = getNetworkState,
            getThermalState = getThermalState,
            getStorageState = getStorageState,
            batteryRepository = batteryRepository,
            proManager = proManager,
            trialManager = trialManager,
            chargerSessionTracker = chargerSessionTracker,
            healthScoreCalculator = healthScoreCalculator,
            manageUserPreferences = manageUserPreferences,
        )

    @Test
    fun `initial state is Loading`() {
        viewModel = createViewModel()
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
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
            // Trial expired, no banners
            proStateFlow.value =
                ProState(
                    status = ProStatus.TRIAL_EXPIRED,
                    trialDaysRemaining = 0,
                    trialStartTimestamp = 0L,
                )

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
    fun `trial day calculation shows correct days remaining`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Trial active with 2 days remaining
            proStateFlow.value =
                ProState(
                    status = ProStatus.TRIAL_ACTIVE,
                    trialDaysRemaining = 2,
                    trialStartTimestamp = System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000,
                )
            coEvery { trialManager.isWelcomeShown() } returns true
            coEvery { trialManager.isDay5PromptShown() } returns true

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertEquals(ProStatus.TRIAL_ACTIVE, state.proState.status)
            assertEquals(2, state.proState.trialDaysRemaining)

            viewModel.stopObserving()
        }

    @Test
    fun `day 5 banner is visible when trial day 5 reached and not dismissed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Trial active, 2 days remaining = 5 days elapsed
            proStateFlow.value =
                ProState(
                    status = ProStatus.TRIAL_ACTIVE,
                    trialDaysRemaining = 2,
                    trialStartTimestamp = System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000,
                )
            coEvery { trialManager.isWelcomeShown() } returns true
            coEvery { trialManager.isDay5PromptShown() } returns false

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertTrue("Day 5 banner should be visible", state.showDay5Banner)

            viewModel.stopObserving()
        }

    @Test
    fun `day 5 banner is hidden after dismissal`() =
        runTest(mainDispatcherRule.testDispatcher) {
            proStateFlow.value =
                ProState(
                    status = ProStatus.TRIAL_ACTIVE,
                    trialDaysRemaining = 2,
                    trialStartTimestamp = System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000,
                )
            coEvery { trialManager.isWelcomeShown() } returns true
            coEvery { trialManager.isDay5PromptShown() } returns false

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            // Verify banner is showing first
            assertTrue((viewModel.uiState.value as HomeUiState.Success).showDay5Banner)

            // Dismiss the banner
            viewModel.dismissDay5Banner()
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertFalse("Day 5 banner should be hidden after dismiss", state.showDay5Banner)
            coVerify { trialManager.setDay5PromptShown() }

            viewModel.stopObserving()
        }

    @Test
    fun `expiration modal shows when trial expired and not dismissed this session`() =
        runTest(mainDispatcherRule.testDispatcher) {
            proStateFlow.value =
                ProState(
                    status = ProStatus.TRIAL_EXPIRED,
                    trialDaysRemaining = 0,
                    trialStartTimestamp = System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000,
                )
            coEvery { trialManager.getUpgradeCardDismissCount() } returns 0
            coEvery { trialManager.getUpgradeCardLastDismissTimestamp() } returns 0L

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertTrue("Expiration modal should show", state.showExpirationModal)

            viewModel.stopObserving()
        }

    @Test
    fun `expiration modal hidden after dismiss`() =
        runTest(mainDispatcherRule.testDispatcher) {
            proStateFlow.value =
                ProState(
                    status = ProStatus.TRIAL_EXPIRED,
                    trialDaysRemaining = 0,
                    trialStartTimestamp = System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000,
                )
            coEvery { trialManager.getUpgradeCardDismissCount() } returns 0
            coEvery { trialManager.getUpgradeCardLastDismissTimestamp() } returns 0L

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            viewModel.dismissExpirationModal()
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertFalse("Expiration modal should be hidden after dismiss", state.showExpirationModal)

            viewModel.stopObserving()
        }

    @Test
    fun `upgrade card visible for expired trial with no prior dismissals`() =
        runTest(mainDispatcherRule.testDispatcher) {
            proStateFlow.value =
                ProState(
                    status = ProStatus.TRIAL_EXPIRED,
                    trialDaysRemaining = 0,
                    trialStartTimestamp = System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000,
                )
            coEvery { trialManager.getUpgradeCardDismissCount() } returns 0
            coEvery { trialManager.getUpgradeCardLastDismissTimestamp() } returns 0L

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertTrue("Upgrade card should be visible", state.showUpgradeCard)

            viewModel.stopObserving()
        }

    @Test
    fun `upgrade card hidden after 3 dismissals`() =
        runTest(mainDispatcherRule.testDispatcher) {
            proStateFlow.value =
                ProState(
                    status = ProStatus.TRIAL_EXPIRED,
                    trialDaysRemaining = 0,
                    trialStartTimestamp = System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000,
                )
            coEvery { trialManager.getUpgradeCardDismissCount() } returns 3
            coEvery { trialManager.getUpgradeCardLastDismissTimestamp() } returns System.currentTimeMillis()

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertFalse("Upgrade card should be hidden after 3 dismissals", state.showUpgradeCard)

            viewModel.stopObserving()
        }

    @Test
    fun `upgrade card hidden when dismissed recently (less than 7 days)`() =
        runTest(mainDispatcherRule.testDispatcher) {
            proStateFlow.value =
                ProState(
                    status = ProStatus.TRIAL_EXPIRED,
                    trialDaysRemaining = 0,
                    trialStartTimestamp = System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000,
                )
            // 1 dismiss, 2 days ago = should still be hidden (cooldown = 7 days)
            coEvery { trialManager.getUpgradeCardDismissCount() } returns 1
            coEvery { trialManager.getUpgradeCardLastDismissTimestamp() } returns
                System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertFalse("Upgrade card should be hidden during 7-day cooldown", state.showUpgradeCard)

            viewModel.stopObserving()
        }

    @Test
    fun `pro user sees no trial banners and no upgrade card`() =
        runTest(mainDispatcherRule.testDispatcher) {
            proStateFlow.value =
                ProState(
                    status = ProStatus.PRO_PURCHASED,
                    trialDaysRemaining = 0,
                    trialStartTimestamp = 0L,
                    purchaseTimestamp = System.currentTimeMillis(),
                )

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceAll()

            val state = viewModel.uiState.value as HomeUiState.Success
            assertFalse("Pro user should not see welcome sheet", state.showWelcomeSheet)
            assertFalse("Pro user should not see day 5 banner", state.showDay5Banner)
            assertFalse("Pro user should not see expiration modal", state.showExpirationModal)
            assertFalse("Pro user should not see upgrade card", state.showUpgradeCard)
            assertTrue("Pro user isPro should be true", state.isPro)

            viewModel.stopObserving()
        }
}
