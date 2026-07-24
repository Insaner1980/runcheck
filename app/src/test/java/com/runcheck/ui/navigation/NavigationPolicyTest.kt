package com.runcheck.ui.navigation

import com.runcheck.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPolicyTest {
    @Test
    fun `top level destinations use the required order labels and screens`() {
        assertEquals(
            listOf(
                TopLevelDestination.Home,
                TopLevelDestination.Insights,
                TopLevelDestination.Tools,
                TopLevelDestination.Settings,
            ),
            topLevelDestinations,
        )
        assertEquals(
            listOf(
                R.string.navigation_home,
                R.string.navigation_insights,
                R.string.navigation_tools,
                R.string.navigation_settings,
            ),
            topLevelDestinations.map { it.labelRes },
        )
        assertEquals(
            listOf(
                Screen.Home,
                Screen.Insights,
                Screen.Tools,
                Screen.Settings,
            ),
            topLevelDestinations.map { it.screen },
        )
        assertTrue(topLevelDestinations.all { it.screen.topLevel })
        assertFalse(Screen.Battery.topLevel)
    }

    @Test
    fun `cold start parents match the navigation contract`() {
        val homeChildren =
            listOf(
                Screen.Battery.route,
                Screen.Network.route,
                Screen.Thermal.route,
                Screen.Storage.route,
                Screen.FullscreenChart("BATTERY_SESSION", "CURRENT", "ALL").route,
            )
        val toolsChildren =
            listOf(
                Screen.SpeedTest.route,
                Screen.Cleanup("LARGE_FILES").route,
                Screen.Charger.route,
                Screen.AppUsage.route,
                Screen.WeeklyReport.route,
                Screen.Learn.route,
                Screen.LearnArticle("battery_health").route,
                Screen.Export.route,
            )

        homeChildren.forEach { route ->
            assertSame(TopLevelDestination.Home, coldStartParentFor(route))
        }
        toolsChildren.forEach { route ->
            assertSame(TopLevelDestination.Tools, coldStartParentFor(route))
        }
        assertSame(TopLevelDestination.Settings, coldStartParentFor(Screen.ProUpgrade.route))
    }

    @Test
    fun `top level routes resolve to themselves`() {
        topLevelDestinations.forEach { destination ->
            assertSame(destination, coldStartParentFor(destination.screen.route))
            assertSame(destination, topLevelDestinationFor(destination.screen.route))
        }
    }

    @Test
    fun `weekly report notification opens above Tools`() {
        assertEquals(
            listOf(Screen.Tools.route, Screen.WeeklyReport.route),
            externalNavigationPlan(Screen.WeeklyReport.route),
        )
        assertTrue(Screen.isDirectRoute(Screen.WeeklyReport.route))
    }

    @Test
    fun `validated argument routes can receive cold start parents`() {
        assertTrue(Screen.isDirectRoute(Screen.Cleanup("LARGE_FILES").route))
        assertTrue(Screen.isDirectRoute(Screen.LearnArticle("battery_health").route))
        assertTrue(
            Screen.isDirectRoute(
                Screen.FullscreenChart("BATTERY_HISTORY", "LEVEL", "DAY").route,
            ),
        )
        assertFalse(Screen.isDirectRoute("cleanup/../../settings"))
        assertFalse(Screen.isDirectRoute("fullscreen_chart/BATTERY_HISTORY/LEVEL"))
        assertFalse(Screen.isDirectRoute("learn/"))
    }

    @Test
    fun `external detail plans keep the configured parent below the target`() {
        assertEquals(
            listOf(Screen.Home.route, Screen.Battery.route),
            externalNavigationPlan(Screen.Battery.route),
        )
        assertEquals(
            listOf(Screen.Tools.route, Screen.SpeedTest.route),
            externalNavigationPlan(Screen.SpeedTest.route),
        )
        assertEquals(
            listOf(Screen.Settings.route, Screen.ProUpgrade.route),
            externalNavigationPlan(Screen.ProUpgrade.route),
        )
    }

    @Test
    fun `reselecting current tab resets it while another tab switches`() {
        assertEquals(
            TopLevelNavigationAction.RESELECT,
            topLevelNavigationAction(
                currentRoute = Screen.Tools.route,
                destination = TopLevelDestination.Tools,
            ),
        )
        assertEquals(
            TopLevelNavigationAction.SWITCH,
            topLevelNavigationAction(
                currentRoute = Screen.Home.route,
                destination = TopLevelDestination.Tools,
            ),
        )
    }

    @Test
    fun `only protected feature routes wait for pro status readiness`() {
        listOf(
            Screen.Charger.route,
            Screen.AppUsage.route,
            Screen.Cleanup("APK_FILES").route,
            Screen.Export.route,
            Screen.FullscreenChart("BATTERY_HISTORY", "LEVEL", "DAY").route,
        ).forEach { route ->
            assertTrue("$route should wait for Pro status", route.requiresReadyProStatus())
        }

        listOf(
            Screen.Home.route,
            Screen.Battery.route,
            Screen.SpeedTest.route,
            Screen.WeeklyReport.route,
            Screen.ProUpgrade.route,
        ).forEach { route ->
            assertFalse("$route should not wait for Pro status", route.requiresReadyProStatus())
        }
    }

    @Test
    fun `protected feature waits for Pro readiness before resolving locked or available content`() {
        assertEquals(
            ProtectedFeatureAccessState.WAITING_FOR_PRO_STATUS,
            protectedFeatureAccessState(proStatusReady = false, hasProAccess = false),
        )
        assertEquals(
            ProtectedFeatureAccessState.WAITING_FOR_PRO_STATUS,
            protectedFeatureAccessState(proStatusReady = false, hasProAccess = true),
        )
        assertEquals(
            ProtectedFeatureAccessState.LOCKED,
            protectedFeatureAccessState(proStatusReady = true, hasProAccess = false),
        )
        assertEquals(
            ProtectedFeatureAccessState.AVAILABLE,
            protectedFeatureAccessState(proStatusReady = true, hasProAccess = true),
        )
    }

    @Test
    fun `Tools cleanup opens the protected large files cleanup destination`() {
        val route = toolsStorageCleanupRoute()

        assertEquals(Screen.Cleanup("LARGE_FILES").route, route)
        assertTrue(route.requiresReadyProStatus())
        assertSame(TopLevelDestination.Tools, coldStartParentFor(route))
    }
}
