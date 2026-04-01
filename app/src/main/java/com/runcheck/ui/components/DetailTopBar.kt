package com.runcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.runcheck.R
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.uiTokens

@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val tokens = MaterialTheme.uiTokens
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = spacing.xs, end = spacing.xs, top = spacing.xxs, bottom = spacing.xs)
                .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back),
            )
        }
        Text(
            text = title,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        // Spacer for symmetry with the back button
        Spacer(
            modifier =
                Modifier
                    .width(tokens.touchTarget)
                    .clearAndSetSemantics {},
        )
    }
}
