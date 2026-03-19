package com.runcheck.ui.storage.cleanup

import androidx.lifecycle.SavedStateHandle
import com.runcheck.data.storage.MediaStoreScanner
import com.runcheck.data.storage.StorageCleanupHelper
import com.runcheck.data.storage.StorageDataSource
import com.runcheck.data.storage.ThumbnailLoader
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.ScannedFile
import com.runcheck.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val mediaStoreScanner: MediaStoreScanner = mockk()
    private val cleanupHelper: StorageCleanupHelper = mockk(relaxed = true)
    private val storageDataSource: StorageDataSource = mockk()
    private val thumbnailLoader: ThumbnailLoader = mockk(relaxed = true)

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

    private val storageInfo = StorageDataSource.StorageInfo(
        totalBytes = 128_000_000_000L,
        availableBytes = 64_000_000_000L,
        usedBytes = 64_000_000_000L,
        appsBytes = null,
        totalCacheBytes = null,
        appCount = null,
        mediaBreakdown = null,
        trashInfo = null,
        sdCardAvailable = false,
        sdCardTotalBytes = null,
        sdCardAvailableBytes = null
    )

    @Before
    fun setup() {
        coEvery { storageDataSource.getStorageInfo() } returns storageInfo
    }

    private fun createViewModel(
        type: String = "LARGE_FILES",
        files: List<ScannedFile> = testFiles
    ): CleanupViewModel {
        coEvery { mediaStoreScanner.scanLargeFiles(any()) } returns files
        coEvery { mediaStoreScanner.scanOldDownloads(any()) } returns files
        coEvery { mediaStoreScanner.scanApkFiles() } returns files

        val savedStateHandle = SavedStateHandle(mapOf("type" to type))
        return CleanupViewModel(
            savedStateHandle = savedStateHandle,
            mediaStoreScanner = mediaStoreScanner,
            cleanupHelper = cleanupHelper,
            storageDataSource = storageDataSource,
            thumbnailLoader = thumbnailLoader
        )
    }

    @Test
    fun `scan produces results with correct grouping`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Results but got $state", state is CleanupUiState.Results)
        val results = state as CleanupUiState.Results

        // Should have 3 groups: VIDEO, IMAGE, DOCUMENT
        assertEquals(3, results.groups.size)

        // Groups sorted by total bytes descending
        // VIDEO group should be first (500M + 200M = 700M)
        val videoGroup = results.groups[0]
        assertEquals(MediaCategory.VIDEO, videoGroup.category)
        assertEquals(2, videoGroup.files.size)
        assertEquals(700_000_000L, videoGroup.totalBytes)

        // IMAGE group second (80M)
        val imageGroup = results.groups[1]
        assertEquals(MediaCategory.IMAGE, imageGroup.category)
        assertEquals(1, imageGroup.files.size)
        assertEquals(80_000_000L, imageGroup.totalBytes)

        // DOCUMENT group third (60M)
        val docGroup = results.groups[2]
        assertEquals(MediaCategory.DOCUMENT, docGroup.category)
        assertEquals(1, docGroup.files.size)
        assertEquals(60_000_000L, docGroup.totalBytes)

        // Total size should be sum of all files
        assertEquals(840_000_000L, results.totalSize)
        assertEquals(4, results.totalCount)

        // Largest group should be expanded by default
        assertTrue("Largest group should be expanded", results.groups[0].expanded)
    }

    @Test
    fun `toggle file selection updates selected set`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Initially no selections (LARGE_FILES does not preselect)
        var state = viewModel.uiState.value as CleanupUiState.Results
        assertTrue("No files should be selected initially", state.selectedUris.isEmpty())
        assertEquals(0L, state.selectedSize)

        // Select first file
        viewModel.toggleSelection("content://media/1")
        advanceUntilIdle()

        state = viewModel.uiState.value as CleanupUiState.Results
        assertTrue(state.selectedUris.contains("content://media/1"))
        assertEquals(1, state.selectedUris.size)
        assertEquals(500_000_000L, state.selectedSize)

        // Select second file
        viewModel.toggleSelection("content://media/3")
        advanceUntilIdle()

        state = viewModel.uiState.value as CleanupUiState.Results
        assertEquals(2, state.selectedUris.size)
        assertEquals(580_000_000L, state.selectedSize)

        // Deselect first file
        viewModel.toggleSelection("content://media/1")
        advanceUntilIdle()

        state = viewModel.uiState.value as CleanupUiState.Results
        assertEquals(1, state.selectedUris.size)
        assertFalse(state.selectedUris.contains("content://media/1"))
        assertTrue(state.selectedUris.contains("content://media/3"))
        assertEquals(80_000_000L, state.selectedSize)
    }

    @Test
    fun `toggle group selection selects and deselects all files in group`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Select entire VIDEO group
        viewModel.toggleGroupSelection(MediaCategory.VIDEO)
        advanceUntilIdle()

        var state = viewModel.uiState.value as CleanupUiState.Results
        assertTrue(state.selectedUris.contains("content://media/1"))
        assertTrue(state.selectedUris.contains("content://media/2"))
        assertEquals(2, state.selectedUris.size)
        assertEquals(700_000_000L, state.selectedSize)

        // Toggle VIDEO group again: should deselect all VIDEO files
        viewModel.toggleGroupSelection(MediaCategory.VIDEO)
        advanceUntilIdle()

        state = viewModel.uiState.value as CleanupUiState.Results
        assertFalse(state.selectedUris.contains("content://media/1"))
        assertFalse(state.selectedUris.contains("content://media/2"))
        assertEquals(0, state.selectedUris.size)
    }

    @Test
    fun `empty scan result produces Empty state`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(files = emptyList())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CleanupUiState.Empty, state)
    }

    @Test
    fun `delete success triggers re-scan`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Select a file
        viewModel.toggleSelection("content://media/1")
        advanceUntilIdle()

        // Simulate delete confirmed (called after system dialog)
        viewModel.onDeleteConfirmed()
        runCurrent()

        // State should be Success (freed bytes shown)
        val state = viewModel.uiState.value
        assertTrue(
            "Expected Success after delete but got $state",
            state is CleanupUiState.Success
        )

        // After delay, re-scan is triggered. Advance past the 1800ms delay.
        advanceTimeBy(2000L)
        advanceUntilIdle()

        // After re-scan, we should be back in Results (since mock returns files)
        val stateAfterRescan = viewModel.uiState.value
        assertTrue(
            "Expected Results after re-scan but got $stateAfterRescan",
            stateAfterRescan is CleanupUiState.Results
        )

        // Verify scanner was called again for the re-scan
        coVerify(atLeast = 2) { mediaStoreScanner.scanLargeFiles(any()) }
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

        coEvery { mediaStoreScanner.scanApkFiles() } returns apkFiles

        val viewModel = createViewModel(type = "APK_FILES", files = apkFiles)
        advanceUntilIdle()

        val state = viewModel.uiState.value as CleanupUiState.Results
        assertEquals(2, state.selectedUris.size)
        assertTrue(state.selectedUris.contains("content://media/10"))
        assertTrue(state.selectedUris.contains("content://media/11"))
        assertEquals(80_000_000L, state.selectedSize)
    }

    @Test
    fun `projected usage percent decreases when files are selected`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val stateNoSelection = viewModel.uiState.value as CleanupUiState.Results
        val initialPct = stateNoSelection.currentUsagePercent

        // Select a large file
        viewModel.toggleSelection("content://media/1") // 500 MB
        advanceUntilIdle()

        val stateWithSelection = viewModel.uiState.value as CleanupUiState.Results
        assertTrue(
            "Projected usage should be less than current usage",
            stateWithSelection.projectedUsagePercent < initialPct
        )
    }
}
