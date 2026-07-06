package com.runcheck.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class DependencyVersionCatalogContractTest {
    private val rootDir: Path = findRootDir()

    @Test
    fun `KSP plugin stays on the verified current release line`() {
        val versionsCatalog = rootDir.resolve("gradle/libs.versions.toml").readText()
        val kspVersion = versionsCatalog.versionFor("ksp")

        assertTrue(
            "KSP version $kspVersion is older than the verified current 2.3.9 release line",
            kspVersion.isAtLeast("2.3.9"),
        )
    }

    @Test
    fun `WorkManager stays on the periodic reschedule fix release line`() {
        val versionsCatalog = rootDir.resolve("gradle/libs.versions.toml").readText()
        val workManagerVersion = versionsCatalog.versionFor("workmanager")

        assertTrue(
            "WorkManager version $workManagerVersion is older than the 2.11.2 periodic work reschedule fix",
            workManagerVersion.isAtLeast("2.11.2"),
        )
    }

    @Test
    fun `Android toolchain stays on the verified Hilt 1_4 compatible release line`() {
        val versionsCatalog = rootDir.resolve("gradle/libs.versions.toml").readText()
        val wrapperProperties = rootDir.resolve("gradle/wrapper/gradle-wrapper.properties").readText()

        val agpVersion = versionsCatalog.versionFor("agp")
        val hiltAndroidxVersion = versionsCatalog.versionFor("hiltAndroidx")
        val dependencyAnalysisVersion = versionsCatalog.versionFor("dependencyAnalysis")

        assertTrue(
            "AGP version $agpVersion is older than the 9.2.1 release line verified with AndroidX Hilt 1.4.0",
            agpVersion.isAtLeast("9.2.1"),
        )
        assertTrue(
            "AndroidX Hilt version $hiltAndroidxVersion is older than the 1.4.0 release line",
            hiltAndroidxVersion.isAtLeast("1.4.0"),
        )
        assertTrue(
            "Dependency Analysis version $dependencyAnalysisVersion must support AGP 9.2.1",
            dependencyAnalysisVersion.isAtLeast("3.16.0"),
        )
        assertTrue(
            "Gradle wrapper must stay on the AGP 9.2 compatible 9.4.1 distribution",
            wrapperProperties.contains("gradle-9.4.1-bin.zip"),
        )
    }

    @Test
    fun `Detekt stays on the verified AGP 9 compatible plugin line`() {
        val versionsCatalog = rootDir.resolve("gradle/libs.versions.toml").readText()
        val detektVersion = versionsCatalog.versionFor("detekt")
        val composeRulesDetektVersion = versionsCatalog.versionFor("composeRulesDetekt")
        val detektPluginId = versionsCatalog.pluginIdFor("detekt")

        assertTrue(
            "Detekt version $detektVersion is older than the AGP 9.1.1 compatible 2.0.0-alpha.3 line",
            detektVersion == "2.0.0-alpha.3",
        )
        assertTrue(
            "compose-rules Detekt version $composeRulesDetektVersion is not on the Detekt 2 compatible 0.5.9 line",
            composeRulesDetektVersion == "0.5.9",
        )
        assertTrue(
            "Detekt Gradle plugin id $detektPluginId must use the Detekt 2 dev.detekt id",
            detektPluginId == "dev.detekt",
        )
    }

    private fun String.versionFor(alias: String): String {
        val pattern = Regex("""(?m)^$alias\s*=\s*"([^"]+)"""")
        return requireNotNull(pattern.find(this)?.groupValues?.get(1)) {
            "Missing $alias version in libs.versions.toml"
        }
    }

    private fun String.pluginIdFor(alias: String): String {
        val pattern = Regex("""(?m)^$alias\s*=\s*\{\s*id\s*=\s*"([^"]+)"""")
        return requireNotNull(pattern.find(this)?.groupValues?.get(1)) {
            "Missing $alias plugin id in libs.versions.toml"
        }
    }

    private fun String.isAtLeast(minimum: String): Boolean {
        val actualParts = split(".").map(String::toInt)
        val minimumParts = minimum.split(".").map(String::toInt)

        return actualParts
            .zip(minimumParts)
            .firstOrNull { (actual, expected) -> actual != expected }
            ?.let { (actual, expected) -> actual > expected }
            ?: (actualParts.size >= minimumParts.size)
    }

    private fun findRootDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .first { Files.exists(it.resolve("gradle/libs.versions.toml")) }
    }
}
