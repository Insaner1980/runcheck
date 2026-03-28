package com.runcheck.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.runcheck.R
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.info.CrossLinkButton
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.ui.theme.spacing

@Composable
fun LearnArticleDetailScreen(
    articleId: String,
    onBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val article = LearnArticleCatalog.findById(articleId)

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = article?.let { stringResource(it.titleRes) } ?: stringResource(R.string.learn_screen_title),
            onBack = onBack,
        )

        ContentContainer {
            if (article == null) {
                Text(
                    text = stringResource(R.string.learn_article_not_found),
                    modifier = Modifier.padding(MaterialTheme.spacing.base),
                )
                return@ContentContainer
            }

            val bodyText = stringResource(article.bodyRes)
            val bodyBlocks = remember(bodyText) { LearnArticleBodyFormatter.parse(bodyText) }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = MaterialTheme.spacing.base,
                        top = MaterialTheme.spacing.sm,
                        end = MaterialTheme.spacing.base,
                        bottom = MaterialTheme.spacing.xl,
                    ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                itemsIndexed(
                    items = bodyBlocks,
                    key = { index, block -> "${block::class.simpleName ?: "learn_block"}_$index" },
                    contentType = { _, block -> block::class.simpleName ?: "learn_block" },
                ) { _, block ->
                    LearnArticleBlockItem(block = block)
                }

                article.crossLinkRoute?.let { route ->
                    item(key = "cross_link", contentType = "learn_cross_link") {
                        CrossLinkButton(
                            label = stringResource(R.string.learn_cross_link_check),
                            onClick = { onNavigateToRoute(route) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnArticleBlockItem(
    block: LearnArticleBlock,
    modifier: Modifier = Modifier,
) {
    when (block) {
        is LearnArticleBlock.Heading -> {
            LearnArticleText(
                text = block.text,
                modifier = modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        is LearnArticleBlock.Paragraph -> {
            LearnArticleText(
                text = block.text,
                modifier = modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        is LearnArticleBlock.BulletList -> {
            LearnArticleList(
                items = block.items,
                markerLabel = { "•" },
                modifier = modifier,
            )
        }

        is LearnArticleBlock.NumberedList -> {
            LearnArticleList(
                items = block.items,
                markerLabel = { index -> "${index + 1}." },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun LearnArticleList(
    items: List<AnnotatedString>,
    markerLabel: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        items.forEachIndexed { index, item ->
            LearnArticleText(
                text =
                    buildAnnotatedString {
                        append(markerLabel(index))
                        append(" ")
                        append(item)
                    },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LearnArticleText(
    text: AnnotatedString,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    ClickableText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        onClick = { offset ->
            text
                .getStringAnnotations(
                    tag = LearnArticleBodyFormatter.URL_ANNOTATION_TAG,
                    start = offset,
                    end = offset,
                ).firstOrNull()
                ?.let { annotation ->
                    uriHandler.openUri(annotation.item)
                }
        },
    )
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun LearnArticleDetailScreenPreview() {
    RuncheckTheme {
        LearnArticleDetailScreen(
            articleId = LearnArticleIds.BATTERY_HEALTH,
            onBack = {},
            onNavigateToRoute = {},
        )
    }
}
