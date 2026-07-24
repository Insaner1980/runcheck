package com.runcheck.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class ExpressiveDetailSourceContractTest {
    private val appDir: Path = findAppDir()

    @Test
    fun `primary details use one expressive scaffold banner loading and learn link`() {
        PRIMARY_DETAILS.forEach { relativePath ->
            val source = appDir.resolve(relativePath).readText()

            assertTrue("$relativePath must use ExpressiveDetailScaffold", source.contains("ExpressiveDetailScaffold("))
            assertTrue("$relativePath must use RuncheckLoadingIndicator", source.contains("RuncheckLoadingIndicator("))
            assertTrue("$relativePath must use one InfoBanner", source.contains("InfoBanner("))
            assertTrue("$relativePath must expose one topic Learn link", source.contains("LearnTopicLink("))
            assertFalse("$relativePath must not render legacy InfoCard stacks", source.contains("InfoCard("))
        }
    }

    @Test
    fun `history selectors delegate to the shared expressive connected control`() {
        val source =
            appDir
                .resolve("src/main/java/com/runcheck/ui/common/ChartSelection.kt")
                .readText()

        assertTrue(source.contains("ExpressiveSingleChoiceSelector("))
        assertFalse(source.contains("FilterChip("))
    }

    @Test
    fun `support screens use shared expressive states and required action surfaces`() {
        val speed = source("network/SpeedTestScreen.kt")
        assertTrue(speed.contains("ExpressiveDetailScaffold("))
        assertTrue(speed.contains("RuncheckLoadingIndicator("))
        assertTrue(speed.contains("stopSpeedTest"))

        val cleanup = source("storage/cleanup/CleanupBottomBar.kt")
        assertTrue(cleanup.contains("HorizontalFloatingToolbar("))

        val charger = source("charger/ChargerComparisonScreen.kt")
        assertTrue(charger.contains("ExtendedFloatingActionButton("))
        assertTrue(charger.contains("ExpressiveEmptyState("))
        assertFalse(charger.contains("Start session"))

        val appUsage = source("appusage/AppUsageScreen.kt")
        assertTrue(appUsage.contains("ExpressiveSingleChoiceSelector("))
        assertTrue(appUsage.contains("app.packageName"))
        assertTrue(appUsage.contains("AppDisplayName("))

        val learn = source("learn/LearnScreen.kt")
        assertTrue(learn.contains("ExpressiveSingleChoiceSelector("))

        val pro = source("pro/ProUpgradeScreen.kt")
        assertTrue(pro.contains("ExpressiveDetailScaffold("))

        val fullscreen = source("fullscreen/FullscreenChartScreen.kt")
        assertTrue(fullscreen.contains("ExpressiveSingleChoiceSelector("))
        assertFalse(fullscreen.contains("FilterChip("))
    }

    @Test
    fun `export owns its UI and ViewModel while retaining the shared use case`() {
        val screen = source("export/ExportScreen.kt")
        val viewModel = source("export/ExportViewModel.kt")

        assertTrue(screen.contains("ExpressiveDetailScaffold("))
        assertTrue(screen.contains("RuncheckLoadingIndicator("))
        assertTrue(viewModel.contains("ExportDataUseCase"))
        assertTrue(viewModel.contains("prepareExportShare()"))
    }

    @Test
    fun `storage explains cache measurement as read only`() {
        val strings = appDir.resolve("src/main/res/values/strings.xml").readText()

        assertTrue(strings.contains("can measure cache storage"))
        assertTrue(strings.contains("cannot clear other apps"))
    }

    private fun source(relativePath: String): String =
        appDir
            .resolve("src/main/java/com/runcheck/ui/$relativePath")
            .readText()

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
