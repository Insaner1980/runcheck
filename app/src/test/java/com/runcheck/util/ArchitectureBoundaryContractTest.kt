package com.runcheck.util

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readLines

class ArchitectureBoundaryContractTest {
    private val rootDir: Path = findRootDir()
    private val sourceRoot: Path = rootDir.resolve("app/src/main/java/com/runcheck")

    @Test
    fun `ui source does not reference data layer packages`() {
        val violations =
            sourceRoot
                .resolve("ui")
                .kotlinFiles()
                .flatMap { file ->
                    file.findViolations { line ->
                        line.trim().startsWith("import com.runcheck.data.") ||
                            line.contains("com.runcheck.data.")
                    }
                }

        assertTrue(violations.message("UI source must not depend directly on data layer code"), violations.isEmpty())
    }

    @Test
    fun `domain source avoids Android imports except PagingData boundary`() {
        val violations =
            sourceRoot
                .resolve("domain")
                .kotlinFiles()
                .flatMap { file -> file.findViolations(::isForbiddenDomainBoundaryReference) }

        assertTrue(violations.message("Domain source must stay Android-free except PagingData"), violations.isEmpty())
    }

    @Test
    fun `domain boundary check flags fully qualified Android references`() {
        val violations =
            listOf(
                "val handle: androidx.lifecycle.SavedStateHandle? = null",
                "val context: android.content.Context? = null",
                "val entity = com.runcheck.data.storage.MediaEntity()",
            ).filter(::isForbiddenDomainBoundaryReference)

        assertTrue(
            violations.joinToString(prefix = "Expected all boundary leaks to be flagged: "),
            violations.size == 3,
        )
    }

    private fun isForbiddenDomainBoundaryReference(line: String): Boolean {
        val withoutAllowedPaging = allowedPagingDataReference.replace(line, "")
        return forbiddenDomainBoundaryReference.containsMatchIn(withoutAllowedPaging)
    }

    private val allowedPagingDataReference = Regex("""\bandroidx\.paging\.PagingData\b""")
    private val forbiddenDomainBoundaryReference = Regex("""\b(?:android|androidx|com\.runcheck\.data)\.""")

    private fun Path.kotlinFiles(): List<Path> =
        Files
            .walk(this)
            .use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .toList()
            }

    private fun Path.findViolations(predicate: (String) -> Boolean): List<BoundaryViolation> =
        readLines().mapIndexedNotNull { index, line ->
            if (predicate(line)) {
                BoundaryViolation(file = this, lineNumber = index + 1, line = line.trim())
            } else {
                null
            }
        }

    private fun List<BoundaryViolation>.message(reason: String): String =
        joinToString(
            separator = System.lineSeparator(),
            prefix = "$reason:${System.lineSeparator()}",
        ) { violation ->
            val relativePath = rootDir.relativize(violation.file).toString().replace('\\', '/')
            "$relativePath:${violation.lineNumber}: ${violation.line}"
        }

    private data class BoundaryViolation(
        val file: Path,
        val lineNumber: Int,
        val line: String,
    )

    private fun findRootDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .first { Files.exists(it.resolve("gradle/libs.versions.toml")) }
    }
}
