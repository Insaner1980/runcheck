package com.runcheck.ui

import com.runcheck.util.readContractText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class StableMaterialDetailSourceContractTest {
    private val appDir: Path = findAppDir()

    @Test
    fun `primary details use one stable scaffold banner progress and learn link`() {
        PRIMARY_DETAILS.forEach { relativePath ->
            val source = appDir.resolve(relativePath).readContractText()

            assertTrue("$relativePath must use RuncheckDetailScaffold", source.contains("RuncheckDetailScaffold("))
            assertTrue("$relativePath must use RuncheckProgressSpinner", source.contains("RuncheckProgressSpinner("))
            assertTrue("$relativePath must use one InfoBanner", source.contains("InfoBanner("))
            assertTrue("$relativePath must expose one topic Learn link", source.contains("LearnTopicLink("))
            assertFalse("$relativePath must not render legacy InfoCard stacks", source.contains("InfoCard("))
        }
    }

    @Test
    fun `history selectors delegate to the shared stable connected control`() {
        val source =
            appDir
                .resolve("src/main/java/com/runcheck/ui/common/ChartSelection.kt")
                .readContractText()

        assertTrue(source.contains("RuncheckSingleChoiceSelector("))
        assertFalse(source.contains("FilterChip("))
    }

    @Test
    fun `support screens use shared stable states and required action surfaces`() {
        val speed = source("network/SpeedTestScreen.kt")
        assertTrue(speed.contains("RuncheckDetailScaffold("))
        assertTrue(speed.contains("RuncheckProgressSpinner("))
        assertTrue(speed.contains("stopSpeedTest"))

        val cleanup = source("storage/cleanup/CleanupBottomBar.kt")
        assertTrue(cleanup.contains("Surface("))
        assertTrue(cleanup.contains("defaultMinSize(minHeight = MaterialTheme.uiTokens.touchTarget)"))

        val charger = source("charger/ChargerComparisonScreen.kt")
        assertTrue(charger.contains("ExtendedFloatingActionButton("))
        assertTrue(charger.contains("RuncheckEmptyState("))
        assertTrue(charger.contains("ChargerEmptyIllustration("))
        assertTrue(charger.contains("StatusPill("))
        assertFalse(charger.contains("Start session"))

        val appUsage = source("appusage/AppUsageScreen.kt")
        assertTrue(appUsage.contains("RuncheckSingleChoiceSelector("))
        assertTrue(appUsage.contains("app.packageName"))
        assertTrue(appUsage.contains("AppDisplayName("))
        assertFalse(appUsage.contains("estimatedDrainMah"))
        val strings = appDir.resolve("src/main/res/values/strings.xml").readContractText()
        assertFalse(strings.contains("app_usage_drain"))

        val learn = source("learn/LearnScreen.kt")
        assertTrue(learn.contains("RuncheckSingleChoiceSelector("))
        assertTrue(learn.contains("LearnArticleCatalog.generalArticles"))
        assertTrue(learn.contains("R.string.learn_topic_general"))

        val pro = source("pro/ProUpgradeScreen.kt")
        assertTrue(pro.contains("RuncheckDetailScaffold("))

        val fullscreen = source("fullscreen/FullscreenChartScreen.kt")
        assertTrue(fullscreen.contains("RuncheckSingleChoiceSelector("))
        assertFalse(fullscreen.contains("FilterChip("))
    }

    @Test
    fun `battery charger comparison is a secondary link instead of a primary panel action`() {
        val battery = source("battery/BatteryDetailScreen.kt")

        assertTrue(battery.contains("SecondaryActionLink("))
        assertFalse(battery.contains("Button(\n                onClick = onNavigateToCharger"))
    }

    @Test
    fun `network and thermal heroes expose confidence aware measurements`() {
        val network = source("network/NetworkDetailScreen.kt")
        val thermal = source("thermal/ThermalDetailScreen.kt")

        assertTrue(network.contains("platformTelemetryMeasurement("))
        assertTrue(network.contains("MeasuredHeroValue("))
        assertTrue(thermal.contains("platformTelemetryMeasurement("))
        assertTrue(thermal.contains("MeasuredHeroValue("))
        val sharedComponents =
            appDir.resolve("src/main/java/com/runcheck/ui/components/RuncheckComponents.kt").readContractText()
        assertTrue(sharedComponents.contains("ConfidenceBadge(confidence = confidence)"))
    }

    @Test
    fun `export owns its UI and ViewModel while retaining the shared use case`() {
        val screen = source("export/ExportScreen.kt")
        val viewModel = source("export/ExportViewModel.kt")

        assertTrue(screen.contains("RuncheckDetailScaffold("))
        assertTrue(screen.contains("RuncheckProgressSpinner("))
        assertTrue(viewModel.contains("ExportDataUseCase"))
        assertTrue(viewModel.contains("prepareExportShare()"))
    }

    @Test
    fun `storage explains cache measurement as read only`() {
        val strings = appDir.resolve("src/main/res/values/strings.xml").readContractText()

        assertTrue(strings.contains("can measure cache storage"))
        assertTrue(strings.contains("cannot clear other apps"))
    }

    private fun source(relativePath: String): String =
        appDir
            .resolve("src/main/java/com/runcheck/ui/$relativePath")
            .readContractText()

    private fun findAppDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .flatMap { path -> sequenceOf(path, path.resolve("app")) }
            .first { Files.exists(it.resolve("src/main/res")) && Files.exists(it.resolve("build.gradle.kts")) }
    }

    private companion object {
        val PRIMARY_DETAILS =
            listOf(
                "src/main/java/com/runcheck/ui/battery/BatteryDetailScreen.kt",
                "src/main/java/com/runcheck/ui/network/NetworkDetailScreen.kt",
                "src/main/java/com/runcheck/ui/thermal/ThermalDetailScreen.kt",
                "src/main/java/com/runcheck/ui/storage/StorageDetailScreen.kt",
            )
    }
}
