package com.runcheck.util

import com.runcheck.testutil.findAppDir
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class ComposeStabilityBaselineContractTest {
    private val appDir: Path = findAppDir()

    @Test
    fun `compose stability validation is strict and backed by a baseline`() {
        val buildFile = appDir.resolve("build.gradle.kts").readText()
        assertTrue(buildFile.contains("stabilityValidation"))
        assertTrue(buildFile.contains("failOnStabilityChange.set(true)"))
        assertTrue(buildFile.contains("allowMissingBaseline.set(false)"))

        val baselines =
            listOf(
                appDir.resolve("stability/app-debug.stability"),
                appDir.resolve("stability/app-release.stability"),
            )
        baselines.forEach { baseline ->
            assertTrue("Compose stability baseline is missing at $baseline", Files.exists(baseline))

            val baselineText = baseline.readText()
            assertTrue(baselineText.contains("@Composable"))
            assertTrue(baselineText.contains("com.runcheck."))
        }
    }
}
