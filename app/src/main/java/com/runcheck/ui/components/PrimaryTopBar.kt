package com.runcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.theme.spacing

@Composable
fun PrimaryTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = spacing.xs, end = spacing.sm, top = spacing.xxs, bottom = spacing.xs)
                .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onMenuClick != null) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = stringResource(R.string.navigate_menu),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.width(spacing.xs))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        actions()
    }
}
