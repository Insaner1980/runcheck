package com.runcheck.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.info.CrossLinkButton
import com.runcheck.ui.theme.spacing

@Composable
fun RelatedArticlesSection(
    articleIds: List<String>,
    onNavigateToArticle: (articleId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val articles = LearnArticleCatalog.findAllByIds(articleIds)

    if (articles.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        SectionHeader(text = stringResource(R.string.related_articles_header))

        articles.forEach { article ->
            CrossLinkButton(
                label = stringResource(article.titleRes),
                onClick = { onNavigateToArticle(article.id) },
            )
        }
    }
}
