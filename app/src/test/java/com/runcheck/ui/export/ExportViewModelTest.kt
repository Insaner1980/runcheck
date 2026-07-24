package com.runcheck.ui.export

import com.runcheck.domain.usecase.ExportDataUseCase
import com.runcheck.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exportDataUseCase = mockk<ExportDataUseCase>()

    @Test
    fun `prepare export share exposes uris and clears loading state`() =
        runTest {
            coEvery { exportDataUseCase.prepareExportShare() } returns
                listOf("content://runcheck/export.csv")
            val viewModel = ExportViewModel(exportDataUseCase)

            viewModel.prepareExportShare()
            runCurrent()

            coVerify(exactly = 1) { exportDataUseCase.prepareExportShare() }
            assertEquals(
                listOf("content://runcheck/export.csv"),
                viewModel.uiState.value.shareUris,
            )
            assertFalse(viewModel.uiState.value.isExporting)
        }

    @Test
    fun `double invocation while export is active prepares and exposes one share result`() =
        runTest {
            val exportStarted = CompletableDeferred<Unit>()
            val completeExport = CompletableDeferred<Unit>()
            coEvery { exportDataUseCase.prepareExportShare() } coAnswers {
                exportStarted.complete(Unit)
                completeExport.await()
                listOf("content://runcheck/export.csv")
            }
            val viewModel = ExportViewModel(exportDataUseCase)

            viewModel.prepareExportShare()
            val waitingForStart = async { exportStarted.await() }
            runCurrent()
            waitingForStart.await()

            viewModel.prepareExportShare()
            completeExport.complete(Unit)
            runCurrent()

            coVerify(exactly = 1) { exportDataUseCase.prepareExportShare() }
            assertEquals(
                listOf("content://runcheck/export.csv"),
                viewModel.uiState.value.shareUris,
            )
        }
}
