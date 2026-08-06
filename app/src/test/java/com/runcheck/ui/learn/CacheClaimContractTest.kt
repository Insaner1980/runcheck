package com.runcheck.ui.learn

import com.runcheck.util.readContractText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CacheClaimContractTest {
    private val appDir = findAppDir()

    @Test
    fun `cache guidance states the read only boundary and links to Android settings`() {
        val strings = appDir.resolve("src/main/res/values/strings.xml").readContractText()
        val storageScreen =
            appDir.resolve("src/main/java/com/runcheck/ui/storage/StorageDetailScreen.kt").readContractText()
        val storageSupport =
            appDir.resolve("src/main/java/com/runcheck/ui/storage/StorageDetailSupport.kt").readContractText()

        assertTrue(strings.contains("cannot clear other apps’ caches"))
        assertTrue(strings.contains("""name="learn_storage_cache_limits_title""""))
        assertTrue(strings.contains("""name="storage_cache_learn_more""""))
        assertTrue(storageScreen.contains("LearnArticleIds.STORAGE_CACHE_LIMITS"))
        assertTrue(storageSupport.contains("Settings.ACTION_INTERNAL_STORAGE_SETTINGS"))
        assertTrue(storageSupport.contains("Settings.ACTION_APPLICATION_DETAILS_SETTINGS"))
    }

    @Test
    fun `learn catalog exposes the cache limitations article`() {
        assertTrue(LearnArticleCatalog.containsId(LearnArticleIds.STORAGE_CACHE_LIMITS))
        assertTrue(
            LearnArticleCatalog
                .articlesForTopic(LearnTopic.STORAGE)
                .any { article -> article.id == LearnArticleIds.STORAGE_CACHE_LIMITS },
        )
    }

    @Test
    fun `storage guidance distinguishes cache measurement settings and cleanup categories`() {
        val strings = appDir.resolve("src/main/res/values/strings.xml").readContractText()
        val slowdownBody =
            Regex(
                """<string name="learn_storage_slowdown_body"[^>]*>(.*?)</string>""",
                setOf(RegexOption.DOT_MATCHES_ALL),
            ).find(strings)?.groupValues?.get(1).orEmpty()

        assertTrue(slowdownBody.contains("read-only cache measurement"))
        assertTrue(slowdownBody.contains("Android Settings"))
        assertTrue(slowdownBody.contains("large files, old downloads, leftover APK installers, and user-visible media"))
        assertTrue(slowdownBody.contains("does not clear app caches"))
        assertFalse(slowdownBody.contains("surface those categories"))
    }

    @Test
    fun `user facing copy does not claim runcheck clears cache ram or speeds up the phone`() {
        val userFacingCopy =
            buildString {
                append(appDir.resolve("src/main/res/values/strings.xml").readContractText())
                append(appDir.parent.resolve("docs/play-store-listing.md").readContractText())
            }.lowercase()
        val forbiddenClaims =
            listOf(
                "runcheck clears cache",
                "runcheck clears other apps",
                "ram booster",
                "cache cleaner",
                "speed up your phone",
            )

        forbiddenClaims.forEach { claim ->
            assertFalse("Found prohibited claim: $claim", userFacingCopy.contains(claim))
        }
    }

    @Test
    fun `store listing states the cache boundary and all current widgets`() {
        val listing = appDir.parent.resolve("docs/play-store-listing.md").readContractText()

        assertTrue(listing.contains("cannot clear other apps' caches"))
        assertTrue(listing.contains("Quick Glance"))
    }

    private fun findAppDir(): Path {
        val start = Paths.get("").toAbsolutePath()
        return generateSequence(start) { it.parent }
            .flatMap { path -> sequenceOf(path, path.resolve("app")) }
            .first { Files.exists(it.resolve("src/main/res")) && Files.exists(it.resolve("build.gradle.kts")) }
    }
}
