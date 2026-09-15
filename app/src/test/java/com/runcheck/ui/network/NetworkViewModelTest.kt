package com.runcheck.ui.network

import androidx.lifecycle.SavedStateHandle
import com.runcheck.R
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.SpeedTestConnectionInfo
import com.runcheck.domain.model.SpeedTestProgress
import com.runcheck.domain.usecase.FinalizeSpeedTestUseCase
import com.runcheck.domain.usecase.GetMeasuredNetworkStateUseCase
import com.runcheck.domain.usecase.GetNetworkHistoryUseCase
import com.runcheck.domain.usecase.GetSpeedTestHistoryUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.RunSpeedTestUseCase
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.ui.common.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
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
            getNetworkHistory = getNetworkHistory,
            manageInfoCardDismissals = manageInfoCardDismissals,
            manageUserPreferences = manageUserPreferences,
            observeProAccess = observeProAccess,
        )

    private fun advanceNetworkSample() {
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(334L)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun `speed history requests follow free pro and revoked entitlement limits`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val access = MutableStateFlow(false)
            val requestedLimits = mutableListOf<Int>()
            every { observeProAccess() } returns access
            every { getMeasuredNetworkState() } returns emptyFlow()
            every { getSpeedTestHistory(capture(requestedLimits)) } returns flowOf(emptyList())
            viewModel = createViewModel()

            try {
                viewModel.startObserving()
                runCurrent()
                assertEquals(listOf(5), requestedLimits)

                access.value = true
                runCurrent()
                assertEquals(listOf(5, 100), requestedLimits)

                access.value = false
                runCurrent()
                assertEquals(listOf(5, 100, 5), requestedLimits)
            } finally {
                viewModel.stopObserving()
            }
        }

    private fun prepareSpeedTest(): MutableSharedFlow<SpeedTestProgress> =
        MutableSharedFlow<SpeedTestProgress>().also { speedTestFlow ->
            every { getMeasuredNetworkState() } returns MutableStateFlow(testNetworkState)
            every { runSpeedTest(any()) } returns speedTestFlow
            viewModel = createViewModel()
            viewModel.startObserving()
            advanceNetworkSample()
        }

    @Test
    fun `history received before the first live sample is retained`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val readings = listOf(NetworkReading(1L, "WIFI", -50, null, null, null, null, 25))
            every { getNetworkHistory(any()) } returns MutableStateFlow(readings)
            every { getMeasuredNetworkState() } returns MutableStateFlow(testNetworkState)
            viewModel = createViewModel()
            try {
                viewModel.startObserving()
                advanceNetworkSample()
                assertEquals(readings, (viewModel.networkUiState.value as NetworkUiState.Success).signalHistory)
            } finally {
                viewModel.stopObserving()
            }
        }

    @Test
    fun `initial state is Loading`() {
        every { getMeasuredNetworkState() } returns emptyFlow()

        viewModel = createViewModel()
        assertEquals(NetworkUiState.Loading, viewModel.networkUiState.value)
    }

    @Test
    fun `network data loads into Success state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns MutableStateFlow(testNetworkState)

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceNetworkSample()

            val state = viewModel.networkUiState.value
            assertTrue("Expected Success but got $state", state is NetworkUiState.Success)
            val success = state as NetworkUiState.Success

            assertEquals(ConnectionType.WIFI, success.networkState.connectionType)
            assertEquals(-50, success.networkState.signalDbm)
            assertEquals(SignalQuality.EXCELLENT, success.networkState.signalQuality)
            assertEquals("TestWiFi", success.networkState.wifiSsid)
            assertEquals(25, success.networkState.latencyMs)
            viewModel.stopObserving()
        }

    @Test
    fun `refresh failure clears indicator while preserving existing success`() =
        runTest(mainDispatcherRule.testDispatcher) {
            var failRefresh = false
            every { getMeasuredNetworkState() } answers {
                if (failRefresh) {
                    flow { throw IllegalStateException("refresh failed") }
                } else {
                    MutableStateFlow(testNetworkState)
                }
            }
            viewModel = createViewModel()
            viewModel.startObserving()
            advanceNetworkSample()
            val successBeforeRefresh = viewModel.networkUiState.value

            failRefresh = true
            viewModel.refresh()
            assertTrue(viewModel.isRefreshing.value)
            runCurrent()

            assertFalse(viewModel.isRefreshing.value)
            assertEquals(successBeforeRefresh, viewModel.networkUiState.value)
            verify(exactly = 2) { getMeasuredNetworkState() }
            viewModel.stopObserving()
        }

    @Test
    fun `speed test transitions through phases correctly`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val speedTestFlow = prepareSpeedTest()

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
            coVerify { finalizeSpeedTest(any()) }
            viewModel.stopObserving()
        }

    @Test
    fun `second speed test call is ignored while running`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepareSpeedTest()

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
            viewModel.stopObserving()
        }

    @Test
    fun `completion waits for finalization and then permits another test`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val finalizationStarted = CompletableDeferred<Unit>()
            val finishFinalization = CompletableDeferred<Unit>()
            every { runSpeedTest(any()) } returns
                flow {
                    emit(SpeedTestProgress.UploadPhase(currentMbps = 35.0, progress = 1f))
                    emit(
                        SpeedTestProgress.Completed(
                            downloadMbps = 95.0,
                            uploadMbps = 35.0,
                            pingMs = 12,
                            jitterMs = 2,
                            serverName = null,
                            serverLocation = null,
                            connectionInfo = SpeedTestConnectionInfo(ConnectionType.WIFI, "WiFi 6", -50),
                        ),
                    )
                }
            coEvery { finalizeSpeedTest(any()) } coAnswers {
                finalizationStarted.complete(Unit)
                finishFinalization.await()
            }
            viewModel = createViewModel()
            viewModel.startSpeedTest()
            runCurrent()

            assertTrue(finalizationStarted.isCompleted)
            assertEquals(SpeedTestPhase.Upload, viewModel.speedTestState.value.phase)
            assertTrue(viewModel.speedTestState.value.isRunning)
            viewModel.startSpeedTest()
            verify(exactly = 1) { runSpeedTest(any()) }

            finishFinalization.complete(Unit)
            runCurrent()
            assertEquals(SpeedTestPhase.Completed, viewModel.speedTestState.value.phase)
            assertFalse(viewModel.speedTestState.value.isRunning)

            viewModel.startSpeedTest()
            assertEquals(SpeedTestPhase.Ping, viewModel.speedTestState.value.phase)
            runCurrent()
            verify(exactly = 2) { runSpeedTest(any()) }
        }

    @Test
    fun `timeout cancels measurement and ends in non running failed phase`() =
        runTest(mainDispatcherRule.testDispatcher) {
            var measurementCancelled = false
            every { runSpeedTest(any()) } returns
                flow {
                    try {
                        emit(SpeedTestProgress.DownloadPhase(currentMbps = 85.5, progress = 0.5f))
                        awaitCancellation()
                    } finally {
                        measurementCancelled = true
                    }
                }
            viewModel = createViewModel()
            viewModel.startSpeedTest()
            runCurrent()
            advanceTimeBy(89_999L)
            runCurrent()
            assertEquals(SpeedTestPhase.Download, viewModel.speedTestState.value.phase)
            assertTrue(viewModel.speedTestState.value.isRunning)
            assertFalse(measurementCancelled)

            advanceTimeBy(1L)
            runCurrent()
            assertEquals(
                SpeedTestPhase.Failed(UiText.Resource(R.string.speed_test_error_timeout)),
                viewModel.speedTestState.value.phase,
            )
            assertFalse(viewModel.speedTestState.value.isRunning)
            assertTrue(measurementCancelled)
            coVerify(exactly = 0) { finalizeSpeedTest(any()) }
        }

    @Test
    fun `cellular confirmation is scoped to one retry and does not leak to the next session`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns MutableStateFlow(testNetworkState)
            val allowCellularCalls = mutableListOf<Boolean>()
            val cellularConnection =
                SpeedTestConnectionInfo(
                    connectionType = ConnectionType.CELLULAR,
                    networkSubtype = "5G NR",
                    signalDbm = -85,
                )
            every { runSpeedTest(any()) } answers {
                val allowCellular = firstArg<Boolean>()
                allowCellularCalls += allowCellular
                if (allowCellular) {
                    flowOf(SpeedTestProgress.Failed("Test stopped"))
                } else {
                    flowOf(SpeedTestProgress.CellularConfirmationRequired(cellularConnection))
                }
            }

            viewModel = createViewModel()
            viewModel.startSpeedTest()
            runCurrent()

            assertTrue(viewModel.speedTestState.value.showCellularWarning)
            assertEquals(SpeedTestPhase.Idle, viewModel.speedTestState.value.phase)
            assertFalse(viewModel.speedTestState.value.isRunning)
            assertEquals(listOf(false), allowCellularCalls)

            viewModel.confirmCellularSpeedTest()
            runCurrent()

            assertFalse(viewModel.speedTestState.value.showCellularWarning)
            assertEquals(listOf(false, true), allowCellularCalls)

            viewModel.confirmCellularSpeedTest()
            runCurrent()
            assertEquals(listOf(false, true), allowCellularCalls)

            viewModel.startSpeedTest()
            runCurrent()

            assertTrue(viewModel.speedTestState.value.showCellularWarning)
            assertEquals(listOf(false, true, false), allowCellularCalls)
        }

    @Test
    fun `speed test error produces Failed phase`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns MutableStateFlow(testNetworkState)

            every { runSpeedTest(any()) } returns
                flow {
                    throw RuntimeException("Network timeout")
                }

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceNetworkSample()

            viewModel.startSpeedTest()
            runCurrent()

            val speedState = viewModel.speedTestState.value
            assertTrue(
                "Phase should be Failed but got ${speedState.phase}",
                speedState.phase is SpeedTestPhase.Failed,
            )
            assertFalse("Speed test should not be running after error", speedState.isRunning)

            val failedPhase = speedState.phase as SpeedTestPhase.Failed
            assertEquals(UiText.Resource(R.string.speed_test_failed), failedPhase.error)
            viewModel.stopObserving()
        }

    @Test
    fun `speed test finalization error does not expose exception details`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns MutableStateFlow(testNetworkState)
            every { runSpeedTest(any()) } returns
                flowOf(
                    SpeedTestProgress.Completed(
                        downloadMbps = 95.0,
                        uploadMbps = 35.0,
                        pingMs = 12,
                        jitterMs = 2,
                        serverName = null,
                        serverLocation = null,
                        connectionInfo = SpeedTestConnectionInfo(ConnectionType.WIFI, "WiFi 6", -50),
                    ),
                )
            coEvery { finalizeSpeedTest(any()) } throws
                IllegalStateException("Database path: /data/user/0/com.runcheck/databases/runcheck.db")

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceNetworkSample()

            viewModel.startSpeedTest()
            runCurrent()

            val failedPhase = viewModel.speedTestState.value.phase as SpeedTestPhase.Failed
            assertEquals(UiText.Resource(R.string.speed_test_error_generic), failedPhase.error)
            assertFalse(viewModel.speedTestState.value.isRunning)
            viewModel.stopObserving()
        }

    @Test
    fun `partial speed test failure does not finalize a result`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns MutableStateFlow(testNetworkState)

            every { runSpeedTest(any()) } returns
                flowOf(
                    SpeedTestProgress.DownloadPhase(currentMbps = 85.5, progress = 0.5f),
                    SpeedTestProgress.Failed("Server unavailable"),
                )

            viewModel = createViewModel()
            viewModel.startObserving()
            advanceNetworkSample()

            viewModel.startSpeedTest()
            runCurrent()

            val speedState = viewModel.speedTestState.value
            assertTrue(speedState.phase is SpeedTestPhase.Failed)
            assertFalse(speedState.isRunning)
            assertEquals(85.5, speedState.downloadMbps, 0.01)
            coVerify(exactly = 0) { finalizeSpeedTest(any()) }
            viewModel.stopObserving()
        }

    @Test
    fun `selected network history period restores from saved state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getMeasuredNetworkState() } returns MutableStateFlow(testNetworkState)

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
            advanceNetworkSample()

            val state = viewModel.networkUiState.value as NetworkUiState.Success
            assertEquals(com.runcheck.domain.model.HistoryPeriod.MONTH, state.selectedHistoryPeriod)
            verify { getNetworkHistory(com.runcheck.domain.model.HistoryPeriod.MONTH) }
            viewModel.stopObserving()
        }
}
