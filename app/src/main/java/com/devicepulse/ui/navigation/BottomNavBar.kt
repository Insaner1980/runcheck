package com.devicepulse.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.devicepulse.R

data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard.route, R.string.nav_dashboard, Icons.Filled.Dashboard),
    BottomNavItem(Screen.Battery.route, R.string.nav_battery, Icons.Filled.Battery4Bar),
    BottomNavItem(Screen.Network.route, R.string.nav_network, Icons.Filled.SignalCellularAlt),
    BottomNavItem("more", R.string.nav_more, Icons.Filled.MoreHoriz)
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        bottomNavItems.forEach { item ->
            val selected = when {
                item.route == "more" -> currentRoute in listOf(
                    Screen.Thermal.route,
                    Screen.Storage.route,
                    Screen.Settings.route
                )
                else -> currentRoute == item.route
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.labelRes)
                    )
                },
                label = { Text(text = stringResource(item.labelRes)) }
            )
        }
    }
}
