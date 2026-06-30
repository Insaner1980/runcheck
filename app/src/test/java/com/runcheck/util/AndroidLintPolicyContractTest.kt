package com.runcheck.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class AndroidLintPolicyContractTest {
    private val appDir: Path = findAppDir()

    @Test
    fun `production code avoids high risk Android lint APIs`() {
        val productionKotlin =
            Files
                .walk(appDir.resolve("src/main/java"))
                .use { paths ->
                    paths
                        .filter { it.toString().endsWith(".kt") }
                        .map { path -> appDir.relativize(path) to path.readText() }
                        .toList()
                }

        val violations =
            productionKotlin
                .flatMap { (relativePath, text) ->
                    listOfNotNull(
                        violation(relativePath, text, "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"),
                        violation(relativePath, text, "Class.forName(\"com.android.internal."),
                        violation(relativePath, text, "Class.forName(\"android.os.SystemProperties\")"),
                        violation(relativePath, text, ".getInstalledApplications("),
                    )
                }.sorted()

        assertEquals(emptyList<String>(), violations)
    }

    @Test
    fun `manifest declares explicit data extraction rules`() {
        val manifest = appDir.resolve("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("""android:dataExtractionRules="@xml/data_extraction_rules""""))
        assertTrue(manifest.contains("""android:fullBackupContent="@xml/backup_rules""""))

        val dataExtractionRules = appDir.resolve("src/main/res/xml/data_extraction_rules.xml")
        assertTrue(Files.exists(dataExtractionRules))
        assertTrue(dataExtractionRules.readText().contains("<data-extraction-rules>"))

        val backupRules = appDir.resolve("src/main/res/xml/backup_rules.xml")
        assertTrue(Files.exists(backupRules))
        assertTrue(backupRules.readText().contains("<full-backup-content>"))
    }

    @Test
    fun `manifest keeps App Startup provider while disabling WorkManager default initializer`() {
        val manifest = appDir.resolve("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""android:name="androidx.startup.InitializationProvider""""))
        assertTrue(manifest.contains("""tools:node="merge""""))
        assertTrue(manifest.contains("""android:name="androidx.work.WorkManagerInitializer""""))
        assertTrue(manifest.contains("""tools:node="remove""""))
        assertTrue(
            "Remove only WorkManagerInitializer metadata so ProfileInstaller startup remains available",
            !Regex(
                pattern = """<provider\s+[^>]*android:name="androidx\.startup\.InitializationProvider"[^>]*tools:node="remove"""",
                option = RegexOption.DOT_MATCHES_ALL,
            ).containsMatchIn(manifest),
        )
    }

    private fun violation(
        relativePath: Path,
        text: String,
        forbiddenText: String,
    ): String? =
        if (text.contains(forbiddenText)) {
            "$relativePath contains $forbiddenText"
        } else {
            null
        }

    private fun findAppDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .flatMap { path -> sequenceOf(path, path.resolve("app")) }
            .first { Files.exists(it.resolve("src/main/res")) && Files.exists(it.resolve("build.gradle.kts")) }
    }
}
