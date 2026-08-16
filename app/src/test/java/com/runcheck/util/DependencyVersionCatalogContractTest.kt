package com.runcheck.util

import com.runcheck.testutil.findRootDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
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
    fun `runtime and test dependencies stay on the August 2026 verified targets`() {
        val versionsCatalog = rootDir.resolve("gradle/libs.versions.toml").readText()
        val expectedVersions =
            mapOf(
                "paging" to "3.5.0",
                "activityCompose" to "1.13.0",
                "billing" to "9.1.0",
                "okhttp" to "5.4.0",
                "gson" to "2.14.0",
                "mockk" to "1.14.11",
                "dependencyAnalysis" to "3.17.0",
                "sentry" to "8.51.0",
            )

        expectedVersions.forEach { (alias, expected) ->
            assertEquals("Unexpected $alias version", expected, versionsCatalog.versionFor(alias))
        }
    }

    @Test
    fun `core toolchain stays on the documented stability analyzer compatibility exception`() {
        val versionsCatalog = rootDir.resolve("gradle/libs.versions.toml").readText()
        val expectedVersions =
            mapOf(
                "agp" to "9.2.1",
                "kotlin" to "2.4.10",
                "kotlinRuntime" to "2.3.20",
                "ksp" to "2.3.11",
                "hilt" to "2.60.1",
                "detekt" to "2.0.0-alpha.5",
                "stabilityAnalyzer" to "0.12.0",
            )

        expectedVersions.forEach { (alias, expected) ->
            assertEquals(
                "Unexpected compatibility-exception version for $alias",
                expected,
                versionsCatalog.versionFor(alias),
            )
        }
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
            "Gradle wrapper must stay on the verified 9.7.0 binary distribution",
            wrapperProperties.contains("gradle-9.7.0-bin.zip"),
        )
        assertTrue(
            "Gradle wrapper must verify the official 9.7.0 binary distribution checksum",
            wrapperProperties.contains(
                "distributionSha256Sum=84fbba45c7f4c64abc77460e1c00f541e9f960e3c7ed2538f1ede19eacd873ae",
            ),
        )
    }

    @Test
    fun `Detekt stays on the verified AGP 9 compatible plugin line`() {
        val versionsCatalog = rootDir.resolve("gradle/libs.versions.toml").readText()
        val detektVersion = versionsCatalog.versionFor("detekt")
        val composeRulesDetektVersion = versionsCatalog.versionFor("composeRulesDetekt")
        val detektPluginId = versionsCatalog.pluginIdFor("detekt")

        assertTrue(
            "Detekt version $detektVersion is not the verified 2.0.0-alpha.5 release",
            detektVersion == "2.0.0-alpha.5",
        )
        assertTrue(
            "compose-rules Detekt version $composeRulesDetektVersion is not on the Detekt 2 compatible 0.6.4 line",
            composeRulesDetektVersion == "0.6.4",
        )
        assertTrue(
            "Detekt Gradle plugin id $detektPluginId must use the Detekt 2 dev.detekt id",
            detektPluginId == "dev.detekt",
        )
    }

    @Test
    fun `version catalog rejects unapproved prerelease versions`() {
        val versionsCatalog = rootDir.resolve("gradle/libs.versions.toml").readText()
        val versionEntries = versionsCatalog.versionEntries()
        val prereleaseMarker =
            Regex(
                "(?:^|[._-])(alpha|beta|rc|preview|eap|milestone|dev|snapshot)(?:[._-]|\\d|$)",
                RegexOption.IGNORE_CASE,
            )
        val unexpectedPrereleases =
            versionEntries.filter { (alias, version) ->
                prereleaseMarker.containsMatchIn(version) &&
                    !(alias == "detekt" && version == "2.0.0-alpha.5")
            }

        assertTrue("Unapproved prerelease versions: $unexpectedPrereleases", unexpectedPrereleases.isEmpty())
        assertFalse(
            "The NDT7 commit hash is not a semantic prerelease version",
            prereleaseMarker.containsMatchIn(versionEntries.getValue("ndt7")),
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

    private fun String.versionEntries(): Map<String, String> {
        val versionsSection = substringAfter("[versions]").substringBefore("[libraries]")
        return Regex("(?m)^([A-Za-z][A-Za-z0-9]*)\\s*=\\s*\"([^\"]+)\"")
            .findAll(versionsSection)
            .associate { match -> match.groupValues[1] to match.groupValues[2] }
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
}
