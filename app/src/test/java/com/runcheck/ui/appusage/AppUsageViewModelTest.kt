package com.runcheck.ui.appusage

import androidx.paging.PagingData
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.AppUsageListSummary
import com.runcheck.domain.usecase.GetAppBatteryUsageSummaryUseCase
import com.runcheck.domain.usecase.GetAppBatteryUsageUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.RefreshAppUsageSnapshotUseCase
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.ui.common.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppUsageViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getAppBatteryUsage: GetAppBatteryUsageUseCase = mockk(relaxed = true)
    private val getAppBatteryUsageSummary: GetAppBatteryUsageSummaryUseCase = mockk()
    private val refreshAppUsageSnapshot: RefreshAppUsageSnapshotUseCase = mockk(relaxed = true)
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private val isProUser: IsProUserUseCase = mockk()

    @Test
    fun `refresh shows locked state for non pro users`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns false
            val viewModel = createViewModel()

            viewModel.refresh()

            assertEquals(AppUsageUiState.Locked, viewModel.uiState.value)
            coVerify(exactly = 0) { refreshAppUsageSnapshot() }
        }

    @Test
    fun `startObserving loads summary when pro access is available`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { observeProAccess() } returns flowOf(true)
            every { getAppBatteryUsage(any()) } returns flowOf(PagingData.empty<AppBatteryUsage>())
            every { getAppBatteryUsageSummary(any()) } returns
                flowOf(AppUsageListSummary(totalForegroundTimeMs = 1_000L, maxForegroundTimeMs = 700L))
            val viewModel = createViewModel()

            try {
                viewModel.startObserving()
                runCurrent()

                assertEquals(
                    AppUsageUiState.Success(totalForegroundTimeMs = 1_000L, maxForegroundTimeMs = 700L),
                    viewModel.uiState.value,
                )
                coVerify(exactly = 1) { refreshAppUsageSnapshot() }
            } finally {
                viewModel.stopObserving()
            }
        }

    @Test
    fun `refresh exposes error message when snapshot refresh fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns true
            coEvery { refreshAppUsageSnapshot() } throws IllegalStateException("usage failed")
            val viewModel = createViewModel()

            viewModel.refresh()
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is AppUsageUiState.Error)
            assertEquals(UiText.Dynamic("usage failed"), (state as AppUsageUiState.Error).message)
        }

    private fun createViewModel(): AppUsageViewModel =
        AppUsageViewModel(
            getAppBatteryUsage = getAppBatteryUsage,
            getAppBatteryUsageSummary = getAppBatteryUsageSummary,
            refreshAppUsageSnapshot = refreshAppUsageSnapshot,
            observeProAccess = observeProAccess,
            isProUser = isProUser,
        )
}
