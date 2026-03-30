package com.runcheck.ui.settings

import com.runcheck.R
import com.runcheck.billing.ProPurchaseManager
import com.runcheck.billing.PurchaseEvent
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.InsightDebugActions
import com.runcheck.domain.repository.SpeedTestRepository
import com.runcheck.domain.usecase.ClearMonitoringDataUseCase
import com.runcheck.domain.usecase.ExportDataUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
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
                UiText.Dynamic("Demo insights seeded (9 active)"),
                viewModel.uiState.value.debugStatus,
            )
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

    private fun createViewModel(): SettingsViewModel =
        SettingsViewModel(
            observeSettings = observeSettings,
            proPurchaseManager = proPurchaseManager,
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
