package com.runcheck.ui.settings

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.runcheck.R
import com.runcheck.billing.ProPurchaseManager
import com.runcheck.billing.ProPurchaseRefreshResult
import com.runcheck.billing.PurchaseEvent
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.InsightDebugActions
import com.runcheck.domain.repository.SpeedTestRepository
import com.runcheck.domain.usecase.ClearMonitoringDataUseCase
import com.runcheck.domain.usecase.ExportDataUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.ObserveSettingsUseCase
import com.runcheck.domain.usecase.SetDataRetentionUseCase
import com.runcheck.domain.usecase.SetMonitoringIntervalUseCase
import com.runcheck.domain.usecase.SetNotificationsEnabledUseCase
import com.runcheck.domain.usecase.SettingsData
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.ui.common.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeSettings: ObserveSettingsUseCase = mockk()
    private val proPurchaseManager: ProPurchaseManager = mockk()
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private val isProUser: IsProUserUseCase = mockk()
    private val clearMonitoringDataUseCase: ClearMonitoringDataUseCase = mockk(relaxed = true)
    private val exportDataUseCase: ExportDataUseCase = mockk(relaxed = true)
    private val setDataRetentionUseCase: SetDataRetentionUseCase = mockk(relaxed = true)
    private val setMonitoringIntervalUseCase: SetMonitoringIntervalUseCase = mockk(relaxed = true)
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase = mockk(relaxed = true)
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk(relaxed = true)
    private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase = mockk(relaxed = true)
    private val speedTestRepository: SpeedTestRepository = mockk(relaxed = true)
    private val insightDebugActions: InsightDebugActions = mockk()

    @Before
    fun setUp() {
        every { observeSettings() } returns flowOf(SettingsData(preferences = UserPreferences(), deviceProfile = null))
        every { proPurchaseManager.isProUser } returns MutableStateFlow(false)
        every { proPurchaseManager.billingAvailable } returns flowOf(false)
        every { proPurchaseManager.purchaseEvents } returns MutableSharedFlow<PurchaseEvent>()
        every { proPurchaseManager.hasPendingPurchase } returns flowOf(false)
        coEvery { proPurchaseManager.getFormattedPrice() } returns null
        coEvery { proPurchaseManager.refreshPurchaseStatus() } returns ProPurchaseRefreshResult.NOT_ACTIVE
        every { proPurchaseManager.launchPurchaseFlow(any()) } returns Unit
        every { observeProAccess() } returns flowOf(false)
        every { isProUser() } returns false
        every { insightDebugActions.isAvailable } returns true
        coEvery { insightDebugActions.seedDemoInsights() } returns 9
    }

    @Test
    fun `debug availability is exposed in ui state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()

            runCurrent()

            assertTrue(viewModel.uiState.value.debugInsightsAvailable)
        }

    @Test
    fun `seed demo insights updates status and clears busy flag`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.seedDemoInsights()
            runCurrent()

            coVerify(exactly = 1) { insightDebugActions.seedDemoInsights() }
            assertFalse(viewModel.uiState.value.isProcessingDebugInsights)
            assertEquals(
                UiText.Resource(R.string.settings_debug_insights_seeded),
                viewModel.uiState.value.debugStatus,
            )
        }

    @Test
    fun `seed demo insights exposes failure and clears busy flag`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { insightDebugActions.seedDemoInsights() } throws IllegalStateException("seed failed")
            val viewModel = createViewModel()
            runCurrent()

            viewModel.seedDemoInsights()
            runCurrent()

            assertFalse(viewModel.uiState.value.isProcessingDebugInsights)
            assertEquals(
                UiText.Resource(R.string.common_error_generic),
                viewModel.uiState.value.errorMessage,
            )
            assertEquals(null, viewModel.uiState.value.debugStatus)
        }

    @Test
    fun `debug actions stay hidden and are not invoked when unavailable`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { insightDebugActions.isAvailable } returns false

            val viewModel = createViewModel()
            runCurrent()

            assertFalse(viewModel.uiState.value.debugInsightsAvailable)

            viewModel.seedDemoInsights()
            runCurrent()

            coVerify(exactly = 0) { insightDebugActions.seedDemoInsights() }
            assertFalse(viewModel.uiState.value.isProcessingDebugInsights)
        }

    @Test
    fun `settings pro state uses centralized pro access`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { proPurchaseManager.isProUser } returns MutableStateFlow(false)
            every { observeProAccess() } returns flowOf(true)
            every { isProUser() } returns true

            val viewModel = createViewModel()
            runCurrent()

            assertTrue(viewModel.uiState.value.isPro)
        }

    @Test
    fun `purchase pro reports billing unavailable when billing is disconnected`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.purchasePro(mockk())

            assertEquals(
                UiText.Resource(R.string.settings_billing_unavailable),
                viewModel.uiState.value.billingStatus,
            )
        }

    @Test
    fun `purchase pro starts purchase flow when billing is available`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { proPurchaseManager.billingAvailable } returns flowOf(true)
            val activity = mockk<Activity>()
            val viewModel = createViewModel()
            runCurrent()

            viewModel.purchasePro(activity)

            io.mockk.verify(exactly = 1) { proPurchaseManager.launchPurchaseFlow(activity) }
            assertEquals(null, viewModel.uiState.value.billingStatus)
        }

    @Test
    fun `refresh purchase status exposes active restore message`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { proPurchaseManager.refreshPurchaseStatus() } returns ProPurchaseRefreshResult.ACTIVE
            val viewModel = createViewModel()
            runCurrent()

            viewModel.refreshPurchaseStatus()
            runCurrent()

            assertEquals(
                UiText.Resource(R.string.settings_restore_success),
                viewModel.uiState.value.billingStatus,
            )
        }

    @Test
    fun `refresh purchase status clearly reports when no purchase is found`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.refreshPurchaseStatus()
            runCurrent()

            assertEquals(
                UiText.Resource(R.string.settings_restore_not_found),
                viewModel.uiState.value.billingStatus,
            )
        }

    @Test
    fun `refresh purchase status reports a transient billing outage`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery {
                proPurchaseManager.refreshPurchaseStatus()
            } returns ProPurchaseRefreshResult.UNAVAILABLE
            val viewModel = createViewModel()
            runCurrent()

            viewModel.refreshPurchaseStatus()
            runCurrent()

            assertEquals(
                UiText.Resource(R.string.settings_restore_unavailable),
                viewModel.uiState.value.billingStatus,
            )
        }

    @Test
    fun `export data blocks non pro users before creating export files`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns false
            val viewModel = createViewModel()
            runCurrent()

            viewModel.exportData()
            runCurrent()

            coVerify(exactly = 0) { exportDataUseCase.prepareExportShare() }
            assertEquals(
                UiText.Resource(R.string.pro_feature_locked_generic),
                viewModel.uiState.value.errorMessage,
            )
            assertFalse(viewModel.uiState.value.isExporting)
        }

    @Test
    fun `export data prepares share uris for pro users`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns true
            coEvery { exportDataUseCase.prepareExportShare() } returns listOf("content://runcheck/export.csv")
            val viewModel = createViewModel()
            runCurrent()

            viewModel.exportData()
            runCurrent()

            coVerify(exactly = 1) { exportDataUseCase.prepareExportShare() }
            assertEquals(listOf("content://runcheck/export.csv"), viewModel.uiState.value.exportUris)
            assertEquals(
                UiText.Resource(R.string.settings_export_ready),
                viewModel.uiState.value.exportStatus,
            )
            assertFalse(viewModel.uiState.value.isExporting)
        }

    @Test
    fun `reset tips reports success only after persistence succeeds`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val persistenceGate = CompletableDeferred<Unit>()
            coEvery { manageInfoCardDismissals.resetDismissedCards() } coAnswers {
                persistenceGate.await()
            }
            val viewModel = createViewModel()
            runCurrent()

            viewModel.resetTips()
            runCurrent()

            coVerify(exactly = 1) { manageInfoCardDismissals.resetDismissedCards() }
            assertEquals(null, viewModel.uiState.value.clearDataStatus)

            persistenceGate.complete(Unit)
            runCurrent()

            assertEquals(
                UiText.Resource(R.string.settings_reset_tips_done),
                viewModel.uiState.value.clearDataStatus,
            )
        }

    @Test
    fun `reset tips failure is not presented as success`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery {
                manageInfoCardDismissals.resetDismissedCards()
            } throws IllegalStateException("failed")
            val viewModel = createViewModel()
            runCurrent()

            viewModel.resetTips()
            runCurrent()

            assertEquals(null, viewModel.uiState.value.clearDataStatus)
            assertEquals(
                UiText.Resource(R.string.common_error_generic),
                viewModel.uiState.value.errorMessage,
            )
        }

    @Test
    fun `formatted pro price is exposed when billing manager returns price`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { proPurchaseManager.getFormattedPrice() } returns "$4.99"

            val viewModel = createViewModel()
            runCurrent()

            assertEquals("$4.99", viewModel.uiState.value.proPrice)
        }

    @Test
    fun `purchase events update and clear billing status`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val purchaseEvents = MutableSharedFlow<PurchaseEvent>()
            every { proPurchaseManager.purchaseEvents } returns purchaseEvents
            val viewModel = createViewModel()
            runCurrent()

            purchaseEvents.emit(PurchaseEvent.Pending)
            runCurrent()
            assertEquals(
                UiText.Resource(R.string.billing_purchase_pending),
                viewModel.uiState.value.billingStatus,
            )

            purchaseEvents.emit(PurchaseEvent.Error("Billing failed"))
            runCurrent()
            assertEquals(UiText.Dynamic("Billing failed"), viewModel.uiState.value.billingStatus)

            purchaseEvents.emit(PurchaseEvent.AlreadyOwned)
            runCurrent()
            assertEquals(
                UiText.Resource(R.string.billing_already_owned),
                viewModel.uiState.value.billingStatus,
            )

            purchaseEvents.emit(PurchaseEvent.Canceled)
            runCurrent()
            assertEquals(null, viewModel.uiState.value.billingStatus)
        }

    @Test
    fun `pending billing status follows tracked purchase state and clears after cancellation`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val hasPendingPurchase = MutableStateFlow(false)
            every { proPurchaseManager.hasPendingPurchase } returns hasPendingPurchase
            val viewModel = createViewModel()
            runCurrent()

            hasPendingPurchase.value = true
            runCurrent()
            assertEquals(
                UiText.Resource(R.string.billing_purchase_pending),
                viewModel.uiState.value.billingStatus,
            )

            hasPendingPurchase.value = false
            runCurrent()
            assertEquals(null, viewModel.uiState.value.billingStatus)
        }

    @Test
    fun `clear speed tests deletes saved speed test history`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.clearSpeedTests()
            runCurrent()

            coVerify(exactly = 1) { speedTestRepository.deleteAll() }
            assertEquals(
                UiText.Resource(R.string.settings_speed_tests_cleared),
                viewModel.uiState.value.clearDataStatus,
            )
        }

    @Test
    fun `clear all data delegates to monitoring data cleanup`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            runCurrent()

            viewModel.clearAllData()
            runCurrent()

            coVerify(exactly = 1) { clearMonitoringDataUseCase.invoke() }
            assertEquals(
                UiText.Resource(R.string.settings_data_cleared),
                viewModel.uiState.value.clearDataStatus,
            )
        }

    @Test
    fun `preference update delegates and reports generic error on failure`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { manageUserPreferences.setAlertTempThreshold(42) } throws IllegalStateException("failed")
            val viewModel = createViewModel()
            runCurrent()

            viewModel.setAlertBatteryThreshold(25)
            runCurrent()
            coVerify(exactly = 1) { manageUserPreferences.setAlertBatteryThreshold(25) }

            viewModel.setAlertTempThreshold(42)
            runCurrent()

            assertEquals(
                UiText.Resource(R.string.common_error_generic),
                viewModel.uiState.value.errorMessage,
            )
        }

    @Test
    fun `active export rejects reset and duplicate export until completion`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns true
            val exportGate = CompletableDeferred<Unit>()
            val uris = listOf("content://runcheck/export.csv")
            coEvery { exportDataUseCase.prepareExportShare() } coAnswers {
                exportGate.await()
                uris
            }
            val viewModel = createViewModel()
            runCurrent()

            viewModel.exportData()
            runCurrent()
            assertTrue(viewModel.uiState.value.isExporting)

            viewModel.clearAllData()
            viewModel.exportData()
            runCurrent()
            coVerify(exactly = 0) { clearMonitoringDataUseCase.invoke() }
            coVerify(exactly = 1) { exportDataUseCase.prepareExportShare() }
            assertEquals(null, viewModel.uiState.value.clearDataStatus)

            // A request made while busy must not run later when the dispatcher resumes.
            viewModel.clearAllData()
            exportGate.complete(Unit)
            runCurrent()
            coVerify(exactly = 0) { clearMonitoringDataUseCase.invoke() }
            assertFalse(viewModel.uiState.value.isExporting)
            assertEquals(uris, viewModel.uiState.value.exportUris)
            assertEquals(UiText.Resource(R.string.settings_export_ready), viewModel.uiState.value.exportStatus)

            assertResetSucceeds(viewModel)
        }

    @Test
    fun `active reset rejects export and duplicate reset until completion`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns true
            val resetGate = CompletableDeferred<Unit>()
            coEvery { clearMonitoringDataUseCase.invoke() } coAnswers { resetGate.await() }
            val uris = listOf("content://runcheck/export.csv")
            coEvery { exportDataUseCase.prepareExportShare() } returns uris
            val viewModel = createViewModel()
            runCurrent()

            viewModel.clearAllData()
            runCurrent()
            viewModel.exportData()
            viewModel.clearAllData()
            runCurrent()
            coVerify(exactly = 0) { exportDataUseCase.prepareExportShare() }
            coVerify(exactly = 1) { clearMonitoringDataUseCase.invoke() }
            assertFalse(viewModel.uiState.value.isExporting)
            assertEquals(null, viewModel.uiState.value.clearDataStatus)

            viewModel.exportData()
            resetGate.complete(Unit)
            runCurrent()
            coVerify(exactly = 0) { exportDataUseCase.prepareExportShare() }
            assertEquals(UiText.Resource(R.string.settings_data_cleared), viewModel.uiState.value.clearDataStatus)

            assertExportSucceeds(viewModel, uris)
        }

    @Test
    fun `export exception releases guard and preserves export error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns true
            coEvery { exportDataUseCase.prepareExportShare() } throws IllegalStateException("export failed")
            val viewModel = createViewModel()
            runCurrent()

            viewModel.exportData()
            runCurrent()
            assertFalse(viewModel.uiState.value.isExporting)
            assertEquals(UiText.Resource(R.string.settings_export_error), viewModel.uiState.value.exportStatus)

            assertResetSucceeds(viewModel)
        }

    @Test
    fun `reset exception releases guard and preserves generic error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns true
            coEvery { clearMonitoringDataUseCase.invoke() } throws IllegalStateException("reset failed")
            val uris = listOf("content://runcheck/export.csv")
            coEvery { exportDataUseCase.prepareExportShare() } returns uris
            val viewModel = createViewModel()
            runCurrent()

            viewModel.clearAllData()
            runCurrent()
            assertEquals(null, viewModel.uiState.value.clearDataStatus)
            assertEquals(UiText.Resource(R.string.common_error_generic), viewModel.uiState.value.errorMessage)

            viewModel.exportData()
            runCurrent()
            coVerify(exactly = 1) { exportDataUseCase.prepareExportShare() }
            assertEquals(uris, viewModel.uiState.value.exportUris)
        }

    @Test
    fun `export cancellation releases guard and busy state without reporting failure`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns true
            val operationJob = CompletableDeferred<Job>()
            coEvery { exportDataUseCase.prepareExportShare() } coAnswers {
                operationJob.complete(currentCoroutineContext().job)
                awaitCancellation()
            }
            val viewModel = createViewModel()
            runCurrent()

            viewModel.exportData()
            runCurrent()
            val job = operationJob.await()
            job.cancel()
            runCurrent()
            assertTrue(job.isCancelled)
            assertTrue(job.isCompleted)
            assertFalse(viewModel.uiState.value.isExporting)
            assertEquals(null, viewModel.uiState.value.exportStatus)
            assertEquals(null, viewModel.uiState.value.exportUris)
            assertEquals(null, viewModel.uiState.value.errorMessage)

            assertResetSucceeds(viewModel)
        }

    @Test
    fun `reset cancellation releases guard without reporting failure`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns true
            val operationJob = CompletableDeferred<Job>()
            coEvery { clearMonitoringDataUseCase.invoke() } coAnswers {
                operationJob.complete(currentCoroutineContext().job)
                awaitCancellation()
            }
            val uris = listOf("content://runcheck/export.csv")
            coEvery { exportDataUseCase.prepareExportShare() } returns uris
            val viewModel = createViewModel()
            runCurrent()

            viewModel.clearAllData()
            runCurrent()
            val job = operationJob.await()
            job.cancel()
            runCurrent()
            assertTrue(job.isCancelled)
            assertTrue(job.isCompleted)
            assertEquals(null, viewModel.uiState.value.clearDataStatus)
            assertEquals(null, viewModel.uiState.value.errorMessage)

            assertExportSucceeds(viewModel, uris)
        }

    @Test
    fun `cancelled viewmodel scope cannot start export or reset`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns true
            val viewModel = createViewModel()
            runCurrent()
            viewModel.viewModelScope.cancel()

            viewModel.exportData()
            viewModel.clearAllData()
            runCurrent()

            coVerify(exactly = 0) { exportDataUseCase.prepareExportShare() }
            coVerify(exactly = 0) { clearMonitoringDataUseCase.invoke() }
            assertFalse(viewModel.uiState.value.isExporting)
            assertEquals(null, viewModel.uiState.value.exportStatus)
            assertEquals(null, viewModel.uiState.value.clearDataStatus)
            assertEquals(null, viewModel.uiState.value.errorMessage)
        }

    private fun TestScope.assertResetSucceeds(viewModel: SettingsViewModel) {
        viewModel.clearAllData()
        runCurrent()
        coVerify(exactly = 1) { clearMonitoringDataUseCase.invoke() }
        assertEquals(UiText.Resource(R.string.settings_data_cleared), viewModel.uiState.value.clearDataStatus)
    }

    private fun TestScope.assertExportSucceeds(
        viewModel: SettingsViewModel,
        uris: List<String>,
    ) {
        viewModel.exportData()
        runCurrent()
        coVerify(exactly = 1) { exportDataUseCase.prepareExportShare() }
        assertEquals(uris, viewModel.uiState.value.exportUris)
        assertEquals(UiText.Resource(R.string.settings_export_ready), viewModel.uiState.value.exportStatus)
    }

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            observeSettings = observeSettings,
            proPurchaseManager = proPurchaseManager,
            observeProAccess = observeProAccess,
            isProUser = isProUser,
            clearMonitoringDataUseCase = clearMonitoringDataUseCase,
            exportDataUseCase = exportDataUseCase,
            setDataRetentionUseCase = setDataRetentionUseCase,
            setMonitoringIntervalUseCase = setMonitoringIntervalUseCase,
            setNotificationsEnabledUseCase = setNotificationsEnabledUseCase,
            manageUserPreferences = manageUserPreferences,
            manageInfoCardDismissals = manageInfoCardDismissals,
            speedTestRepository = speedTestRepository,
            insightDebugActions = insightDebugActions,
        )
}
