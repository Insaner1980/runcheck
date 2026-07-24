package com.runcheck.ui.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NavigationControllerBehaviorTest {
    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        navController = TestNavHostController(RuntimeEnvironment.getApplication())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        navController.setViewModelStore(ViewModelStore())
        navController.setLifecycleOwner(ResumedLifecycleOwner())
        navController.graph =
            navController.createGraph(startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {}
                composable(Screen.Tools.route) {}
                composable(Screen.SpeedTest.route) {}
                composable(Screen.WeeklyReport.route) {}
                composable(Screen.Learn.route) {}
                composable(Screen.LearnArticle.ROUTE) {}
                composable(Screen.Settings.route) {}
            }
    }

    @Test
    fun `external Weekly route resets a restored Tools detail stack`() {
        navController.navigateTopLevel(TopLevelDestination.Tools)
        navController.navigate(Screen.SpeedTest.route)

        navController.navigateExternalRoute(Screen.WeeklyReport.route)

        assertEquals(Screen.WeeklyReport.route, navController.currentDestination?.route)
        navController.popBackStack()
        assertEquals(Screen.Tools.route, navController.currentDestination?.route)
        navController.popBackStack()
        assertEquals(Screen.Home.route, navController.currentDestination?.route)
    }

    @Test
    fun `Learn cross-link to Settings uses top-level back semantics`() {
        navController.navigateTopLevel(TopLevelDestination.Tools)
        navController.navigate(Screen.Learn.route)
        navController.navigate(Screen.LearnArticle("battery_health").route)

        navController.navigateRoute(Screen.Settings.route)

        assertEquals(Screen.Settings.route, navController.currentDestination?.route)
        navController.popBackStack()
        assertEquals(Screen.Home.route, navController.currentDestination?.route)
    }

    private class ResumedLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle = registry

        init {
            registry.currentState = Lifecycle.State.RESUMED
        }
    }
}
