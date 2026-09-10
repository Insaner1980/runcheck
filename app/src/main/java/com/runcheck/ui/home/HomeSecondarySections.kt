package com.runcheck.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.ui.components.ProBadgePill
import com.runcheck.ui.theme.HomeCream
import com.runcheck.ui.theme.HomeGraphite
import com.runcheck.ui.theme.HomeInk
import com.runcheck.ui.theme.HomeStone
import com.runcheck.ui.theme.uiTokens

@Composable
internal fun HomeQuickToolsSection(
    isPro: Boolean,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
    onNavigateToLearn: () -> Unit,
    compact: Boolean,
) {
    val tokens = MaterialTheme.uiTokens
    BoxWithConstraints {
        val singleColumn = LocalDensity.current.fontScale >= 1.5f || maxWidth / LocalDensity.current.fontScale < 320.dp
        val appUsage: @Composable () -> Unit = {
            HomeToolTile(
                title = stringResource(R.string.home_app_usage_card),
                description = stringResource(R.string.home_app_usage_description),
                icon = Icons.Outlined.GridView,
                background = HomeCream,
                foreground = HomeInk,
                onClick = if (isPro) onNavigateToAppUsage else onNavigateToProUpgrade,
                locked = !isPro,
                compact = compact,
            )
        }
        val learn: @Composable () -> Unit = {
            HomeToolTile(
                title = stringResource(R.string.home_learn),
                description = stringResource(R.string.home_learn_description),
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                background = HomeGraphite,
                foreground = HomeCream,
                onClick = onNavigateToLearn,
                compact = compact,
                modifier =
                    if (singleColumn) {
                        Modifier
                    } else {
                        Modifier
                            .wrapContentWidth(Alignment.Start, unbounded = true)
                            .requiredWidth((maxWidth - tokens.homeStatusTileGap) / 2 + 20.dp)
                    },
                shape =
                    if (singleColumn) {
                        RoundedCornerShape(
                            tokens.homeStatusTileCornerRadius,
                        )
                    } else {
                        HomeTileShape(HomeTileEdge.LEARN)
                    },
            )
        }
        val speed: @Composable (Modifier) -> Unit = { modifier ->
            HomeToolTile(
                title = stringResource(R.string.home_speed_test),
                description = stringResource(R.string.home_speed_test_description),
                icon = Icons.Outlined.Speed,
                background = HomeStone,
                foreground = HomeInk,
                onClick = onNavigateToSpeedTest,
                compact = compact,
                shape =
                    if (singleColumn) {
                        RoundedCornerShape(
                            tokens.homeStatusTileCornerRadius,
                        )
                    } else {
                        HomeTileShape(HomeTileEdge.SPEED)
                    },
                modifier = modifier,
            )
        }
        if (singleColumn) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.homeStatusTileGap)) {
                appUsage()
                learn()
                speed(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(tokens.homeStatusTileGap),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(tokens.homeStatusTileGap),
                ) {
                    appUsage()
                    learn()
                }
                speed(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun HomeToolTile(
    title: String,
    description: String,
    icon: ImageVector,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MaterialTheme.uiTokens.homeStatusTileCornerRadius),
    locked: Boolean = false,
    compact: Boolean = false,
) {
    val tokens = MaterialTheme.uiTokens
    Surface(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(
                    min =
                        if (compact) {
                            tokens.homeCompactToolTileHeight
                        } else {
                            tokens.homeToolTileHeight
                        },
                ),
        shape = shape,
        color = background,
        contentColor = foreground,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = 16.dp,
                    vertical =
                        if (compact) {
                            tokens.homeCompactToolTileVerticalPadding
                        } else {
                            16.dp
                        },
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(MaterialTheme.uiTokens.iconXLarge))
                if (locked) ProBadgePill()
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!compact) {
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
