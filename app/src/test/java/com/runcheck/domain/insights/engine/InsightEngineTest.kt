package com.runcheck.domain.insights.engine

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

    private fun mockRule(ruleId: String): InsightRule =
        mockk {
            every { this@mockk.ruleId } returns ruleId
        }

    companion object {
        private const val NOW = 1_000L
    }
}
