package com.runcheck.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class OpenSourceNoticesContractTest {
    private val appDir: Path = findAppDir()

    @Test
    fun `settings exposes open source licenses notice`() {
        val settingsScreen = appDir.resolve("src/main/java/com/runcheck/ui/settings/SettingsScreen.kt").readText()
        val strings = appDir.resolve("src/main/res/values/strings.xml").readText()

        assertTrue(settingsScreen.contains("R.raw.third_party_notices"))
        assertTrue(settingsScreen.contains("settings_open_source_licenses"))
        assertTrue(strings.contains("""<string name="settings_open_source_licenses">Open source licenses</string>"""))
    }

    @Test
    fun `third party notices cover production license surface`() {
        val notices = appDir.resolve("src/main/res/raw/third_party_notices.txt").readText()
        val expectedSnippets =
            listOf(
                "AndroidX and Jetpack Compose",
                "Compose Material Icons Extended",
                "AndroidX repackaged protobuf artifacts",
                "Google Play Billing and Google Play services",
                "M-Lab NDT7 Android client",
                "e0cb663613eb252a7793216ad28cf54a35677b8f",
                "OkHttp and Okio",
                "Gson",
                "Guava ListenableFuture",
                "Manrope font",
                "JetBrains Mono font",
                "Apache-2.0",
                "BSD-3-Clause",
                "SIL Open Font License 1.1",
                "Android Software Development Kit License",
            )

        val missingSnippets = expectedSnippets.filterNot(notices::contains)
        assertTrue("Missing notice snippets: $missingSnippets", missingSnippets.isEmpty())
    }

    private fun findAppDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .flatMap { path -> sequenceOf(path, path.resolve("app")) }
            .first { Files.exists(it.resolve("src/main/res")) && Files.exists(it.resolve("build.gradle.kts")) }
    }
}
