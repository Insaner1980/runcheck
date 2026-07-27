package com.runcheck.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class StableMaterialTopLevelSourceContractTest {
    private val appDir: Path = findAppDir()

    @Test
    fun `shared stable Material wrappers exist behind one component boundary`() {
        val source =
            appDir
                .resolve("src/main/java/com/runcheck/ui/components/RuncheckComponents.kt")
                .readText()

        REQUIRED_COMPONENTS.forEach { component ->
            assertTrue("$component is missing", source.contains("$component("))
        }
        val chartTheme =
            appDir
                .resolve("src/main/java/com/runcheck/ui/theme/ChartTheme.kt")
                .readText()
        assertTrue("ChartTheme is missing", chartTheme.contains("fun ChartTheme("))
        assertTrue(source.contains("LargeTopAppBar("))
        assertTrue(source.contains("CircularProgressIndicator("))
        assertTrue(source.contains("MaterialTheme.reducedMotion"))
        assertTrue(source.contains("MaterialTheme.uiTokens.touchTarget"))

        val toolEntries =
            appDir
                .resolve("src/main/java/com/runcheck/ui/tools/ToolEntryScreens.kt")
                .readText()
        assertTrue(toolEntries.contains("RuncheckDetailScaffold("))

        val trendChart =
            appDir
                .resolve("src/main/java/com/runcheck/ui/components/TrendChart.kt")
                .readText()
        assertTrue(trendChart.contains("MaterialTheme.chartColors"))
    }

    @Test
    fun `Home uses the health first viewport and no longer owns tool or expired Pro cards`() {
        val home = appDir.resolve("src/main/java/com/runcheck/ui/home/HomeScreen.kt").readText()
        val secondary = appDir.resolve("src/main/java/com/runcheck/ui/home/HomeSecondarySections.kt").readText()

        assertTrue(home.contains("HomeHealthHero("))
        assertTrue(home.contains("RuncheckProgressGauge("))
        assertTrue(home.contains("rememberFormattedDateTime("))
        assertTrue(home.contains("ConfidenceBadge("))
        assertTrue(secondary.contains("BatteryGridCard("))
        assertFalse(home.contains("BatteryHeroCard("))
        assertFalse(home.contains("HomeQuickToolsSection("))
        assertFalse(home.contains("HomeProStatusSection("))
        assertFalse(secondary.contains("ChargerGridCard("))
    }

    @Test
    fun `Tools uses one speed hero a bento grid and visible Pro locks`() {
        val tools = appDir.resolve("src/main/java/com/runcheck/ui/tools/ToolsScreen.kt").readText()

        assertTrue(tools.contains("RuncheckActionCard("))
        assertTrue(tools.contains("ToolsBentoGrid("))
        assertTrue(tools.contains("locked = !hasProAccess"))
        assertTrue(tools.contains("subtitleStyle = GridCardSubtitleStyle.BODY"))
        assertTrue(tools.contains("LearnTopicLink("))
    }

    @Test
    fun `Insights uses a top-level filter and shared stable states`() {
        val insights = appDir.resolve("src/main/java/com/runcheck/ui/insights/InsightsScreen.kt").readText()

        assertTrue(insights.contains("RuncheckSingleChoiceSelector("))
        assertTrue(insights.contains("RuncheckProgressSpinner("))
        assertTrue(insights.contains("RuncheckEmptyState("))
        assertFalse(insights.contains("DetailTopBar("))

        val homeInsights =
            appDir
                .resolve("src/main/java/com/runcheck/ui/home/insights/InsightsCard.kt")
                .readText()
        assertFalse(homeInsights.contains("if (insights.isEmpty()) return"))
        assertTrue(homeInsights.contains("RuncheckEmptyState("))

        val appUsage =
            appDir
                .resolve("src/main/java/com/runcheck/ui/appusage/AppUsageScreen.kt")
                .readText()
        assertTrue(appUsage.contains("style = MaterialTheme.typography.bodyMedium"))
    }

    @Test
    fun `Settings orders the required top-level sections`() {
        val settings = appDir.resolve("src/main/java/com/runcheck/ui/settings/SettingsScreen.kt").readText()

        assertInOrder(
            source = settings,
            tokens =
                listOf(
                    "DisplaySection(",
                    "MonitoringSection(",
                    "NotificationsSection(",
                    "DataSection(",
                    "WidgetsSection(",
                    "ProSection(",
                    "SettingsAboutSection(",
                    "DebugInsightsSection(",
                ),
        )
    }

    private fun assertInOrder(
        source: String,
        tokens: List<String>,
    ) {
        var previous = -1
        tokens.forEach { token ->
            val index = source.indexOf(token)
            assertTrue("$token is missing or out of order", index > previous)
            previous = index
        }
    }

    private fun findAppDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .flatMap { path -> sequenceOf(path, path.resolve("app")) }
            .first { Files.exists(it.resolve("src/main/res")) && Files.exists(it.resolve("build.gradle.kts")) }
    }

    private companion object {
        val REQUIRED_COMPONENTS =
            listOf(
                "RuncheckDetailScaffold",
                "RuncheckSingleChoiceSelector",
                "RuncheckActionCard",
                "InfoBanner",
                "StatusPill",
                "LearnTopicLink",
                "RuncheckEmptyState",
                "RuncheckProgressSpinner",
                "RuncheckProgressGauge",
                "AppDisplayName",
            )
    }
}
