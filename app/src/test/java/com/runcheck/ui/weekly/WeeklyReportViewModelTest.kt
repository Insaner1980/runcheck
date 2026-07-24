package com.runcheck.ui.weekly

import com.runcheck.domain.model.WeeklyReport
import com.runcheck.domain.usecase.GenerateWeeklyReportUseCase
import com.runcheck.domain.usecase.WeeklyReportGenerationResult
import com.runcheck.service.report.WeeklyReportTimeProvider
import com.runcheck.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WeeklyReportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `load publishes generated previous completed week`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val report = mockk<WeeklyReport>()
            val generate = mockk<GenerateWeeklyReportUseCase>()
            coEvery { generate(any()) } returns WeeklyReportGenerationResult.Available(report)
            val timeProvider =
                WeeklyReportTimeProvider().apply {
                    overrideClock = Clock.fixed(Instant.parse("2026-02-16T08:00:00Z"), ZoneId.of("UTC"))
                    overrideZoneProvider = { ZoneId.of("UTC") }
                }
            val viewModel = WeeklyReportViewModel(generate, timeProvider)

            viewModel.load()
            runCurrent()

            assertEquals(WeeklyReportUiState.Success(report), viewModel.uiState.value)
        }

    @Test
    fun `domain gate publishes locked without a report`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val generate = mockk<GenerateWeeklyReportUseCase>()
            coEvery { generate(any()) } returns WeeklyReportGenerationResult.Locked
            val viewModel = WeeklyReportViewModel(generate, WeeklyReportTimeProvider())

            viewModel.load()
            runCurrent()

            assertEquals(WeeklyReportUiState.Locked, viewModel.uiState.value)
        }
}
