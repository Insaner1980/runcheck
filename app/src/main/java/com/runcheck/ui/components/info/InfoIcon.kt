package com.runcheck.ui.components.info

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.theme.uiTokens

@Composable
fun InfoIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.uiTokens
    IconButton(
        onClick = onClick,
        modifier = modifier.size(tokens.touchTarget),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = stringResource(R.string.a11y_info_button),
            modifier = Modifier.size(tokens.iconSmall),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
