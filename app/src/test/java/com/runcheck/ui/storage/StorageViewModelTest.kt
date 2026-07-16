package com.runcheck.ui.storage

import androidx.lifecycle.SavedStateHandle
import com.runcheck.R
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.usecase.GetStorageHistoryUseCase
import com.runcheck.domain.usecase.GetStorageStateUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.StorageCleanupUseCase
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
class StorageViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getStorageState: GetStorageStateUseCase = mockk()
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private val isProUser: IsProUserUseCase = mockk()
    private val storageCleanup: StorageCleanupUseCase = mockk()
    private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase = mockk()
    private val manageUserPreferences: ManageUserPreferencesUseCase = mockk()
    private val getStorageHistory: GetStorageHistoryUseCase = mockk()

    @Test
    fun `empty trash does not query trashed uris without pro access`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { getStorageState() } returns
                flowOf(
                    StorageState(
                        totalBytes = 1L,
                        availableBytes = 1L,
                        usedBytes = 0L,
                        usagePercent = 0f,
                    ),
                )
            every { observeProAccess() } returns flowOf(false)
            every { isProUser() } returns false
            every { manageInfoCardDismissals.observeDismissedCardIds() } returns flowOf(emptySet())
            every { manageUserPreferences.observePreferences() } returns flowOf(UserPreferences())
            every { getStorageHistory(HistoryPeriod.WEEK) } returns flowOf(emptyList())
            coEvery { storageCleanup.getTrashedUris() } returns listOf("content://media/1")

            val viewModel = createViewModel()
            viewModel.startObserving()
            runCurrent()

            viewModel.emptyTrash()
            runCurrent()

            coVerify(exactly = 0) { storageCleanup.getTrashedUris() }
        }

    @Test
    fun `empty trash emits no delete request when trash is empty`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepareProStorageState()
            coEvery { storageCleanup.getTrashedUris() } returns emptyList()
            val viewModel = createViewModel()

            viewModel.emptyTrash()
            runCurrent()

            assertTrue(viewModel.trashDeleteRequestUris.replayCache.isEmpty())
        }

    @Test
    fun `empty trash reports permission error when access is revoked during query`() =
        runTest(mainDispatcherRule.testDispatcher) {
            prepareProStorageState()
            coEvery { storageCleanup.getTrashedUris() } throws SecurityException("revoked")
            val viewModel = createViewModel()

            viewModel.emptyTrash()
            runCurrent()

            assertEquals(
                StorageUiState.Error(UiText.Resource(R.string.cleanup_delete_permission_error)),
                viewModel.uiState.value,
            )
            assertTrue(viewModel.trashDeleteRequestUris.replayCache.isEmpty())
        }

    private fun prepareProStorageState() {
        every { getStorageState() } returns
            flowOf(
                StorageState(
                    totalBytes = 1L,
                    availableBytes = 1L,
                    usedBytes = 0L,
                    usagePercent = 0f,
                ),
            )
        every { observeProAccess() } returns flowOf(true)
        every { isProUser() } returns true
        every { manageInfoCardDismissals.observeDismissedCardIds() } returns flowOf(emptySet())
        every { manageUserPreferences.observePreferences() } returns flowOf(UserPreferences())
        every { getStorageHistory(HistoryPeriod.WEEK) } returns flowOf(emptyList())
    }

    private fun createViewModel(): StorageViewModel =
        StorageViewModel(
            savedStateHandle = SavedStateHandle(),
            getStorageState = getStorageState,
            observeProAccess = observeProAccess,
            isProUser = isProUser,
            storageCleanup = storageCleanup,
            manageInfoCardDismissals = manageInfoCardDismissals,
            manageUserPreferences = manageUserPreferences,
            getStorageHistory = getStorageHistory,
        )
}
