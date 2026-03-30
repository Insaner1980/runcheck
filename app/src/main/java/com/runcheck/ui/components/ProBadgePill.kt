package com.runcheck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.uiTokens

@Composable
fun ProBadgePill(
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val badgeText = text ?: stringResource(R.string.pro_feature_badge)
    val accentColor = MaterialTheme.colorScheme.primary
    val tokens = MaterialTheme.uiTokens
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = accentColor.copy(alpha = tokens.proBadgeBackgroundAlpha),
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = tokens.proBadgeHorizontalPadding,
                    vertical = tokens.proBadgeVerticalPadding,
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(tokens.iconTiny),
                tint = accentColor,
            )
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
            )
        }
    }
}
