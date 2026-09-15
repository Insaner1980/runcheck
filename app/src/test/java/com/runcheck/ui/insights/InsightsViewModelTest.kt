package com.runcheck.ui.insights

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.repository.InsightRepository
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.testutil.insightFixture
import com.runcheck.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val insightRepository: InsightRepository = mockk(relaxed = true)
    private val observeProAccess: ObserveProAccessUseCase = mockk()

    private fun stubActiveInsight(insight: Insight) {
        every { insightRepository.getActiveInsights() } returns flowOf(listOf(insight))
        every { observeProAccess() } returns flowOf(false)
    }

    @Test
    fun `loads active insights and marks unseen entries as seen`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val insight = insightFixture(seen = false)
            stubActiveInsight(insight)

            val viewModel =
                InsightsViewModel(
                    insightRepository = insightRepository,
                    observeProAccess = observeProAccess,
                )
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is InsightsUiState.Success)
            assertEquals(1, (state as InsightsUiState.Success).insights.size)
            coVerify(exactly = 1) { insightRepository.markSeen(setOf(1L)) }
        }

    @Test
    fun `filters pro-only insight targets for free users`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { insightRepository.getActiveInsights() } returns
                flowOf(
                    listOf(
                        insightFixture(id = 1L, target = InsightTarget.BATTERY, seen = false),
                        insightFixture(id = 2L, target = InsightTarget.APP_USAGE),
                        insightFixture(id = 3L, target = InsightTarget.CHARGER),
                    ),
                )
            every { observeProAccess() } returns flowOf(false)

            val viewModel =
                InsightsViewModel(
                    insightRepository = insightRepository,
                    observeProAccess = observeProAccess,
                )
            runCurrent()

            val state = viewModel.uiState.value as InsightsUiState.Success
            assertEquals(listOf(InsightTarget.BATTERY), state.insights.map { it.target })
            assertEquals(1, state.unseenInsightCount)
            coVerify(exactly = 1) { insightRepository.markSeen(setOf(1L)) }
        }

    @Test
    fun `visible counts react to seen changes pro access and dismissal`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val battery = insightFixture(id = 1L, target = InsightTarget.BATTERY, seen = false)
            val thermal = insightFixture(id = 2L, target = InsightTarget.THERMAL, seen = false)
            val charger = insightFixture(id = 3L, target = InsightTarget.CHARGER, seen = false)
            val activeInsights = MutableStateFlow(listOf(battery, thermal, charger))
            val proAccess = MutableStateFlow(false)
            every { insightRepository.getActiveInsights() } returns activeInsights
            every { observeProAccess() } returns proAccess
            coEvery { insightRepository.dismiss(2L) } answers {
                activeInsights.value = activeInsights.value.filterNot { it.id == 2L }
            }

            val viewModel = InsightsViewModel(insightRepository, observeProAccess)
            runCurrent()

            val initial = viewModel.uiState.value as InsightsUiState.Success
            assertEquals(listOf(battery, thermal), initial.insights)
            assertEquals(2, initial.unseenInsightCount)

            activeInsights.value = listOf(battery.copy(seen = true), thermal, charger)
            runCurrent()

            val seen = viewModel.uiState.value as InsightsUiState.Success
            assertEquals(listOf(battery.copy(seen = true), thermal), seen.insights)
            assertEquals(1, seen.unseenInsightCount)

            proAccess.value = true
            runCurrent()

            val pro = viewModel.uiState.value as InsightsUiState.Success
            assertEquals(activeInsights.value, pro.insights)
            assertEquals(2, pro.unseenInsightCount)
            assertTrue(pro.isPro)

            viewModel.dismissInsight(2L)
            runCurrent()

            val dismissed = viewModel.uiState.value as InsightsUiState.Success
            assertEquals(listOf(battery.copy(seen = true), charger), dismissed.insights)
            assertEquals(1, dismissed.unseenInsightCount)
            coVerify(exactly = 1) { insightRepository.dismiss(2L) }
        }

    @Test
    fun `active insight flow failure produces error state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { insightRepository.getActiveInsights() } returns flow { error("insights failed") }
            every { observeProAccess() } returns flowOf(false)

            val viewModel = InsightsViewModel(insightRepository, observeProAccess)
            runCurrent()

            assertTrue(viewModel.uiState.value is InsightsUiState.Error)
        }

    @Test
    fun `dismiss delegates to repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { insightRepository.getActiveInsights() } returns flowOf(emptyList())
            every { observeProAccess() } returns flowOf(false)

            val viewModel =
                InsightsViewModel(
                    insightRepository = insightRepository,
                    observeProAccess = observeProAccess,
                )
            runCurrent()

            viewModel.dismissInsight(42L)
            runCurrent()

            coVerify(exactly = 1) { insightRepository.dismiss(42L) }
        }

    @Test
    fun `mark seen database failure keeps loaded insights visible`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val insight = insightFixture(seen = false)
            stubActiveInsight(insight)
            coEvery { insightRepository.markSeen(setOf(1L)) } throws IllegalStateException("database failed")

            val viewModel =
                InsightsViewModel(
                    insightRepository = insightRepository,
                    observeProAccess = observeProAccess,
                )
            runCurrent()

            val state = viewModel.uiState.value as InsightsUiState.Success
            assertEquals(listOf(insight), state.insights)
            coVerify(exactly = 1) { insightRepository.markSeen(setOf(1L)) }
        }
}
