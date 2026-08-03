package com.runcheck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing

/** Standard full-width card container for vertically arranged runcheck content. */
@Composable
fun RuncheckCard(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    colors: CardColors = runcheckCardColors(),
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.spacing.base),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) = RuncheckCardSurface(modifier = modifier, colors = colors) {
    Column(
        modifier =
            contentModifier
                .fillMaxWidth()
                .padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** Standard runcheck card surface for non-column content. */
@Composable
fun RuncheckCardSurface(
    modifier: Modifier = Modifier,
    colors: CardColors = runcheckCardColors(),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = colors,
        elevation = runcheckCardElevation(),
    ) {
        content()
    }
}

@Composable
fun RuncheckClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.spacing.base),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}
