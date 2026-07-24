package com.runcheck.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.runcheck.R
import com.runcheck.ui.appusage.AppUsageScreen
import com.runcheck.ui.battery.BatteryDetailScreen
import com.runcheck.ui.charger.ChargerComparisonScreen
import com.runcheck.ui.fullscreen.FullscreenChartResult
import com.runcheck.ui.fullscreen.FullscreenChartScreen
import com.runcheck.ui.home.HomeScreen
import com.runcheck.ui.home.insights.InsightNavigationHandlers
import com.runcheck.ui.insights.InsightsScreen
import com.runcheck.ui.learn.LearnArticleDetailScreen
import com.runcheck.ui.learn.LearnScreen
import com.runcheck.ui.network.NetworkDetailScreen
import com.runcheck.ui.network.NetworkViewModel
import com.runcheck.ui.network.SpeedTestScreen
import com.runcheck.ui.pro.ProUpgradeScreen
import com.runcheck.ui.settings.SettingsScreen
import com.runcheck.ui.storage.StorageDetailScreen
import com.runcheck.ui.storage.cleanup.CleanupScreen
import com.runcheck.ui.theme.LocalReducedMotion
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.thermal.ThermalDetailScreen
import com.runcheck.ui.tools.ExportEntryScreen
import com.runcheck.ui.tools.ToolsScreen
import com.runcheck.ui.tools.WeeklyReportEntryScreen

@Composable
fun RuncheckNavHost(
    modifier: Modifier = Modifier,
    deepLinkRoute: String? = null,
    onConsumeDeepLink: () -> Unit = {},
) {
    val navController = rememberNavController()
    val appShellViewModel: AppShellViewModel = hiltViewModel()
    val reducedMotion = LocalReducedMotion.current
    val appShellState by appShellViewModel.uiState.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val currentTopLevelDestination = topLevelDestinationFor(currentDestination?.route)

    // Protected external routes wait for the persisted purchase state so a purchased
    // user is not briefly treated as free during app startup.
    val currentOnConsumeDeepLink by rememberUpdatedState(onConsumeDeepLink)
    LaunchedEffect(deepLinkRoute, appShellState.proStatusReady) {
        val route = deepLinkRoute ?: return@LaunchedEffect
        if (!route.requiresReadyProStatus() || appShellState.proStatusReady) {
            navController.navigateExternalRoute(route)
            currentOnConsumeDeepLink()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
        bottomBar = {
            if (currentTopLevelDestination != null) {
                RuncheckNavigationBar(
                    selectedDestination = currentTopLevelDestination,
                    unseenInsightCount = appShellState.unseenInsightCount,
                    onDestinationSelect = navController::navigateTopLevel,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            navController = navController,
            startDestination = Screen.Home.route,
            enterTransition = {
                if (reducedMotion) {
                    EnterTransition.None
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(MotionTokens.MEDIUM),
                    ) + fadeIn(animationSpec = tween(MotionTokens.MEDIUM))
                }
            },
            exitTransition = {
                if (reducedMotion) {
                    ExitTransition.None
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(MotionTokens.MEDIUM),
                    ) + fadeOut(animationSpec = tween(MotionTokens.MEDIUM))
                }
            },
            popEnterTransition = {
                if (reducedMotion) {
                    EnterTransition.None
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(MotionTokens.MEDIUM),
                    ) + fadeIn(animationSpec = tween(MotionTokens.MEDIUM))
                }
            },
            popExitTransition = {
                if (reducedMotion) {
                    ExitTransition.None
                } else {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(MotionTokens.MEDIUM),
                    ) + fadeOut(animationSpec = tween(MotionTokens.MEDIUM))
                }
            },
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToBattery = { navController.navigateSingleTop(Screen.Battery.route) },
                    onNavigateToNetwork = { navController.navigateSingleTop(Screen.Network.route) },
                    onNavigateToThermal = { navController.navigateSingleTop(Screen.Thermal.route) },
                    onNavigateToStorage = { navController.navigateSingleTop(Screen.Storage.route) },
                    onNavigateToCharger = {
                        navController.navigateNested(
                            parentRoute = Screen.Battery.route,
                            childRoute = Screen.Charger.route,
                        )
                    },
                    onNavigateToSpeedTest = {
                        navController.navigateNested(
                            parentRoute = Screen.Network.route,
                            childRoute = Screen.SpeedTest.route,
                        )
                    },
                    onNavigateToAppUsage = { navController.navigateSingleTop(Screen.AppUsage.route) },
                    onNavigateToInsights = { navController.navigateTopLevel(TopLevelDestination.Insights) },
                    onNavigateToProUpgrade = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                    onNavigateToLearn = { navController.navigateSingleTop(Screen.Learn.route) },
                    onNavigateToLearnArticle = { articleId ->
                        navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                    },
                )
            }
            composable(Screen.Insights.route) {
                val insightNavigationHandlers =
                    remember(navController) {
                        InsightNavigationHandlers(
                            onNavigateToBattery = { navController.navigateSingleTop(Screen.Battery.route) },
                            onNavigateToNetwork = { navController.navigateSingleTop(Screen.Network.route) },
                            onNavigateToThermal = { navController.navigateSingleTop(Screen.Thermal.route) },
                            onNavigateToStorage = { navController.navigateSingleTop(Screen.Storage.route) },
                            onNavigateToCharger = {
                                navController.navigateNested(
                                    parentRoute = Screen.Battery.route,
                                    childRoute = Screen.Charger.route,
                                )
                            },
                            onNavigateToAppUsage = { navController.navigateSingleTop(Screen.AppUsage.route) },
                            onNavigateToProUpgrade = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                        )
                    }
                InsightsScreen(
                    navigationHandlers = insightNavigationHandlers,
                )
            }
            composable(Screen.Tools.route) {
                ToolsScreen(
                    onNavigateToSpeedTest = { navController.navigateSingleTop(Screen.SpeedTest.route) },
                    onNavigateToStorageCleanup = { navController.navigateSingleTop(Screen.Storage.route) },
                    onNavigateToCharger = { navController.navigateSingleTop(Screen.Charger.route) },
                    onNavigateToAppUsage = { navController.navigateSingleTop(Screen.AppUsage.route) },
                    onNavigateToLearn = { navController.navigateSingleTop(Screen.Learn.route) },
                    onNavigateToWeeklyReport = { navController.navigateSingleTop(Screen.WeeklyReport.route) },
                    onNavigateToExport = { navController.navigateSingleTop(Screen.Export.route) },
                )
            }
            composable(Screen.Battery.route) { entry ->
                val resultSource by entry.savedStateHandle
                    .getStateFlow<String?>(FullscreenChartResult.KEY_SOURCE, null)
                    .collectAsStateWithLifecycle()
                val resultMetric by entry.savedStateHandle
                    .getStateFlow<String?>(FullscreenChartResult.KEY_METRIC, null)
                    .collectAsStateWithLifecycle()
                val resultPeriod by entry.savedStateHandle
                    .getStateFlow<String?>(FullscreenChartResult.KEY_PERIOD, null)
                    .collectAsStateWithLifecycle()
                BatteryDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToCharger = { navController.navigateSingleTop(Screen.Charger.route) },
                    onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                    onNavigateToFullscreen = { source, metric, period ->
                        navController.navigateSingleTop(Screen.FullscreenChart(source, metric, period).route)
                    },
                    onNavigateToLearnArticle = { articleId ->
                        navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                    },
                    fullscreenResultSource = resultSource,
                    fullscreenResultMetric = resultMetric,
                    fullscreenResultPeriod = resultPeriod,
                    onConsumeFullscreenResult = {
                        entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_SOURCE)
                        entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_METRIC)
                        entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_PERIOD)
                    },
                )
            }
            composable(Screen.Network.route) { entry ->
                val networkViewModel: NetworkViewModel = hiltViewModel(entry)
                val resultMetric by entry.savedStateHandle
                    .getStateFlow<String?>(FullscreenChartResult.KEY_METRIC, null)
                    .collectAsStateWithLifecycle()
                val resultPeriod by entry.savedStateHandle
                    .getStateFlow<String?>(FullscreenChartResult.KEY_PERIOD, null)
                    .collectAsStateWithLifecycle()
                NetworkDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToSpeedTest = { navController.navigateSingleTop(Screen.SpeedTest.route) },
                    onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                    onNavigateToFullscreen = { source, metric, period ->
                        navController.navigateSingleTop(Screen.FullscreenChart(source, metric, period).route)
                    },
                    onNavigateToLearnArticle = { articleId ->
                        navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                    },
                    fullscreenResultMetric = resultMetric,
                    fullscreenResultPeriod = resultPeriod,
                    onConsumeFullscreenResult = {
                        entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_SOURCE)
                        entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_METRIC)
                        entry.savedStateHandle.remove<String>(FullscreenChartResult.KEY_PERIOD)
                    },
                    viewModel = networkViewModel,
                )
            }
            composable(Screen.Thermal.route) {
                ThermalDetailScreen(
                    onBack = { navController.popBackStack() },
                    onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                    onNavigateToLearnArticle = { articleId ->
                        navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                    },
                )
            }
            composable(Screen.Storage.route) {
                StorageDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToCleanup = { type ->
                        navController.navigateSingleTop(Screen.Cleanup(type.name).route)
                    },
                    onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                    onNavigateToLearnArticle = { articleId ->
                        navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                    },
                )
            }
            composable(
                route = Screen.Cleanup.ROUTE,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) {
                CleanupScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Charger.route) {
                ChargerComparisonScreen(
                    onBack = { navController.popBackStack() },
                    onUpgradeToPro = {
                        navController.navigate(Screen.ProUpgrade.route) {
                            launchSingleTop = true
                            popUpTo(Screen.Charger.route) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            composable(Screen.AppUsage.route) {
                AppUsageScreen(
                    onBack = { navController.popBackStack() },
                    onUpgradeToPro = {
                        navController.navigate(Screen.ProUpgrade.route) {
                            launchSingleTop = true
                            popUpTo(Screen.AppUsage.route) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToLearnArticle = { articleId ->
                        navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                    },
                )
            }
            composable(Screen.SpeedTest.route) {
                val networkParentEntry =
                    remember(navController) {
                        runCatching { navController.getBackStackEntry(Screen.Network.route) }.getOrNull()
                    }
                val networkViewModel: NetworkViewModel =
                    if (networkParentEntry != null) {
                        hiltViewModel(networkParentEntry)
                    } else {
                        hiltViewModel()
                    }
                SpeedTestScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = networkViewModel,
                )
            }
            composable(Screen.ProUpgrade.route) {
                ProUpgradeScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.WeeklyReport.route) {
                WeeklyReportEntryScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Export.route) {
                ExportEntryScreen(
                    proStatusReady = appShellState.proStatusReady,
                    hasProAccess = appShellState.hasProAccess,
                    onBack = { navController.popBackStack() },
                    onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                )
            }
            composable(Screen.Learn.route) {
                LearnScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToArticle = { articleId ->
                        navController.navigateSingleTop(Screen.LearnArticle(articleId).route)
                    },
                )
            }
            composable(
                route = Screen.LearnArticle.ROUTE,
                arguments = listOf(navArgument("articleId") { type = NavType.StringType }),
            ) {
                val articleId = it.arguments?.getString("articleId") ?: ""
                LearnArticleDetailScreen(
                    articleId = articleId,
                    onBack = { navController.popBackStack() },
                    onNavigateToRoute = navController::navigateRoute,
                )
            }
            composable(
                route = Screen.FullscreenChart.ROUTE,
                arguments =
                    listOf(
                        navArgument("source") { type = NavType.StringType },
                        navArgument("metric") { type = NavType.StringType },
                        navArgument("period") { type = NavType.StringType },
                    ),
                enterTransition = {
                    if (reducedMotion) {
                        EnterTransition.None
                    } else {
                        scaleIn(
                            initialScale = 0.94f,
                            animationSpec = tween(MotionTokens.FULLSCREEN_ENTER_SCALE),
                        ) + fadeIn(animationSpec = tween(MotionTokens.FULLSCREEN_ENTER_FADE))
                    }
                },
                exitTransition = {
                    if (reducedMotion) {
                        ExitTransition.None
                    } else {
                        scaleOut(
                            targetScale = 1.02f,
                            animationSpec = tween(MotionTokens.FULLSCREEN_EXIT),
                        ) + fadeOut(animationSpec = tween(MotionTokens.FULLSCREEN_EXIT))
                    }
                },
                popEnterTransition = {
                    if (reducedMotion) {
                        EnterTransition.None
                    } else {
                        fadeIn(animationSpec = tween(MotionTokens.FULLSCREEN_EXIT))
                    }
                },
                popExitTransition = {
                    if (reducedMotion) {
                        ExitTransition.None
                    } else {
                        scaleOut(
                            targetScale = 0.96f,
                            animationSpec = tween(MotionTokens.FULLSCREEN_ENTER_FADE),
                        ) + fadeOut(animationSpec = tween(MotionTokens.FULLSCREEN_ENTER_FADE))
                    }
                },
            ) {
                FullscreenChartScreen(
                    onBack = { navController.popBackStack() },
                    onUpgradeToPro = { navController.navigateSingleTop(Screen.ProUpgrade.route) },
                    onSelectionChange = { source, metric, period ->
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set(FullscreenChartResult.KEY_SOURCE, source)
                            set(FullscreenChartResult.KEY_METRIC, metric)
                            set(FullscreenChartResult.KEY_PERIOD, period)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun RuncheckNavigationBar(
    selectedDestination: TopLevelDestination,
    unseenInsightCount: Int,
    onDestinationSelect: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        topLevelDestinations.forEach { destination ->
            val isInsights = destination == TopLevelDestination.Insights
            val hasUnseenInsights = isInsights && unseenInsightCount > 0
            val unseenDescription =
                if (hasUnseenInsights) {
                    pluralStringResource(
                        id = R.plurals.navigation_insights_unseen,
                        count = unseenInsightCount,
                        unseenInsightCount,
                    )
                } else {
                    null
                }

            NavigationBarItem(
                selected = selectedDestination == destination,
                onClick = { onDestinationSelect(destination) },
                icon = {
                    if (hasUnseenInsights) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(
                                        text =
                                            if (unseenInsightCount > MAX_BADGE_COUNT) {
                                                "$MAX_BADGE_COUNT+"
                                            } else {
                                                unseenInsightCount.toString()
                                            },
                                    )
                                }
                            },
                            modifier =
                                Modifier.semantics {
                                    contentDescription = unseenDescription.orEmpty()
                                },
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = null,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                        )
                    }
                },
                label = { Text(stringResource(destination.labelRes)) },
                alwaysShowLabel = true,
            )
        }
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

internal fun NavHostController.navigateRoute(route: String) {
    val topLevelDestination = topLevelDestinationFor(route)
    if (topLevelDestination != null) {
        navigateTopLevel(topLevelDestination)
    } else {
        navigateSingleTop(route)
    }
}

internal fun NavHostController.navigateExternalRoute(route: String) {
    if (currentBackStackEntry?.destination?.route == route) {
        return
    }
    val plan = externalNavigationPlan(route)
    val parent = topLevelDestinationFor(plan.firstOrNull()) ?: return
    navigateExternalParentRoot(parent)
    if (route != parent.screen.route) {
        navigateSingleTop(route)
    }
}

private fun NavHostController.navigateExternalParentRoot(parent: TopLevelDestination) {
    navigate(parent.screen.route) {
        launchSingleTop = true
        restoreState = false
        popUpTo(graph.findStartDestination().id) {
            saveState = false
        }
    }
}

internal fun NavHostController.navigateTopLevel(destination: TopLevelDestination) {
    when (topLevelNavigationAction(currentDestination?.route, destination)) {
        TopLevelNavigationAction.RESELECT -> {
            popBackStack(destination.screen.route, inclusive = false)
        }

        TopLevelNavigationAction.SWITCH -> {
            navigate(destination.screen.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(graph.findStartDestination().id) {
                    saveState = true
                }
            }
        }
    }
}

private fun NavHostController.navigateNested(
    parentRoute: String,
    childRoute: String,
) {
    navigateSingleTop(parentRoute)
    navigateSingleTop(childRoute)
}

private const val MAX_BADGE_COUNT = 99
