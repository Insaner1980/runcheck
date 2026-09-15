package com.runcheck.ui.components.info

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/** Binds catalog content and shared dismissal settings without deciding contextual relevance. */
@Composable
fun CatalogInfoCard(
    definition: DismissibleInfoCardDefinition,
    dismissedInfoCards: Set<String>,
    showInfoCards: Boolean,
    onDismiss: (String) -> Unit,
    onNavigateToLearnArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoCard(
        id = definition.id,
        headline = stringResource(definition.headlineRes),
        body = stringResource(definition.bodyRes),
        onDismiss = onDismiss,
        modifier = modifier,
        visible = definition.id !in dismissedInfoCards && showInfoCards,
        onLearnMore =
            definition.learnArticleId?.let {
                { InfoCardCatalog.resolveLearnArticleId(definition)?.let(onNavigateToLearnArticle) }
            },
    )
}
