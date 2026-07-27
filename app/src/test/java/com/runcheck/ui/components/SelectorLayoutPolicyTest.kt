package com.runcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectorLayoutPolicyTest {
    @Test
    fun `small selector shares the available row width`() {
        val policy = selectorLayoutPolicy(optionCount = 4)

        assertFalse(policy.isScrollable)
        assertEquals(null, policy.minimumOptionWidthDp)
        assertTrue(policy.minimumTouchTargetDp >= 48)
    }

    @Test
    fun `seven and eight option selectors scroll without shrinking labels or touch targets`() {
        listOf(7, 8).forEach { count ->
            val policy = selectorLayoutPolicy(optionCount = count)

            assertTrue("$count options must scroll", policy.isScrollable)
            assertTrue(
                "$count options need readable option width",
                requireNotNull(policy.minimumOptionWidthDp) >= 88,
            )
            assertTrue(policy.minimumTouchTargetDp >= 48)
        }
    }
}
