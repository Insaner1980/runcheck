package com.runcheck.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class WidgetRegistrationContractTest {
    private val appDir = findAppDir()

    @Test
    fun `all three widget receivers and previews are registered`() {
        val manifest = appDir.resolve("src/main/AndroidManifest.xml").readText()
        val quickInfo = appDir.resolve("src/main/res/xml/quick_glance_widget_info.xml")
        val quickPreview = appDir.resolve("src/main/res/layout/widget_quick_glance_preview.xml")

        assertTrue(manifest.contains(".widget.BatteryWidgetReceiver"))
        assertTrue(manifest.contains(".widget.HealthWidgetReceiver"))
        assertTrue(manifest.contains(".widget.QuickGlanceWidgetReceiver"))
        assertTrue(manifest.contains("@xml/quick_glance_widget_info"))
        assertTrue(Files.exists(quickInfo))
        assertTrue(Files.exists(quickPreview))
        val info = quickInfo.readText()
        assertTrue(info.contains("""android:targetCellWidth="4""""))
        assertTrue(info.contains("""android:targetCellHeight="2""""))
        assertTrue(info.contains("""android:previewLayout="@layout/widget_quick_glance_preview""""))
    }

    @Test
    fun `widget theme uses day and night providers without app theme mode`() {
        val common = appDir.resolve("src/main/java/com/runcheck/widget/WidgetCommon.kt").readText()
        val dayColors = appDir.resolve("src/main/res/values/colors.xml").readText()
        val nightColors = appDir.resolve("src/main/res/values-night/colors.xml").readText()

        assertTrue(common.contains("ColorProvider(day ="))
        assertTrue(common.contains("night ="))
        assertFalse(common.contains("ThemeMode"))
        assertTrue(dayColors.contains("""name="widget_background">#E9EFF1"""))
        assertTrue(nightColors.contains("""name="widget_background">#133040"""))
    }

    @Test
    fun `all widget refresh updates quick glance too`() {
        val provider = appDir.resolve("src/main/java/com/runcheck/widget/WidgetDataProvider.kt").readText()

        assertTrue(provider.contains("BatteryWidget().updateAll(context)"))
        assertTrue(provider.contains("HealthWidget().updateAll(context)"))
        assertTrue(provider.contains("QuickGlanceWidget().updateAll(context)"))
    }

    private fun findAppDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .flatMap { path -> sequenceOf(path, path.resolve("app")) }
            .first { Files.exists(it.resolve("src/main/res")) && Files.exists(it.resolve("build.gradle.kts")) }
    }
}
