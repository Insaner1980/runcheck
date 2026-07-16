package com.runcheck.ui.storage.cleanup

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.runcheck.R
import com.runcheck.domain.model.CleanupGroupSummary
import com.runcheck.domain.model.CleanupScanQuery
import com.runcheck.domain.model.CleanupSummary
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.domain.usecase.StorageCleanupUseCase
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.ui.common.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CleanupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val storageCleanup: StorageCleanupUseCase = mockk()
    private val observeProAccess: ObserveProAccessUseCase = mockk()
    private val isProUser: IsProUserUseCase = mockk()
    private val proAccessFlow = MutableStateFlow(true)

    private val testFiles =
        listOf(
            ScannedFile(
                uri = "content://media/1",
                displayName = "video1.mp4",
                sizeBytes = 500_000_000L,
                mimeType = "video/mp4",
                dateModified = System.currentTimeMillis() - 86_400_000,
                category = MediaCategory.VIDEO,
            ),
            ScannedFile(
                uri = "content://media/2",
                displayName = "video2.mp4",
                sizeBytes = 200_000_000L,
                mimeType = "video/mp4",
                dateModified = System.currentTimeMillis() - 172_800_000,
                category = MediaCategory.VIDEO,
            ),
            ScannedFile(
                uri = "content://media/3",
                displayName = "photo1.jpg",
                sizeBytes = 80_000_000L,
                mimeType = "image/jpeg",
                dateModified = System.currentTimeMillis() - 259_200_000,
                category = MediaCategory.IMAGE,
            ),
            ScannedFile(
                uri = "content://media/4",
                displayName = "document1.pdf",
                sizeBytes = 60_000_000L,
                mimeType = "application/pdf",
                dateModified = System.currentTimeMillis() - 345_600_000,
                category = MediaCategory.DOCUMENT,
            ),
        )

    private val storageState =
        StorageState(
            totalBytes = 128_000_000_000L,
            availableBytes = 64_000_000_000L,
            usedBytes = 64_000_000_000L,
            usagePercent = 50f,
            appsBytes = null,
            totalCacheBytes = null,
            appCount = null,
            mediaBreakdown = null,
            trashInfo = null,
            removableStorageAvailable = false,
            removableStorageTotalBytes = null,
            removableStorageAvailableBytes = null,
            fileSystemType = null,
            encryptionStatus = null,
            storageVolumes = 1,
        )

    @Before
    fun setup() {
        coEvery { storageCleanup.getCurrentStorageState() } returns storageState
        coEvery { storageCleanup.findExistingUris(any()) } returns emptySet()
        every { observeProAccess() } returns proAccessFlow
        every { isProUser() } answers { proAccessFlow.value }
    }

    private fun createSummary(files: List<ScannedFile>): CleanupSummary {
        val groups =
            files
                .groupBy { it.category }
                .map { (category, categoryFiles) ->
                    CleanupGroupSummary(
                        category = category,
                        itemCount = categoryFiles.size,
                        totalBytes = categoryFiles.sumOf { it.sizeBytes },
                    )
                }.sortedByDescending { it.totalBytes }
        return CleanupSummary(
            groups = groups,
            totalCount = files.size,
            totalBytes = files.sumOf { it.sizeBytes },
            maxFileSizeBytes = files.maxOfOrNull { it.sizeBytes } ?: 0L,
        )
    }

    private fun stubCleanupData(files: List<ScannedFile>) {
        val summary = createSummary(files)
        coEvery { storageCleanup.getCleanupSummary(any()) } returns summary
        every { storageCleanup.getCleanupItems(any(), any()) } answers {
            val category = secondArg<MediaCategory>()
            flowOf(PagingData.from(files.filter { it.category == category }))
        }
        coEvery { storageCleanup.getCleanupGroupFileSizes(any(), any()) } answers {
            val category = secondArg<MediaCategory>()
            files.filter { it.category == category }.associate { it.uri to it.sizeBytes }
        }
    }

    private fun createViewModel(
        type: String = "LARGE_FILES",
        files: List<ScannedFile> = testFiles,
        savedStateValues: Map<String, Any> = mapOf("type" to type),
    ): CleanupViewModel {
        stubCleanupData(files)
        val savedStateHandle = SavedStateHandle(savedStateValues)
        return CleanupViewModel(
            savedStateHandle = savedStateHandle,
            storageCleanup = storageCleanup,
            observeProAccess = observeProAccess,
            isProUser = isProUser,
        )
    }

    private fun pendingDeleteState(files: List<ScannedFile>): Map<String, Any> =
        mapOf(
            "type" to "LARGE_FILES",
            "cleanup_pending_delete_uris" to ArrayList(files.map { it.uri }),
            "cleanup_pending_selected_groups" to arrayListOf<String>(),
            "cleanup_pending_selected_uris" to ArrayList(files.map { it.uri }),
            "cleanup_pending_deselected_uris" to arrayListOf<String>(),
            "cleanup_pending_metadata_uris" to ArrayList(files.map { it.uri }),
            "cleanup_pending_uri_categories" to ArrayList(files.map { it.category.name }),
            "cleanup_pending_uri_sizes" to files.map { it.sizeBytes }.toLongArray(),
        )

    @Test
    fun `cleanup scan returns pro locked error for non pro users`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { isProUser() } returns false
            proAccessFlow.value = false

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("Expected Error but got $state", state is CleanupUiState.Error)
            assertEquals(
                UiText.Resource(R.string.pro_feature_locked_generic),
                (state as CleanupUiState.Error).message,
            )
            coVerify(exactly = 0) { storageCleanup.getCleanupSummary(any()) }
        }

    @Test
    fun `scan produces results with correct grouping`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("Expected Results but got $state", state is CleanupUiState.Results)
            val results = state as CleanupUiState.Results

            assertEquals(3, results.groups.size)

            val videoGroup = results.groups[0]
            assertEquals(MediaCategory.VIDEO, videoGroup.category)
            assertEquals(2, videoGroup.itemCount)
            assertEquals(700_000_000L, videoGroup.totalBytes)

            val imageGroup = results.groups[1]
            assertEquals(MediaCategory.IMAGE, imageGroup.category)
            assertEquals(1, imageGroup.itemCount)
            assertEquals(80_000_000L, imageGroup.totalBytes)

            val docGroup = results.groups[2]
            assertEquals(MediaCategory.DOCUMENT, docGroup.category)
            assertEquals(1, docGroup.itemCount)
            assertEquals(60_000_000L, docGroup.totalBytes)

            assertEquals(840_000_000L, results.totalSize)
            assertEquals(4, results.totalCount)
            assertEquals(500_000_000L, results.maxFileSizeBytes)
            assertTrue(results.groups[0].expanded)
        }

    @Test
    fun `toggle file selection updates selected state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            var state = viewModel.uiState.value as CleanupUiState.Results
            assertEquals(0, state.selectedCount)
            assertEquals(0L, state.selectedSize)

            viewModel.toggleSelection(testFiles[0])
            advanceUntilIdle()

            state = viewModel.uiState.value as CleanupUiState.Results
            assertEquals(1, state.selectedCount)
            assertEquals(500_000_000L, state.selectedSize)
            assertTrue(viewModel.isSelected(testFiles[0]))

            viewModel.toggleSelection(testFiles[2])
            advanceUntilIdle()

            state = viewModel.uiState.value as CleanupUiState.Results
            assertEquals(2, state.selectedCount)
            assertEquals(580_000_000L, state.selectedSize)

            viewModel.toggleSelection(testFiles[0])
            advanceUntilIdle()

            state = viewModel.uiState.value as CleanupUiState.Results
            assertEquals(1, state.selectedCount)
            assertFalse(viewModel.isSelected(testFiles[0]))
            assertTrue(viewModel.isSelected(testFiles[2]))
            assertEquals(80_000_000L, state.selectedSize)
        }

    @Test
    fun `toggle group selection selects and deselects all files in group`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.toggleGroupSelection(MediaCategory.VIDEO)
            advanceUntilIdle()

            var state = viewModel.uiState.value as CleanupUiState.Results
            val videoGroup = state.groups.first { it.category == MediaCategory.VIDEO }
            assertEquals(2, videoGroup.selectedCount)
            assertEquals(2, state.selectedCount)
            assertEquals(700_000_000L, state.selectedSize)

            viewModel.toggleGroupSelection(MediaCategory.VIDEO)
            advanceUntilIdle()

            state = viewModel.uiState.value as CleanupUiState.Results
            assertEquals(0, state.selectedCount)
            assertEquals(0L, state.selectedSize)
        }

    @Test
    fun `trial expiry revokes cleanup results without recreating view model`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is CleanupUiState.Results)

            proAccessFlow.value = false
            runCurrent()

            assertEquals(
                CleanupUiState.Error(UiText.Resource(R.string.pro_feature_locked_generic)),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `whole group selection applies to files as their pages load`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.toggleGroupSelection(MediaCategory.VIDEO)

            assertTrue(viewModel.isSelected(testFiles[0]))
            assertTrue(viewModel.isSelected(testFiles[1]))
            val state = viewModel.uiState.value as CleanupUiState.Results
            assertEquals(2, state.groups.first { it.category == MediaCategory.VIDEO }.selectedCount)
        }

    @Test
    fun `filter change clears selection from the previous query`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.toggleSelection(testFiles[0])
            viewModel.toggleGroupSelection(MediaCategory.IMAGE)

            viewModel.setFilter(2)
            advanceUntilIdle()

            val state = viewModel.uiState.value as CleanupUiState.Results
            assertEquals(0, state.selectedCount)
            assertEquals(0L, state.selectedSize)
            assertFalse(viewModel.isSelected(testFiles[0]))
            assertFalse(viewModel.isSelected(testFiles[2]))
        }

    @Test
    fun `empty scan result produces Empty state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(files = emptyList())
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(CleanupUiState.Empty, state)
        }

    @Test
    fun `scan failure produces Error state instead of Empty`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { storageCleanup.getCleanupSummary(any()) } throws IllegalStateException("boom")
            every { storageCleanup.getCleanupItems(any(), any()) } returns flowOf(PagingData.empty())

            val savedStateHandle = SavedStateHandle(mapOf("type" to "LARGE_FILES"))
            val viewModel =
                CleanupViewModel(
                    savedStateHandle = savedStateHandle,
                    storageCleanup = storageCleanup,
                    observeProAccess = observeProAccess,
                    isProUser = isProUser,
                )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is CleanupUiState.Error)
            assertEquals(
                UiText.Resource(com.runcheck.R.string.common_error_generic),
                (state as CleanupUiState.Error).message,
            )
        }

    @Test
    fun `android 10 delete waits for explicit confirmation`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { storageCleanup.deleteLegacy(any()) } returns setOf(testFiles[0].uri)
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.toggleSelection(testFiles[0])

            viewModel.requestDelete(apiLevel = 29)
            advanceUntilIdle()

            assertEquals(1, viewModel.legacyDeleteConfirmationCount.value)
            coVerify(exactly = 0) { storageCleanup.deleteLegacy(any()) }

            viewModel.confirmLegacyDelete()
            advanceUntilIdle()

            assertEquals(null, viewModel.legacyDeleteConfirmationCount.value)
            coVerify(exactly = 1) { storageCleanup.deleteLegacy(listOf(testFiles[0].uri)) }
        }

    @Test
    fun `delete success triggers re-scan`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(savedStateValues = pendingDeleteState(listOf(testFiles[0])))
            advanceUntilIdle()

            viewModel.onDeleteConfirmed()
            advanceTimeBy(200L)
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is CleanupUiState.Success)

            advanceTimeBy(2000L)
            advanceUntilIdle()

            val stateAfterRescan = viewModel.uiState.value
            assertTrue(stateAfterRescan is CleanupUiState.Results)
            coVerify(atLeast = 2) { storageCleanup.getCleanupSummary(any()) }
        }

    @Test
    fun `android delete requests are split at the platform URI limit`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val files =
                (1..2_001).map { index ->
                    ScannedFile(
                        uri = "content://media/video/$index",
                        displayName = "video-$index.mp4",
                        sizeBytes = index.toLong(),
                        mimeType = "video/mp4",
                        dateModified = 0L,
                        category = MediaCategory.VIDEO,
                    )
                }
            val viewModel = createViewModel(files = files)
            advanceUntilIdle()
            viewModel.toggleGroupSelection(MediaCategory.VIDEO)

            val firstRequest =
                async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.deleteRequestUris.first()
                }
            viewModel.requestDelete(apiLevel = Build.VERSION_CODES.R)
            assertEquals(2_000, firstRequest.await().size)

            val secondRequest =
                async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.deleteRequestUris.first()
                }
            viewModel.onDeleteConfirmed()
            assertEquals(1, secondRequest.await().size)

            viewModel.onDeleteConfirmed()
            advanceTimeBy(200L)
            runCurrent()

            assertEquals(
                CleanupUiState.Success(files.sumOf { it.sizeBytes }),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `recreated process counts only files deleted from approved request`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { storageCleanup.findExistingUris(any()) } returns setOf(testFiles[1].uri)
            val recreatedViewModel =
                createViewModel(savedStateValues = pendingDeleteState(testFiles.take(2)))
            advanceUntilIdle()

            assertEquals(CleanupUiState.Deleting(2), recreatedViewModel.uiState.value)
            recreatedViewModel.onDeleteConfirmed()
            advanceTimeBy(200L)
            runCurrent()

            assertEquals(
                CleanupUiState.Success(testFiles[0].sizeBytes),
                recreatedViewModel.uiState.value,
            )
        }

    @Test
    fun `delete verification access failure is not counted as freed space`() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { storageCleanup.findExistingUris(any()) } throws SecurityException("revoked")
            val recreatedViewModel =
                createViewModel(savedStateValues = pendingDeleteState(listOf(testFiles[0])))
            advanceUntilIdle()

            recreatedViewModel.onDeleteConfirmed()
            advanceTimeBy(200L)
            runCurrent()

            assertEquals(
                CleanupUiState.Error(UiText.Resource(R.string.cleanup_delete_permission_error)),
                recreatedViewModel.uiState.value,
            )
        }

    @Test
    fun `partial delete keeps the remaining files selected after rescan`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.toggleGroupSelection(MediaCategory.VIDEO)
            viewModel.requestDelete(apiLevel = 30)
            advanceUntilIdle()

            val remainingFiles = testFiles.drop(1)
            coEvery { storageCleanup.findExistingUris(any()) } returns setOf(testFiles[1].uri)
            coEvery { storageCleanup.getCleanupSummary(any()) } returns createSummary(remainingFiles)
            every { storageCleanup.getCleanupItems(any(), any()) } answers {
                val category = secondArg<MediaCategory>()
                flowOf(PagingData.from(remainingFiles.filter { it.category == category }))
            }
            coEvery { storageCleanup.getCleanupGroupFileSizes(any(), any()) } answers {
                val category = secondArg<MediaCategory>()
                remainingFiles.filter { it.category == category }.associate { it.uri to it.sizeBytes }
            }

            viewModel.onDeleteConfirmed()
            advanceUntilIdle()

            val state = viewModel.uiState.value as CleanupUiState.Results
            assertEquals(1, state.selectedCount)
            assertEquals(testFiles[1].sizeBytes, state.selectedSize)
            assertTrue(viewModel.isSelected(testFiles[1]))
        }

    @Test
    fun `recreated process restores selection when delete request is cancelled`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val recreatedViewModel =
                createViewModel(savedStateValues = pendingDeleteState(listOf(testFiles[0])))
            advanceUntilIdle()
            recreatedViewModel.onDeleteCancelled()
            advanceUntilIdle()

            val state = recreatedViewModel.uiState.value as CleanupUiState.Results
            assertEquals(1, state.selectedCount)
            assertEquals(testFiles[0].sizeBytes, state.selectedSize)
            assertTrue(recreatedViewModel.isSelected(testFiles[0]))
        }

    @Test
    fun `APK cleanup is blocked on unsupported Android versions in JVM tests`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel(type = "APK_FILES", files = emptyList())
            advanceUntilIdle()

            assertEquals(CleanupUiState.UnsupportedVersion, viewModel.uiState.value)
        }

    @Test
    fun `APK type keeps preselect all configuration enabled`() {
        assertTrue(CleanupType.APK_FILES.preselectAll)
    }

    @Test
    fun `projected usage percent decreases when files are selected`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            val stateNoSelection = viewModel.uiState.value as CleanupUiState.Results
            val initialPct = stateNoSelection.currentUsagePercent

            viewModel.toggleSelection(testFiles[0])
            advanceUntilIdle()

            val stateWithSelection = viewModel.uiState.value as CleanupUiState.Results
            assertTrue(stateWithSelection.projectedUsagePercent < initialPct)
        }

    @Test
    fun `partially deselecting selected group updates counts and size`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.toggleGroupSelection(MediaCategory.VIDEO)
            advanceUntilIdle()
            viewModel.toggleSelection(testFiles[0])
            advanceUntilIdle()

            val state = viewModel.uiState.value as CleanupUiState.Results
            val videoGroup = state.groups.first { it.category == MediaCategory.VIDEO }
            assertEquals(1, videoGroup.selectedCount)
            assertEquals(1, state.selectedCount)
            assertEquals(200_000_000L, state.selectedSize)
        }

    @Test
    fun `selected filter restores from saved state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel =
                createViewModel(
                    savedStateValues =
                        mapOf(
                            "type" to "LARGE_FILES",
                            "cleanup_selected_filter" to 2,
                        ),
                )
            advanceUntilIdle()

            assertEquals(2, viewModel.getSelectedFilterIndex())
            coVerify {
                storageCleanup.getCleanupSummary(
                    match { query -> query.filterValue == 100L * 1_000_000 },
                )
            }
        }
}
