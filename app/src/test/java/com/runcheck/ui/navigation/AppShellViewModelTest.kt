package com.runcheck.ui.navigation

import androidx.lifecycle.viewModelScope
import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.repository.InsightRepository
import com.runcheck.pro.ProState
import com.runcheck.pro.ProStateProvider
import com.runcheck.pro.ProStatus
import com.runcheck.ui.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppShellViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val insightRepository: InsightRepository = mockk(relaxed = true)
    private val proStateProvider: ProStateProvider = mockk()
    private val insights = MutableStateFlow<List<Insight>>(emptyList())
    private val proState = MutableStateFlow(ProState())
    private val proStatusReady = MutableStateFlow(false)

    @Test
    fun `badge counts only unseen insights visible to the current access level`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubFlows()
            insights.value =
                listOf(
                    testInsight(id = 1L, target = InsightTarget.BATTERY, seen = false),
                    testInsight(id = 2L, target = InsightTarget.STORAGE, seen = true),
                    testInsight(id = 3L, target = InsightTarget.APP_USAGE, seen = false),
                )

            val viewModel = AppShellViewModel(insightRepository, proStateProvider)
            advanceTimeBy(334L)
            runCurrent()

            assertEquals(1, viewModel.uiState.value.unseenInsightCount)

            proState.value = ProState(status = ProStatus.PRO_PURCHASED)
            advanceTimeBy(334L)
            runCurrent()

            assertEquals(2, viewModel.uiState.value.unseenInsightCount)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `shell exposes Pro readiness without treating the default free state as ready`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubFlows()

            val viewModel = AppShellViewModel(insightRepository, proStateProvider)
            advanceTimeBy(334L)
            runCurrent()

            assertFalse(viewModel.uiState.value.proStatusReady)
            assertFalse(viewModel.uiState.value.hasProAccess)

            proState.value = ProState(status = ProStatus.PRO_PURCHASED)
            proStatusReady.value = true
            advanceTimeBy(334L)
            runCurrent()

            assertTrue(viewModel.uiState.value.proStatusReady)
            assertTrue(viewModel.uiState.value.hasProAccess)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `insight failure does not block Pro route readiness`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { insightRepository.getActiveInsights() } returns
                flow { throw IllegalStateException("Room unavailable") }
            every { proStateProvider.proState } returns proState
            every { proStateProvider.proStatusReady } returns MutableStateFlow(true)

            val viewModel = AppShellViewModel(insightRepository, proStateProvider)
            advanceTimeBy(334L)
            runCurrent()

            assertTrue(viewModel.uiState.value.proStatusReady)
            assertEquals(0, viewModel.uiState.value.unseenInsightCount)
            viewModel.viewModelScope.cancel()
        }

    private fun stubFlows() {
        every { insightRepository.getActiveInsights() } returns insights
        every { proStateProvider.proState } returns proState
        every { proStateProvider.proStatusReady } returns proStatusReady
    }

    private fun testInsight(
        id: Long,
        target: InsightTarget,
        seen: Boolean,
    ) = Insight(
        id = id,
        ruleId = "rule-$id",
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
