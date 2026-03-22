package com.runcheck.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.runcheck.R
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.ui.theme.spacing

@Composable
fun LearnScreen(
    onBack: () -> Unit,
    onNavigateToArticle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = stringResource(R.string.learn_screen_title),
            onBack = onBack
        )

        val groupedArticles = remember {
            LearnTopic.entries.flatMap { topic ->
                buildList {
                    add(LearnArticleListItem.Header(topic))
                    LearnArticleCatalog.articles
                        .asSequence()
                        .filter { it.topic == topic }
                        .forEach { add(LearnArticleListItem.Article(it)) }
                    add(LearnArticleListItem.Spacer(topic))
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            item(key = "top_spacing") {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            }

            items(
                items = groupedArticles,
                key = { item -> item.key },
                contentType = { item -> item::class.simpleName ?: "learn_item" }
            ) { item ->
                when (item) {
                    is LearnArticleListItem.Header -> {
                        CardSectionTitle(text = stringResource(item.topic.labelRes))
                    }
                    is LearnArticleListItem.Article -> {
                        val article = item.article
                        LearnArticleCard(
                            article = article,
                            onClick = { onNavigateToArticle(article.id) }
                        )
                    }
                    is LearnArticleListItem.Spacer -> {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    }
                }
            }

            item(key = "bottom_spacing") {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
            }
        }
    }
}

private sealed interface LearnArticleListItem {
    val key: String

    data class Header(val topic: LearnTopic) : LearnArticleListItem {
        override val key: String = "header_${topic.name}"
    }

    data class Article(val article: LearnArticle) : LearnArticleListItem {
        override val key: String = article.id
    }

    data class Spacer(val topic: LearnTopic) : LearnArticleListItem {
        override val key: String = "spacer_${topic.name}"
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun LearnScreenPreview() {
    RuncheckTheme {
        LearnScreen(
            onBack = {},
            onNavigateToArticle = {}
        )
    }
}
