package com.runcheck.ui.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnTopicFilterTest {
    @Test
    fun `topic choices match the five expressive filters`() {
        val expectedOrder =
            listOf(
                LearnTopic.BATTERY,
                LearnTopic.NETWORK,
                LearnTopic.THERMAL,
                LearnTopic.STORAGE,
                LearnTopic.PRIVACY,
            )

        assertEquals(expectedOrder, LearnTopic.entries)
    }

    @Test
    fun `selected topic returns only that catalog section`() {
        val result = filterLearnSections(LearnArticleCatalog.sections, LearnTopic.NETWORK)

        assertEquals(1, result.size)
        assertEquals(LearnTopic.NETWORK, result.single().topic)
        assertTrue(result.single().articles.all { it.topic == LearnTopic.NETWORK })
    }

    @Test
    fun `no selected topic keeps the complete catalog`() {
        assertEquals(
            LearnArticleCatalog.sections,
            filterLearnSections(LearnArticleCatalog.sections, selectedTopic = null),
        )
    }
}
