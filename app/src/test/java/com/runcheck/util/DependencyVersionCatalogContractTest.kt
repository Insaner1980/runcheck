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

    private fun String.versionFor(alias: String): String {
        val pattern = Regex("""(?m)^$alias\s*=\s*"([^"]+)"""")
        return requireNotNull(pattern.find(this)?.groupValues?.get(1)) {
            "Missing $alias version in libs.versions.toml"
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
