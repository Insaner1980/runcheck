package com.runcheck.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProRouteAccessPolicyTest {
    @Test
    fun `protected routes wait until pro access is ready`() {
        protectedRoutes.forEach { route ->
            assertNull(resolveProRoute(route, proAccessReady = false, isPro = false))
            assertNull(resolveProRoute(route, proAccessReady = false, isPro = true))
        }
    }

    @Test
    fun `protected routes redirect free users to pro upgrade`() {
        protectedRoutes.forEach { route ->
            assertEquals(
                Screen.ProUpgrade.route,
                resolveProRoute(route, proAccessReady = true, isPro = false),
            )
        }
    }

    @Test
    fun `protected routes remain available to purchased pro access`() {
        protectedRoutes.forEach { route ->
            assertEquals(
                route,
                resolveProRoute(route, proAccessReady = true, isPro = true),
            )
        }
    }

    @Test
    fun `unprotected deep link routes do not wait for pro access`() {
        assertEquals(
            Screen.Battery.route,
            resolveProRoute(Screen.Battery.route, proAccessReady = false, isPro = false),
        )
    }

    private companion object {
        val protectedRoutes = listOf(Screen.Charger.route, Screen.AppUsage.route)
    }
}
