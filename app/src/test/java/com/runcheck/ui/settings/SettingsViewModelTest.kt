package com.runcheck.ui.settings

import android.app.Activity
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
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
        every { proPurchaseManager.isProUser } returns flowOf(false)
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
    fun `settings pro state uses trial-aware pro access when purchase is inactive`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { proPurchaseManager.isProUser } returns flowOf(false)
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
