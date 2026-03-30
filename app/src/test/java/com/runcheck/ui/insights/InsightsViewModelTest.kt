package com.runcheck.ui.insights

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.repository.InsightRepository
import com.runcheck.pro.ProManager
import com.runcheck.pro.ProState
import com.runcheck.ui.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val proManager: ProManager = mockk()
    private val proStateFlow = MutableStateFlow(ProState())

    @Test
    fun `loads active insights and marks unseen entries as seen`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val insight = testInsight(seen = false)
            every { insightRepository.getActiveInsights() } returns flowOf(listOf(insight))
            every { insightRepository.getUnseenCount() } returns flowOf(1)
            every { proManager.proState } returns proStateFlow

            val viewModel =
                InsightsViewModel(
                    insightRepository = insightRepository,
                    proManager = proManager,
                )
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is InsightsUiState.Success)
            assertEquals(1, (state as InsightsUiState.Success).insights.size)
            coVerify(exactly = 1) { insightRepository.markAllSeen() }
        }

    @Test
    fun `dismiss delegates to repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { insightRepository.getActiveInsights() } returns flowOf(emptyList())
            every { insightRepository.getUnseenCount() } returns flowOf(0)
            every { proManager.proState } returns proStateFlow

            val viewModel =
                InsightsViewModel(
                    insightRepository = insightRepository,
                    proManager = proManager,
                )
            runCurrent()

            viewModel.dismissInsight(42L)
            runCurrent()

            coVerify(exactly = 1) { insightRepository.dismiss(42L) }
        }

    private fun testInsight(seen: Boolean) =
        Insight(
            id = 1L,
            ruleId = "rule",
            type = InsightType.BATTERY,
            priority = InsightPriority.HIGH,
            confidence = 0.9f,
            titleKey = "title",
            bodyKey = "body",
            bodyArgs = emptyList(),
            generatedAt = 0L,
            expiresAt = Long.MAX_VALUE,
            target = InsightTarget.BATTERY,
            seen = seen,
            dismissed = false,
        )
}
