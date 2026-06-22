package com.runcheck.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText

class EnglishOnlyResourceContractTest {
    private val appDir: Path = findAppDir()

    @Test
    fun `english only locale contract is declared consistently`() {
        val buildFile = appDir.resolve("build.gradle.kts").readText()
        assertTrue(buildFile.contains("""localeFilters += listOf("en")"""))

        val manifest = appDir.resolve("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("""android:localeConfig="@xml/locales_config""""))

        val localeConfig = appDir.resolve("src/main/res/xml/locales_config.xml").readText()
        val declaredLocales =
            Regex("""<locale\s+android:name="([^"]+)"""")
                .findAll(localeConfig)
                .map { it.groupValues[1] }
                .toList()
        assertEquals(listOf("en"), declaredLocales)

        val localizedValueDirs =
            Files
                .walk(appDir.resolve("src"))
                .use { paths ->
                    paths
                        .filter { it.isDirectory() && it.name.startsWith("values-") }
                        .map { appDir.relativize(it).toString() }
                        .sorted()
                        .toList()
                }
        assertEquals(emptyList<String>(), localizedValueDirs)
    }

    @Test
    fun `xml layout text comes from resources`() {
        val layoutDir = appDir.resolve("src/main/res/layout")
        val layoutFiles =
            Files
                .walk(layoutDir)
                .use { paths ->
                    paths
                        .filter { it.toString().endsWith(".xml") }
                        .toList()
                }
        val hardcodedText =
            layoutFiles
                .flatMap { path ->
                    Regex("""android:text="(?!@)[^"]+"""")
                        .findAll(path.readText())
                        .map { "${appDir.relativize(path)}:${it.value}" }
                        .toList()
                }.sorted()

        assertEquals(emptyList<String>(), hardcodedText)
    }

    private fun findAppDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .flatMap { path -> sequenceOf(path, path.resolve("app")) }
            .first { Files.exists(it.resolve("src/main/res")) && Files.exists(it.resolve("build.gradle.kts")) }
    }
}
