package com.runcheck.ui.network

import androidx.lifecycle.SavedStateHandle
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.SpeedTestConnectionInfo
import com.runcheck.domain.model.SpeedTestProgress
import com.runcheck.domain.usecase.FinalizeSpeedTestUseCase
import com.runcheck.domain.usecase.GetMeasuredNetworkStateUseCase
import com.runcheck.domain.usecase.GetNetworkHistoryUseCase
import com.runcheck.domain.usecase.GetSpeedTestHistoryUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.RunSpeedTestUseCase
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.ui.common.UiText
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
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
class NetworkViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMeasuredNetworkState: GetMeasuredNetworkStateUseCase = mockk()
    private val runSpeedTest: RunSpeedTestUseCase = mockk()
    private val getSpeedTestHistory: GetSpeedTestHistoryUseCase = mockk()
    private val finalizeSpeedTest: FinalizeSpeedTestUseCase = mockk(relaxed = true)
    private val isProUser: IsProUserUseCase = mockk()
    private val getNetworkHistory: GetNetworkHistoryUseCase = mockk()
    private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase = mockk(relaxed = true)
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk(relaxed = true)
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private lateinit var viewModel: NetworkViewModel

    private val testNetworkState =
        NetworkState(
            connectionType = ConnectionType.WIFI,
            signalDbm = -50,
            signalQuality = SignalQuality.EXCELLENT,
            wifiSsid = "TestWiFi",
            latencyMs = 25,
        )

    @Before
    fun setup() {
        every { isProUser() } returns false
        every { getSpeedTestHistory(any()) } returns flowOf(emptyList())
        every { getNetworkHistory(any()) } returns flowOf(emptyList())
        every { manageInfoCardDismissals.observeDismissedCardIds() } returns flowOf(emptySet())
        every { manageUserPreferences.observePreferences() } returns
            flowOf(
                com.runcheck.domain.model
                    .UserPreferences(),
            )
        every { observeProAccess() } returns flowOf(false)
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.stopObserving()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): NetworkViewModel =
        NetworkViewModel(
            savedStateHandle = savedStateHandle,
            getMeasuredNetworkState = getMeasuredNetworkState,
            runSpeedTest = runSpeedTest,
            getSpeedTestHistory = getSpeedTestHistory,
            finalizeSpeedTest = finalizeSpeedTest,
            isProUser = isProUser,
            getNetworkHistory = getNetworkHistory,
            manageInfoCardDismissals = manageInfoCardDismissals,
            manageUserPreferences = manageUserPreferences,
            observeProAccess = observeProAccess,
        )

    @Test
    fun `initial state is Loading`() {
        every { getMeasuredNetworkState() } returns emptyFlow()

        viewModel = createViewModel()
        assertEquals(NetworkUiState.Loading, viewModel.networkUiState.value)
    }

    @Test
    fun `network data loads into Success state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns flowOf(testNetworkState)

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceUntilIdle()

            val state = viewModel.networkUiState.value
            assertTrue("Expected Success but got $state", state is NetworkUiState.Success)
            val success = state as NetworkUiState.Success

            assertEquals(ConnectionType.WIFI, success.networkState.connectionType)
            assertEquals(-50, success.networkState.signalDbm)
            assertEquals(SignalQuality.EXCELLENT, success.networkState.signalQuality)
            assertEquals("TestWiFi", success.networkState.wifiSsid)
            assertEquals(25, success.networkState.latencyMs)
        }

    @Test
    fun `speed test transitions through phases correctly`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns flowOf(testNetworkState)

            val speedTestFlow = MutableSharedFlow<SpeedTestProgress>()
            every { runSpeedTest(any()) } returns speedTestFlow

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceUntilIdle()

            // Start speed test
            viewModel.startSpeedTest()
            runCurrent()

            // Initial state after starting: Ping phase, isRunning = true
            var speedState = viewModel.speedTestState.value
            assertEquals(SpeedTestPhase.Ping, speedState.phase)
            assertTrue("Speed test should be running", speedState.isRunning)

            // Emit ping progress
            speedTestFlow.emit(SpeedTestProgress.PingPhase(pingMs = 15, jitterMs = 3))
            runCurrent()

            speedState = viewModel.speedTestState.value
            assertEquals(SpeedTestPhase.Ping, speedState.phase)
            assertEquals(15, speedState.pingMs)
            assertEquals(3, speedState.jitterMs)

            // Emit download progress
            speedTestFlow.emit(SpeedTestProgress.DownloadPhase(currentMbps = 85.5, progress = 0.5f))
            runCurrent()

            speedState = viewModel.speedTestState.value
            assertEquals(SpeedTestPhase.Download, speedState.phase)
            assertEquals(85.5, speedState.downloadMbps, 0.01)
            assertEquals(0.5f, speedState.downloadProgress, 0.01f)

            // Emit upload progress
            speedTestFlow.emit(SpeedTestProgress.UploadPhase(currentMbps = 30.2, progress = 0.8f))
            runCurrent()

            speedState = viewModel.speedTestState.value
            assertEquals(SpeedTestPhase.Upload, speedState.phase)
            assertEquals(30.2, speedState.uploadMbps, 0.01)
            assertEquals(0.8f, speedState.uploadProgress, 0.01f)

            // Emit completed
            speedTestFlow.emit(
                SpeedTestProgress.Completed(
                    downloadMbps = 95.0,
                    uploadMbps = 35.0,
                    pingMs = 12,
                    jitterMs = 2,
                    serverName = "Test Server",
                    serverLocation = "Helsinki",
                    connectionInfo =
                        SpeedTestConnectionInfo(
                            connectionType = ConnectionType.WIFI,
                            networkSubtype = null,
                            signalDbm = -50,
                        ),
                ),
            )
            runCurrent()

            speedState = viewModel.speedTestState.value
            assertEquals(SpeedTestPhase.Completed, speedState.phase)
            assertFalse("Speed test should not be running after completion", speedState.isRunning)
            assertEquals(95.0, speedState.downloadMbps, 0.01)
            assertEquals(35.0, speedState.uploadMbps, 0.01)
            assertEquals(12, speedState.pingMs)

            // Verify finalize was called
            coVerify { finalizeSpeedTest(any(), any()) }
        }

    @Test
    fun `second speed test call is ignored while running`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns flowOf(testNetworkState)

            val speedTestFlow = MutableSharedFlow<SpeedTestProgress>()
            every { runSpeedTest(any()) } returns speedTestFlow

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceUntilIdle()

            // Start first speed test
            viewModel.startSpeedTest()
            runCurrent()

            assertTrue("Speed test should be running", viewModel.speedTestState.value.isRunning)

            // Try starting a second speed test (should be ignored)
            viewModel.startSpeedTest()
            runCurrent()

            // runSpeedTest should only have been invoked once
            // The second call should be blocked by isRunning guard
            verify(exactly = 1) { runSpeedTest(any()) }
        }

    @Test
    fun `speed test error produces Failed phase`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns flowOf(testNetworkState)

            every { runSpeedTest(any()) } returns
                flow {
                    throw RuntimeException("Network timeout")
                }

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceUntilIdle()

            viewModel.startSpeedTest()
            advanceUntilIdle()

            val speedState = viewModel.speedTestState.value
            assertTrue(
                "Phase should be Failed but got ${speedState.phase}",
                speedState.phase is SpeedTestPhase.Failed,
            )
            assertFalse("Speed test should not be running after error", speedState.isRunning)

            val failedPhase = speedState.phase as SpeedTestPhase.Failed
            // Error message should contain the exception message
            assertTrue(
                "Error message should reflect the exception",
                failedPhase.error is UiText.Dynamic && failedPhase.error.value == "Network timeout",
            )
        }

    @Test
    fun `speed test Failed progress event produces Failed phase`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns flowOf(testNetworkState)

            val speedTestFlow = MutableSharedFlow<SpeedTestProgress>()
            every { runSpeedTest(any()) } returns speedTestFlow

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceUntilIdle()

            viewModel.startSpeedTest()
            runCurrent()

            speedTestFlow.emit(SpeedTestProgress.Failed("Server unavailable"))
            runCurrent()

            val speedState = viewModel.speedTestState.value
            assertTrue(speedState.phase is SpeedTestPhase.Failed)
            assertFalse(speedState.isRunning)
        }

    @Test
    fun `selected network history period restores from saved state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns flowOf(testNetworkState)

            viewModel =
                createViewModel(
                    savedStateHandle =
                        SavedStateHandle(
                            mapOf(
                                "network_selected_history_period" to com.runcheck.domain.model.HistoryPeriod.MONTH.name,
                            ),
                        ),
                )
            viewModel.startObserving()
            advanceUntilIdle()

            val state = viewModel.networkUiState.value as NetworkUiState.Success
            assertEquals(com.runcheck.domain.model.HistoryPeriod.MONTH, state.selectedHistoryPeriod)
            verify { getNetworkHistory(com.runcheck.domain.model.HistoryPeriod.MONTH) }
        }
}
