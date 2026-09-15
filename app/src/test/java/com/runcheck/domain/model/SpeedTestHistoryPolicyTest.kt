package com.runcheck.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedTestHistoryPolicyTest {
    @Test
    fun `free history limit is five`() {
        assertEquals(5, SpeedTestHistoryPolicy.resultLimit(isPro = false))
    }

    @Test
    fun `pro history limit is one hundred`() {
        assertEquals(100, SpeedTestHistoryPolicy.resultLimit(isPro = true))
    }
}
