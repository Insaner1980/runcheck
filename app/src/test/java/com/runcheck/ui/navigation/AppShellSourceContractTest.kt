package com.runcheck.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class AppShellSourceContractTest {
    private val appDir: Path = findAppDir()

    @Test
    fun `root app shell uses Scaffold and a four destination NavigationBar`() {
        val source = appDir.resolve("src/main/java/com/runcheck/ui/navigation/NavGraph.kt").readText()

        assertTrue(source.contains("Scaffold("))
        assertTrue(source.contains("NavigationBar("))
        assertTrue(source.contains("topLevelDestinations.forEach"))
        assertTrue(source.contains("currentDestination?.route"))
    }

    @Test
    fun `top level switching uses the official multiple back stack options`() {
        val source = appDir.resolve("src/main/java/com/runcheck/ui/navigation/NavGraph.kt").readText()

        assertTrue(source.contains("launchSingleTop = true"))
        assertTrue(source.contains("restoreState = true"))
        assertTrue(source.contains("findStartDestination().id"))
        assertTrue(source.contains("saveState = true"))
    }

    @Test
    fun `Home no longer owns a Settings action`() {
        val source = appDir.resolve("src/main/java/com/runcheck/ui/home/HomeScreen.kt").readText()

        assertFalse(source.contains("onNavigateToSettings"))
        assertFalse(source.contains("Icons.Outlined.Settings"))
    }

    @Test
    fun `top level content delegates bottom insets to the root Scaffold`() {
        val home = appDir.resolve("src/main/java/com/runcheck/ui/home/HomeScreen.kt").readText()
        val insights = appDir.resolve("src/main/java/com/runcheck/ui/insights/InsightsScreen.kt").readText()
        val navGraph = appDir.resolve("src/main/java/com/runcheck/ui/navigation/NavGraph.kt").readText()

        assertFalse(home.contains("navigationBarsPadding"))
        assertFalse(insights.contains("navigationBarsPadding"))
        assertFalse(navGraph.contains("contentWindowInsets = WindowInsets(0"))
        assertTrue(
            navGraph.contains(
                "contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)",
            ),
        )
        assertTrue(navGraph.contains(".consumeWindowInsets(innerPadding)"))
    }

    @Test
    fun `top level destinations do not expose an Up action`() {
        val insights = appDir.resolve("src/main/java/com/runcheck/ui/insights/InsightsScreen.kt").readText()
        val settings = appDir.resolve("src/main/java/com/runcheck/ui/settings/SettingsScreen.kt").readText()

        assertTrue(insights.contains("PrimaryTopBar("))
        assertFalse(insights.contains("DetailTopBar("))
        assertTrue(settings.contains("PrimaryTopBar("))
        assertFalse(settings.contains("DetailTopBar("))
    }

    @Test
    fun `Export route uses the standard Pro locked state`() {
        val tools = appDir.resolve("src/main/java/com/runcheck/ui/tools/ToolEntryScreens.kt").readText()

        assertTrue(tools.contains("ProFeatureLockedState("))
        assertTrue(tools.contains("ProtectedFeatureAccessState.WAITING_FOR_PRO_STATUS"))
        assertTrue(tools.contains("ProtectedFeatureAccessState.LOCKED"))
        assertTrue(tools.contains("ProtectedFeatureAccessState.AVAILABLE"))
    }

    private fun findAppDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .flatMap { path -> sequenceOf(path, path.resolve("app")) }
            .first { Files.exists(it.resolve("src/main/res")) && Files.exists(it.resolve("build.gradle.kts")) }
    }
}
