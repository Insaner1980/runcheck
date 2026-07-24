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

    @Test
    fun `privacy topic contains only genuine privacy content`() {
        val privacyArticles = LearnArticleCatalog.articlesForTopic(LearnTopic.PRIVACY)

        assertEquals(listOf(LearnArticleIds.PRIVACY_DATA), privacyArticles.map(LearnArticle::id))
        assertTrue(privacyArticles.all { it.topic == LearnTopic.PRIVACY })
        assertTrue(
            listOf(
                LearnArticleIds.HEALTH_SCORE,
                LearnArticleIds.SOFTWARE_VS_HARDWARE,
                LearnArticleIds.BACKGROUND_MONITORING,
            ).all { id -> LearnArticleCatalog.findById(id)?.topic == null },
        )
    }

    @Test
    fun `catalog sections follow selector order and include every categorized article once`() {
        assertEquals(LearnTopic.entries, LearnArticleCatalog.sections.map(LearnTopicSection::topic))
        val categorizedArticles = LearnArticleCatalog.articles.filter { it.topic != null }
        val sectionArticles = LearnArticleCatalog.sections.flatMap(LearnTopicSection::articles)
        assertEquals(categorizedArticles.size, sectionArticles.size)
        assertEquals(categorizedArticles.toSet(), sectionArticles.toSet())
    }
}
