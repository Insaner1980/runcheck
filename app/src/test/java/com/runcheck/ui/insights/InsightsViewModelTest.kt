package com.runcheck.ui.insights

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.repository.InsightRepository
import com.runcheck.domain.usecase.ObserveProAccessUseCase
import com.runcheck.ui.MainDispatcherRule
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
class InsightsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val insightRepository: InsightRepository = mockk(relaxed = true)
    private val observeProAccess: ObserveProAccessUseCase = mockk()

    @Test
    fun `loads active insights and marks unseen entries as seen`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val insight = testInsight(seen = false)
            every { insightRepository.getActiveInsights() } returns flowOf(listOf(insight))
            every { insightRepository.getUnseenCount() } returns flowOf(1)
            every { observeProAccess() } returns flowOf(false)

            val viewModel =
                InsightsViewModel(
                    insightRepository = insightRepository,
                    observeProAccess = observeProAccess,
                )
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is InsightsUiState.Success)
            assertEquals(1, (state as InsightsUiState.Success).insights.size)
            coVerify(exactly = 1) { insightRepository.markAllSeen() }
        }

    @Test
    fun `filters pro-only insight targets for free users`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { insightRepository.getActiveInsights() } returns
                flowOf(
                    listOf(
                        testInsight(id = 1L, target = InsightTarget.BATTERY, seen = false),
                        testInsight(id = 2L, target = InsightTarget.APP_USAGE),
                        testInsight(id = 3L, target = InsightTarget.CHARGER),
                    ),
                )
            every { insightRepository.getUnseenCount() } returns flowOf(3)
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
        }

    @Test
    fun `dismiss delegates to repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { insightRepository.getActiveInsights() } returns flowOf(emptyList())
            every { insightRepository.getUnseenCount() } returns flowOf(0)
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

    private fun testInsight(
        id: Long = 1L,
        target: InsightTarget = InsightTarget.BATTERY,
        seen: Boolean = true,
    ) = Insight(
        id = id,
        ruleId = "rule",
        type = InsightType.BATTERY,
        priority = InsightPriority.HIGH,
        confidence = 0.9f,
        titleKey = "title",
        bodyKey = "body",
        bodyArgs = emptyList(),
        generatedAt = 0L,
        expiresAt = Long.MAX_VALUE,
        target = target,
        seen = seen,
        dismissed = false,
    )
}
