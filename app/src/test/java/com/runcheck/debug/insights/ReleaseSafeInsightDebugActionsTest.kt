package com.runcheck.debug.insights

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReleaseSafeInsightDebugActionsTest {
    private val actions = ReleaseSafeInsightDebugActions()

    @Test
    fun isNotAvailable() {
        assertFalse(actions.isAvailable)
    }

    @Test
    fun noOpMethodsReturnZero() =
        runTest {
            assertEquals(0, actions.seedDemoInsights())
            assertEquals(0, actions.generateInsightsNow())
            assertEquals(0, actions.clearInsights())
        }
}
