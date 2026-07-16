package com.runcheck.domain.insights.engine

import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.insights.rules.RecurringThermalThrottlingRule
import com.runcheck.domain.insights.rules.ThermalPatternDetectionRule
import com.runcheck.domain.repository.InsightRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightEngineTest {
    private val insightRepository: InsightRepository = mockk(relaxed = true)

    @Test
    fun `cancellation during rule evaluation does not replace generation results`() =
        runTest {
            val completedRule = mockRule("completed")
            val cancelledRule = mockRule("cancelled")
            coEvery { completedRule.evaluate(NOW) } returns emptyList()
            coEvery { cancelledRule.evaluate(NOW) } throws CancellationException("stopped")
            val engine = InsightEngine(linkedSetOf(completedRule, cancelledRule), insightRepository)

            val thrown = runCatching { engine.generateInsights(NOW) }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
            coVerify(exactly = 0) { insightRepository.replaceGenerationResults(any(), any()) }
        }

    @Test
    fun `recurring throttling suppresses overlapping thermal pattern`() =
        runTest {
            val thermalPatternRule = mockRule(ThermalPatternDetectionRule.RULE_ID)
            val recurringThrottlingRule = mockRule(RecurringThermalThrottlingRule.RULE_ID)
            coEvery { thermalPatternRule.evaluate(NOW) } returns
                listOf(candidate(ThermalPatternDetectionRule.RULE_ID))
            coEvery { recurringThrottlingRule.evaluate(NOW) } returns
                listOf(candidate(RecurringThermalThrottlingRule.RULE_ID))
            val engine = InsightEngine(linkedSetOf(thermalPatternRule, recurringThrottlingRule), insightRepository)

            engine.generateInsights(NOW)

            coVerify {
                insightRepository.replaceGenerationResults(
                    match { candidates ->
                        candidates[ThermalPatternDetectionRule.RULE_ID].isNullOrEmpty() &&
                            candidates[RecurringThermalThrottlingRule.RULE_ID]?.size == 1
                    },
                    NOW,
                )
            }
        }

    private fun mockRule(ruleId: String): InsightRule =
        mockk {
            every { this@mockk.ruleId } returns ruleId
        }

    private fun candidate(ruleId: String) =
        InsightCandidate(
            ruleId = ruleId,
            dedupeKey = "same-episode",
            type = InsightType.THERMAL,
            priority = InsightPriority.HIGH,
            confidence = 0.9f,
            titleKey = "title",
            bodyKey = "body",
            bodyArgs = emptyList(),
            generatedAt = NOW,
            expiresAt = NOW + 1_000L,
            dataWindowStart = NOW - 1_000L,
            dataWindowEnd = NOW,
            target = InsightTarget.THERMAL,
        )

    companion object {
        private const val NOW = 1_000L
    }
}
