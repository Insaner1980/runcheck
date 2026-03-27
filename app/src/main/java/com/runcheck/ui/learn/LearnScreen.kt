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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.runcheck.R
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.ContentContainer
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

        ContentContainer {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.spacing.base),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                item(key = "top_spacing") {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                }

                LearnArticleCatalog.sections.forEachIndexed { index, section ->
                    item(
                        key = "header_${section.topic.name}",
                        contentType = "learn_header"
                    ) {
                        CardSectionTitle(text = stringResource(section.topic.labelRes))
                    }

                    items(
                        items = section.articles,
                        key = { article -> article.id },
                        contentType = { "learn_article" }
                    ) { article ->
                        LearnArticleCard(
                            article = article,
                            onClick = { onNavigateToArticle(article.id) }
                        )
                    }

                    if (index != LearnArticleCatalog.sections.lastIndex) {
                        item(
                            key = "spacer_${section.topic.name}",
                            contentType = "learn_section_spacer"
                        ) {
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
