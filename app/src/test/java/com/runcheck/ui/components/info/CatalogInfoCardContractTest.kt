package com.runcheck.ui.components.info

import com.runcheck.testutil.findRootDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.readText

class CatalogInfoCardContractTest {
    private val uiRoot = findRootDir().resolve("app/src/main/java/com/runcheck/ui")

    @Test
    fun `adapter delegates content visibility and callbacks without removing the animated card`() {
        val source = uiRoot.resolve("components/info/CatalogInfoCard.kt").readText()
        val body = source.substringAfter(") {").replace(Regex("\\s+"), " ").trim()

        // Keep InfoCard composed when hidden so its existing exit animation can finish.
        assertEquals(
            "InfoCard( id = definition.id, headline = stringResource(definition.headlineRes), " +
                "body = stringResource(definition.bodyRes), onDismiss = onDismiss, modifier = modifier, " +
                "visible = definition.id !in dismissedInfoCards && showInfoCards, " +
                "onLearnMore = definition.learnArticleId?.let { " +
                "{ InfoCardCatalog.resolveLearnArticleId(definition)?.let(onNavigateToLearnArticle) } }, ) }",
            body,
        )
    }

    @Test
    fun `every catalog card is adapted once by its owning screen with shared state and callbacks`() {
        val screens =
            mapOf(
                InfoCardScreen.BATTERY_DETAIL to "battery/BatteryDetailScreen.kt",
                InfoCardScreen.NETWORK_DETAIL to "network/NetworkDetailScreen.kt",
                InfoCardScreen.THERMAL_DETAIL to "thermal/ThermalDetailScreen.kt",
                InfoCardScreen.STORAGE_DETAIL to "storage/StorageDetailScreen.kt",
            )
        val callPattern = Regex("CatalogInfoCard\\((.*?)\\)", RegexOption.DOT_MATCHES_ALL)
        val definitionNames =
            Regex("val (\\w+) =\\s*DismissibleInfoCardDefinition")
                .findAll(uiRoot.resolve("components/info/InfoCardCatalog.kt").readText())
                .map { it.groupValues[1] }
                .toSet()
        val usedNames = mutableListOf<String>()

        screens.forEach { (screen, path) ->
            val source = uiRoot.resolve(path).readText()
            val calls = callPattern.findAll(source).toList()
            assertEquals(InfoCardCatalog.all.count { it.screen == screen }, calls.size)
            assertFalse(Regex("\\bInfoCard\\(").containsMatchIn(source))
            calls.forEach { call ->
                val args = call.groupValues[1].replace(Regex("\\s+"), " ").trim()
                val name = args.substringAfter("definition = InfoCardCatalog.").substringBefore(',')
                usedNames.add(name)
                assertEquals(
                    "definition = InfoCardCatalog.$name, dismissedInfoCards = state.dismissedInfoCards, " +
                        "showInfoCards = state.showInfoCards, onDismiss = onDismissInfoCard, " +
                        "onNavigateToLearnArticle = onNavigateToLearnArticle,",
                    args,
                )
            }
        }
        assertEquals(definitionNames, usedNames.toSet())
        assertEquals(definitionNames.size, usedNames.size)
    }

    @Test
    fun `article resolution preserves destinations including the card without a Learn action`() {
        InfoCardCatalog.all.forEach { definition ->
            assertEquals(definition.learnArticleId, InfoCardCatalog.resolveLearnArticleId(definition))
        }
        assertTrue(InfoCardCatalog.BatteryLiveNotification.learnArticleId == null)
    }
}
