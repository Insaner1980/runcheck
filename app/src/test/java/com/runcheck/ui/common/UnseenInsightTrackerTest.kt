package com.runcheck.ui.common

import com.runcheck.testutil.insightFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnseenInsightTrackerTest {
    @Test
    fun `failed write remains eligible for retry`() {
        val tracker = UnseenInsightTracker()
        val insights = listOf(insightFixture(id = 7L, seen = false))

        val firstAttempt = tracker.idsToMarkSeen(insights)
        assertEquals(setOf(7L), firstAttempt)
        assertNull(tracker.idsToMarkSeen(insights))

        tracker.complete(setOf(7L), succeeded = false)

        assertEquals(setOf(7L), tracker.idsToMarkSeen(insights))
    }

    @Test
    fun `successful write suppresses an unchanged set`() {
        val tracker = UnseenInsightTracker()
        val insights = listOf(insightFixture(id = 7L, seen = false))

        tracker.idsToMarkSeen(insights)
        tracker.complete(setOf(7L), succeeded = true)

        assertNull(tracker.idsToMarkSeen(insights))
    }
}
