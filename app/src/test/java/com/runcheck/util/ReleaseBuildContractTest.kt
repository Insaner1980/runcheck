package com.runcheck.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class ReleaseBuildContractTest {
    private val appDir: Path = findAppDir()

    @Test
    fun `release artifact tasks require a published version code floor`() {
        val buildFile = appDir.resolve("build.gradle.kts").readText()

        assertTrue(buildFile.contains("val currentReleaseVersionCode ="))
        assertTrue(buildFile.contains("versionCode = currentReleaseVersionCode"))
        assertTrue(
            buildFile.contains("""val releaseVersionCodeFloorPropertyName = "runcheck.releaseVersionCodeFloor""""),
        )
        assertTrue(buildFile.contains("""val releaseVersionCodeFloorEnvName = "RUNCHECK_RELEASE_VERSION_CODE_FLOOR""""))
        assertTrue(buildFile.contains("currentReleaseVersionCode <= releaseVersionCodeFloor"))
    }

    @Test
    fun `release upload artifacts include version and version code in copied filenames`() {
        val buildFile = appDir.resolve("build.gradle.kts").readText()

        assertTrue(buildFile.contains("""tasks.register<Copy>("copyReleaseArtifacts")"""))
        assertTrue(
            buildFile.contains(
                """runcheck-${'$'}currentReleaseVersionName-code${'$'}currentReleaseVersionCode-release""",
            ),
        )
        assertTrue(buildFile.contains("""outputs/release-upload"""))
    }

    private fun findAppDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .flatMap { path -> sequenceOf(path, path.resolve("app")) }
            .first { Files.exists(it.resolve("src/main/res")) && Files.exists(it.resolve("build.gradle.kts")) }
    }
}
