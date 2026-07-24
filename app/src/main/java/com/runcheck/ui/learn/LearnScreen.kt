package com.runcheck.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.ExpressiveDetailScaffold
import com.runcheck.ui.components.ExpressiveSingleChoiceSelector
import com.runcheck.ui.theme.RuncheckPreviews
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.ui.theme.spacing

@Composable
fun LearnScreen(
    onBack: () -> Unit,
    onNavigateToArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialTopic: LearnTopic? = null,
) {
    var selectedTopic by rememberSaveable(initialTopic) {
        mutableStateOf(initialTopic ?: LearnTopic.BATTERY)
    }
    val visibleSections = filterLearnSections(LearnArticleCatalog.sections, selectedTopic)

    ExpressiveDetailScaffold(
        title = stringResource(R.string.learn_screen_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        ExpressiveSingleChoiceSelector(
            options = LearnTopic.entries,
            selected = selectedTopic,
            labelFor = { topic -> stringResource(topic.labelRes) },
            onSelect = { selectedTopic = it },
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            item(key = "top_spacing") {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            }

            visibleSections.forEachIndexed { index, section ->
                item(
                    key = "header_${section.topic.name}",
                    contentType = "learn_header",
                ) {
                    CardSectionTitle(text = stringResource(section.topic.labelRes))
                }

                items(
                    items = section.articles,
                    key = { article -> article.id },
                    contentType = { "learn_article" },
                ) { article ->
                    LearnArticleCard(
                        article = article,
                        onClick = { onNavigateToArticle(article.id) },
                    )
                }

                if (index != visibleSections.lastIndex) {
                    item(
                        key = "spacer_${section.topic.name}",
                        contentType = "learn_section_spacer",
                    ) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    }
                }
            }

            item(
                key = "general_spacer",
                contentType = "learn_section_spacer",
            ) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            }

            item(
                key = "header_general",
                contentType = "learn_header",
            ) {
                CardSectionTitle(text = stringResource(R.string.learn_topic_general))
            }

            items(
                items = LearnArticleCatalog.generalArticles,
                key = { article -> article.id },
                contentType = { "learn_article" },
            ) { article ->
                LearnArticleCard(
                    article = article,
                    onClick = { onNavigateToArticle(article.id) },
                )
            }

            item(key = "bottom_spacing") {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
            }
        }
    }
}

@RuncheckPreviews
@Composable
private fun LearnScreenPreview() {
    RuncheckTheme {
        LearnScreen(
            onBack = {},
            onNavigateToArticle = {},
        )
    }
}
