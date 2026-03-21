package com.runcheck.ui.storage.cleanup

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import com.runcheck.domain.model.CleanupGroupSummary
import com.runcheck.domain.model.CleanupScanQuery
import com.runcheck.domain.model.CleanupSummary
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.usecase.StorageCleanupUseCase
import com.runcheck.ui.MainDispatcherRule
import com.runcheck.ui.common.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val testFiles = listOf(
        ScannedFile(
            uri = "content://media/1",
            displayName = "video1.mp4",
            sizeBytes = 500_000_000L,
            mimeType = "video/mp4",
            dateModified = System.currentTimeMillis() - 86_400_000,
            category = MediaCategory.VIDEO
        ),
        ScannedFile(
            uri = "content://media/2",
            displayName = "video2.mp4",
            sizeBytes = 200_000_000L,
            mimeType = "video/mp4",
            dateModified = System.currentTimeMillis() - 172_800_000,
            category = MediaCategory.VIDEO
        ),
        ScannedFile(
            uri = "content://media/3",
            displayName = "photo1.jpg",
            sizeBytes = 80_000_000L,
            mimeType = "image/jpeg",
            dateModified = System.currentTimeMillis() - 259_200_000,
            category = MediaCategory.IMAGE
        ),
        ScannedFile(
            uri = "content://media/4",
            displayName = "document1.pdf",
            sizeBytes = 60_000_000L,
            mimeType = "application/pdf",
            dateModified = System.currentTimeMillis() - 345_600_000,
            category = MediaCategory.DOCUMENT
        )
    )

    private val storageState = StorageState(
        totalBytes = 128_000_000_000L,
        availableBytes = 64_000_000_000L,
        usedBytes = 64_000_000_000L,
        usagePercent = 50f,
        appsBytes = null,
        totalCacheBytes = null,
        appCount = null,
        mediaBreakdown = null,
        trashInfo = null,
        sdCardAvailable = false,
        sdCardTotalBytes = null,
        sdCardAvailableBytes = null,
        fileSystemType = null,
        encryptionStatus = null,
        storageVolumes = 1
    )

    @Before
    fun setup() {
        coEvery { storageCleanup.getCurrentStorageState() } returns storageState
        coEvery { storageCleanup.findExistingUris(any()) } returns emptySet()
    }

    private fun createSummary(files: List<ScannedFile>): CleanupSummary {
        val groups = files.groupBy { it.category }
            .map { (category, categoryFiles) ->
                CleanupGroupSummary(
                    category = category,
                    itemCount = categoryFiles.size,
                    totalBytes = categoryFiles.sumOf { it.sizeBytes }
                )
            }
            .sortedByDescending { it.totalBytes }
        return CleanupSummary(
            groups = groups,
            totalCount = files.size,
            totalBytes = files.sumOf { it.sizeBytes },
            maxFileSizeBytes = files.maxOfOrNull { it.sizeBytes } ?: 0L
        )
    }

    private fun stubCleanupData(files: List<ScannedFile>) {
        val summary = createSummary(files)
        coEvery { storageCleanup.getCleanupSummary(any()) } returns summary
        every { storageCleanup.getCleanupItems(any(), any()) } answers {
            val category = secondArg<MediaCategory>()
            flowOf(PagingData.from(files.filter { it.category == category }))
        }
        coEvery { storageCleanup.getCleanupGroupUris(any(), any()) } answers {
            val category = secondArg<MediaCategory>()
            files.filter { it.category == category }.map { it.uri }.toSet()
        }
    }

    private fun createViewModel(
        type: String = "LARGE_FILES",
        files: List<ScannedFile> = testFiles,
        savedStateValues: Map<String, Any> = mapOf("type" to type)
    ): CleanupViewModel {
        stubCleanupData(files)
        val savedStateHandle = SavedStateHandle(savedStateValues)
        return CleanupViewModel(
            savedStateHandle = savedStateHandle,
            storageCleanup = storageCleanup
        )
    }

    @Test
    fun `scan produces results with correct grouping`() = runTest(mainDispatcherRule.testDispatcher) {
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
    fun `toggle file selection updates selected state`() = runTest(mainDispatcherRule.testDispatcher) {
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
    fun `toggle group selection selects and deselects all files in group`() = runTest(mainDispatcherRule.testDispatcher) {
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
    fun `empty scan result produces Empty state`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(files = emptyList())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CleanupUiState.Empty, state)
    }

    @Test
    fun `scan failure produces Error state instead of Empty`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { storageCleanup.getCleanupSummary(any()) } throws IllegalStateException("boom")
        every { storageCleanup.getCleanupItems(any(), any()) } returns flowOf(PagingData.empty())

        val savedStateHandle = SavedStateHandle(mapOf("type" to "LARGE_FILES"))
        val viewModel = CleanupViewModel(
            savedStateHandle = savedStateHandle,
            storageCleanup = storageCleanup
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CleanupUiState.Error)
        assertEquals(
            UiText.Resource(com.runcheck.R.string.common_error_generic),
            (state as CleanupUiState.Error).message
        )
    }

    @Test
    fun `delete success triggers re-scan`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleSelection(testFiles[0])
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
    fun `APK type preselects all files`() = runTest(mainDispatcherRule.testDispatcher) {
        val apkFiles = listOf(
            ScannedFile(
                uri = "content://media/10",
                displayName = "app1.apk",
                sizeBytes = 50_000_000L,
                mimeType = "application/vnd.android.package-archive",
                dateModified = System.currentTimeMillis(),
                category = MediaCategory.APK
            ),
            ScannedFile(
                uri = "content://media/11",
                displayName = "app2.apk",
                sizeBytes = 30_000_000L,
                mimeType = "application/vnd.android.package-archive",
                dateModified = System.currentTimeMillis(),
                category = MediaCategory.APK
            )
        )

        val viewModel = createViewModel(type = "APK_FILES", files = apkFiles)
        advanceUntilIdle()

        val state = viewModel.uiState.value as CleanupUiState.Results
        assertEquals(2, state.selectedCount)
        assertEquals(80_000_000L, state.selectedSize)
    }

    @Test
    fun `projected usage percent decreases when files are selected`() = runTest(mainDispatcherRule.testDispatcher) {
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
    fun `partially deselecting selected group updates counts and size`() = runTest(mainDispatcherRule.testDispatcher) {
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
    fun `selected filter restores from saved state`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(
            savedStateValues = mapOf(
                "type" to "LARGE_FILES",
                "cleanup_selected_filter" to 2
            )
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.getSelectedFilterIndex())
        coVerify {
            storageCleanup.getCleanupSummary(
                match { query -> query.filterValue == 100L * 1024 * 1024 }
            )
        }
    }
}
