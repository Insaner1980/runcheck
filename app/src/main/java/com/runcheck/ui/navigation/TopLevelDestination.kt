package com.runcheck.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.runcheck.R

@Immutable
sealed class TopLevelDestination(
    val screen: Screen,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    data object Home :
        TopLevelDestination(
            screen = Screen.Home,
            labelRes = R.string.navigation_home,
            icon = Icons.Outlined.Home,
        )

    data object Insights :
        TopLevelDestination(
            screen = Screen.Insights,
            labelRes = R.string.navigation_insights,
            icon = Icons.Outlined.Lightbulb,
        )

    data object Tools :
        TopLevelDestination(
            screen = Screen.Tools,
            labelRes = R.string.navigation_tools,
            icon = Icons.Outlined.Build,
        )

    data object Settings :
        TopLevelDestination(
            screen = Screen.Settings,
            labelRes = R.string.navigation_settings,
            icon = Icons.Outlined.Settings,
        )
}

val topLevelDestinations: List<TopLevelDestination> =
    listOf(
        TopLevelDestination.Home,
        TopLevelDestination.Insights,
        TopLevelDestination.Tools,
        TopLevelDestination.Settings,
    )
