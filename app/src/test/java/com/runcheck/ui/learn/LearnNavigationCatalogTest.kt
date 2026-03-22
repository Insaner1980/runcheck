package com.runcheck.ui.learn

import com.runcheck.ui.components.info.InfoCardCatalog
import com.runcheck.ui.navigation.Screen
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnNavigationCatalogTest {

    @Test
    fun articleCrossLinks_onlyUseDirectlyReachableRoutes() {
        val invalidLinks = LearnArticleCatalog.articles.mapNotNull { article ->
            val route = article.crossLinkRoute ?: return@mapNotNull null
            if (Screen.isValidLearnCrossLinkRoute(route)) {
                null
            } else {
                "${article.id} -> $route"
            }
        }

        assertTrue(
            "Found learn articles with invalid cross-link routes: $invalidLinks",
            invalidLinks.isEmpty()
        )
    }

    @Test
    fun infoCardLearnLinks_resolveToExistingArticles() {
        val missingArticles = InfoCardCatalog.all.mapNotNull { card ->
            val articleId = card.learnArticleId ?: return@mapNotNull null
            if (LearnArticleCatalog.containsId(articleId)) {
                null
            } else {
                "${card.key} -> $articleId"
            }
        }

        assertTrue(
            "Found info cards pointing to missing learn articles: $missingArticles",
            missingArticles.isEmpty()
        )
    }

    @Test
    fun detailTopics_haveContextualLearnArticles() {
        val emptyTopics = listOf(
            LearnTopic.BATTERY,
            LearnTopic.NETWORK,
            LearnTopic.TEMPERATURE,
            LearnTopic.STORAGE
        ).filter { topic ->
            LearnArticleCatalog.articlesForTopic(topic).isEmpty()
        }

        assertTrue(
            "Expected contextual learn content for all detail topics, but found none for: $emptyTopics",
            emptyTopics.isEmpty()
        )
    }
}
