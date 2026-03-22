package com.runcheck.ui.learn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.runcheck.R
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.info.CrossLinkButton
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.ui.theme.spacing

@Composable
fun LearnArticleDetailScreen(
    articleId: String,
    onBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val article = LearnArticleCatalog.findById(articleId)

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = article?.let { stringResource(it.titleRes) } ?: "",
            onBack = onBack
        )

        if (article == null) {
            Text(
                text = stringResource(R.string.learn_article_not_found),
                modifier = Modifier.padding(MaterialTheme.spacing.base)
            )
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.base)
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            val bodyText = stringResource(article.bodyRes)
            parseArticleBody(bodyText)

            article.crossLinkRoute?.let { route ->
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
                CrossLinkButton(
                    label = stringResource(R.string.learn_cross_link_check),
                    onClick = { onNavigateToRoute(route) }
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun parseArticleBody(body: String) {
    val paragraphs = remember(body) { body.split("\n\n") }

    paragraphs.forEach { paragraph ->
        val trimmed = paragraph.trim()
        if (trimmed.isEmpty()) return@forEach

        if (trimmed.startsWith("## ")) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))
            Text(
                text = trimmed.removePrefix("## "),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        } else {
            Text(
                text = trimmed,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun LearnArticleDetailScreenPreview() {
    RuncheckTheme {
        LearnArticleDetailScreen(
            articleId = "battery_health",
            onBack = {},
            onNavigateToRoute = {}
        )
    }
}
