package com.runcheck.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

class InsightRuleMultibindingContractTest {
    private val appDir: Path = findAppDir()
    private val insightsModule = appDir.resolve("src/main/java/com/runcheck/di/InsightsModule.kt")
    private val insightEngine = appDir.resolve("src/main/java/com/runcheck/domain/insights/engine/InsightEngine.kt")

    @Test
    fun `every production insight rule is bound into the Hilt set exactly once`() {
        val implementations = insightRuleImplementations()
        val bindings = insightRuleBindings()

        assertEquals("Duplicate InsightRule implementations", emptyList<String>(), implementations.duplicates())
        assertEquals("Duplicate InsightRule Hilt bindings", emptyList<String>(), bindings.duplicates())
        assertEquals(implementations.sorted(), bindings.sorted())
    }

    @Test
    fun `insight rule ids are unique`() {
        val rules = insightRuleSources()
        val missingRuleIds = rules.filter { it.ruleId == null }.map { it.className }
        val duplicateRuleIds =
            rules
                .mapNotNull { source -> source.ruleId?.let { ruleId -> source.copy(ruleId = ruleId) } }
                .groupBy { it.ruleId }
                .filterValues { it.size > 1 }
                .map { (ruleId, rules) -> "$ruleId: ${rules.joinToString { it.className }}" }

        assertEquals("InsightRule implementations missing RULE_ID", emptyList<String>(), missingRuleIds)
        assertEquals(emptyList<String>(), duplicateRuleIds)
    }

    @Test
    fun `multibound insight rule set has no order sensitive consumers`() {
        val consumers =
            productionKotlinFiles()
                .filter { path ->
                    val text = path.readText()
                    text.contains("Set<@JvmSuppressWildcards InsightRule>") ||
                        text.contains("Set<InsightRule>")
                }.map { appDir.relativize(it).toString().replace('\\', '/') }
                .sorted()

        assertEquals(
            listOf("src/main/java/com/runcheck/domain/insights/engine/InsightEngine.kt"),
            consumers,
        )

        val engineText = insightEngine.readText()
        val forbiddenOrderOperations =
            listOf(
                "rules.first",
                "rules.last",
                "rules.elementAt",
                "rules.toList",
                "rules.forEachIndexed",
                "rules.withIndex",
                "rules.sorted",
                "rules.take",
                "rules.drop",
                "rules.indexOf",
            ).filter { engineText.contains(it) }

        assertEquals(emptyList<String>(), forbiddenOrderOperations)
        assertTrue(engineText.contains("rules.associate { rule ->"))
    }

    private fun insightRuleImplementations(): List<String> = insightRuleSources().map { it.className }

    private fun insightRuleBindings(): List<String> =
        insightRuleBindingPattern
            .findAll(insightsModule.readText())
            .map { it.groupValues[1] }
            .toList()

    private fun insightRuleSources(): List<InsightRuleSource> =
        productionKotlinFiles().flatMap { path ->
            val text = path.readText()
            insightRuleClassPattern
                .findAll(text)
                .map { match ->
                    InsightRuleSource(
                        className = match.groupValues[1],
                        ruleId = ruleIdPattern.find(text)?.groupValues?.get(1),
                    )
                }.toList()
        }

    private fun productionKotlinFiles(): List<Path> =
        Files
            .walk(appDir.resolve("src/main/java"))
            .use { paths ->
                paths
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .toList()
            }

    private fun List<String>.duplicates(): List<String> =
        groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .map { (name, count) -> "$name x$count" }

    private fun findAppDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .flatMap { path -> sequenceOf(path, path.resolve("app")) }
            .first { Files.exists(it.resolve("src/main/res")) && Files.exists(it.resolve("build.gradle.kts")) }
    }

    private data class InsightRuleSource(
        val className: String,
        val ruleId: String?,
    )

    private companion object {
        val insightRuleClassPattern =
            Regex("""\b(?:class|object)\s+(\w+)\b(?:[^({]*:\s*InsightRule\b|[^{]*\)\s*:\s*InsightRule\b)""")
        val insightRuleBindingPattern = Regex("""fun\s+bind\w+\(rule:\s*(\w+)\):\s*InsightRule""")
        val ruleIdPattern = Regex("const\\s+val\\s+RULE_ID\\s*=\\s*\"([^\"]+)\"")
    }
}
