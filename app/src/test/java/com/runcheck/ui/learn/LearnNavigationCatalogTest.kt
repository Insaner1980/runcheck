package com.runcheck.ui.learn

import com.runcheck.ui.components.info.InfoCardCatalog
import com.runcheck.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnNavigationCatalogTest {
    @Test
    fun catalog_containsEveryDeclaredArticleIdExactlyOnce() {
        assertEquals(17, LearnArticleCatalog.articles.size)
        assertEquals(LearnArticleIds.all, LearnArticleCatalog.articles.map { it.id }.toSet())
    }

    @Test
    fun articleCrossLinks_onlyUseDirectlyReachableRoutes() {
        val invalidLinks =
            LearnArticleCatalog.articles.mapNotNull { article ->
                val route = article.crossLinkRoute ?: return@mapNotNull null
                if (Screen.isValidLearnCrossLinkRoute(route)) {
                    null
                } else {
                    "${article.id} -> $route"
                }
            }

        assertTrue(
            "Found learn articles with invalid cross-link routes: $invalidLinks",
            invalidLinks.isEmpty(),
        )
    }

    @Test
    fun infoCardLearnLinks_resolveToExistingArticles() {
        val missingArticles =
            InfoCardCatalog.all.mapNotNull { card ->
                val articleId = card.learnArticleId ?: return@mapNotNull null
                if (LearnArticleCatalog.containsId(articleId)) {
                    null
                } else {
                    "${card.key} -> $articleId"
                }
            }

        assertTrue(
            "Found info cards pointing to missing learn articles: $missingArticles",
            missingArticles.isEmpty(),
        )
    }

    @Test
    fun detailTopics_haveContextualLearnArticles() {
        val emptyTopics =
            listOf(
                LearnTopic.BATTERY,
                LearnTopic.NETWORK,
                LearnTopic.THERMAL,
                LearnTopic.STORAGE,
                LearnTopic.PRIVACY,
            ).filter { topic ->
                LearnArticleCatalog.articlesForTopic(topic).isEmpty()
            }

        assertTrue(
            "Expected contextual learn content for all detail topics, but found none for: $emptyTopics",
            emptyTopics.isEmpty(),
        )
    }

    @Test
    fun learnSurfaces_exposeEveryCatalogArticleExactlyOnce() {
        val topicArticles = LearnArticleCatalog.sections.flatMap(LearnTopicSection::articles)
        val reachableArticles = topicArticles + LearnArticleCatalog.generalArticles

        assertEquals(5, LearnArticleCatalog.sections.size)
        assertEquals(LearnArticleCatalog.articles.size, reachableArticles.size)
        assertEquals(
            LearnArticleCatalog.articles.map(LearnArticle::id).toSet(),
            reachableArticles.map(LearnArticle::id).toSet(),
        )
        assertTrue(LearnArticleCatalog.generalArticles.all { article -> article.topic == null })
        assertEquals(
            listOf(
                LearnArticleIds.HEALTH_SCORE,
                LearnArticleIds.SOFTWARE_VS_HARDWARE,
                LearnArticleIds.BACKGROUND_MONITORING,
            ),
            LearnArticleCatalog.generalArticles.map(LearnArticle::id),
        )
    }
}
