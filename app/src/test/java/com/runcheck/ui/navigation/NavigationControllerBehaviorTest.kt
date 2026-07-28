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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        navController = createNavController()
    }

    @Test
    fun `all four top level roots are reachable through production navigation`() {
        topLevelDestinations.forEach { destination ->
            navController.navigateTopLevel(destination)

            assertEquals(destination.screen.route, navController.currentDestination?.route)
        }
    }

    @Test
    fun `Home and Tools restore their own detail destination after tab switches`() {
        navController.navigate(Screen.Battery.route)
        navController.navigateTopLevel(TopLevelDestination.Tools)
        navController.navigate(Screen.SpeedTest.route)

        navController.navigateTopLevel(TopLevelDestination.Home)
        assertEquals(Screen.Battery.route, navController.currentDestination?.route)

        navController.navigateTopLevel(TopLevelDestination.Tools)
        assertEquals(Screen.SpeedTest.route, navController.currentDestination?.route)
    }

    @Test
    fun `reselecting the current tab from its detail returns to that tab root`() {
        navController.navigateTopLevel(TopLevelDestination.Tools)
        navController.navigate(Screen.SpeedTest.route)

        navController.navigateTopLevel(TopLevelDestination.Tools)

        assertEquals(Screen.Tools.route, navController.currentDestination?.route)
        assertTrue(navController.popBackStack())
        assertEquals(Screen.Home.route, navController.currentDestination?.route)
    }

    @Test
    fun `selecting a detail canonical tab opens its root when that root is absent`() {
        navController.navigate(Screen.AppUsage.route)

        navController.navigateTopLevel(TopLevelDestination.Tools)

        assertEquals(Screen.Tools.route, navController.currentDestination?.route)
    }

    @Test
    fun `Back from every non Home root returns Home and next Back exits`() {
        listOf(
            TopLevelDestination.Insights,
            TopLevelDestination.Tools,
            TopLevelDestination.Settings,
        ).forEach { destination ->
            val controller = createNavController()
            controller.navigateTopLevel(destination)

            assertTrue(controller.popBackStack())
            assertEquals(Screen.Home.route, controller.currentDestination?.route)
            assertFalse(controller.popBackStack())
        }
    }

    @Test
    fun `protected deep link stays pending until Pro readiness then consumes once`() {
        val protectedRoute = Screen.AppUsage.route

        assertFalse(
            navController.navigateExternalRouteWhenReady(
                route = protectedRoute,
                proStatusReady = false,
            ),
        )
        assertEquals(Screen.Home.route, navController.currentDestination?.route)

        assertTrue(
            navController.navigateExternalRouteWhenReady(
                route = protectedRoute,
                proStatusReady = true,
            ),
        )
        assertEquals(Screen.AppUsage.route, navController.currentDestination?.route)
        navController.popBackStack()
        assertEquals(Screen.Tools.route, navController.currentDestination?.route)
    }

    @Test
    fun `unprotected deep link navigates before Pro readiness`() {
        assertTrue(
            navController.navigateExternalRouteWhenReady(
                route = Screen.Battery.route,
                proStatusReady = false,
            ),
        )

        assertEquals(Screen.Battery.route, navController.currentDestination?.route)
        navController.popBackStack()
        assertEquals(Screen.Home.route, navController.currentDestination?.route)
    }

    @Test
    fun `top level save restore preserves the destination scroll state contract`() {
        navController.navigateTopLevel(TopLevelDestination.Tools)
        navController.currentBackStackEntry?.savedStateHandle?.set(SCROLL_INDEX_KEY, 37)

        navController.navigateTopLevel(TopLevelDestination.Insights)
        navController.navigateTopLevel(TopLevelDestination.Tools)

        assertEquals(
            37,
            navController.currentBackStackEntry?.savedStateHandle?.get<Int>(SCROLL_INDEX_KEY),
        )
    }

    @Test
    fun `external Weekly route resets a restored Tools detail stack under Insights`() {
        navController.navigateTopLevel(TopLevelDestination.Tools)
        navController.navigate(Screen.SpeedTest.route)

        navController.navigateExternalRoute(Screen.WeeklyReport.route)

        assertEquals(Screen.WeeklyReport.route, navController.currentDestination?.route)
        navController.popBackStack()
        assertEquals(Screen.Insights.route, navController.currentDestination?.route)
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

    private fun createNavController(): TestNavHostController {
        val controller = TestNavHostController(RuntimeEnvironment.getApplication())
        controller.navigatorProvider.addNavigator(ComposeNavigator())
        controller.setViewModelStore(ViewModelStore())
        controller.setLifecycleOwner(ResumedLifecycleOwner())
        controller.graph =
            controller.createGraph(startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {}
                composable(Screen.Insights.route) {}
                composable(Screen.Tools.route) {}
                composable(Screen.Settings.route) {}
                composable(Screen.Battery.route) {}
                composable(Screen.Network.route) {}
                composable(Screen.Thermal.route) {}
                composable(Screen.Storage.route) {}
                composable(Screen.SpeedTest.route) {}
                composable(Screen.WeeklyReport.route) {}
                composable(Screen.Learn.route) {}
                composable(Screen.LearnArticle.ROUTE) {}
                composable(Screen.AppUsage.route) {}
            }
        return controller
    }

    private class ResumedLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)

        override val lifecycle: Lifecycle = registry

        init {
            registry.currentState = Lifecycle.State.RESUMED
        }
    }

    private companion object {
        const val SCROLL_INDEX_KEY = "scroll_index"
    }
}
