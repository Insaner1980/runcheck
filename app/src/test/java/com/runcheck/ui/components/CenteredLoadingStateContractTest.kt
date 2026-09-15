package com.runcheck.ui.components

import com.runcheck.testutil.findRootDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.readText

class CenteredLoadingStateContractTest {
    private val uiRoot = findRootDir().resolve("app/src/main/java/com/runcheck/ui")

    @Test
    fun `shared loading area exposes one description and a polite live region`() {
        val source = source("components/CenteredLoadingState.kt")

        assertTrue(
            source.contains(
                "modifier .fillMaxSize() .semantics { contentDescription = description liveRegion = LiveRegionMode.Polite }",
            ),
        )
        assertTrue(source.contains("contentAlignment = Alignment.Center"))
        assertEquals(1, Regex("contentDescription =").findAll(source).count())
        assertTrue(source.contains("CircularProgressIndicator()"))
        assertFalse(source.contains("Text("))
    }

    @Test
    fun `cleanup idle and scanning retain the scan description while deleting retains its own`() {
        val source = source("storage/cleanup/CleanupScreen.kt")

        assertTrue(source.contains("val scanningDescription = stringResource(R.string.a11y_scanning_files)"))
        assertTrue(source.contains("val deletingDescription = stringResource(R.string.a11y_deleting_files)"))
        assertEquals(
            "{ CenteredLoadingState(description = scanningDescription) }",
            source.substringAfter("is CleanupUiState.Scanning, ->").substringBefore("is CleanupUiState.Empty").trim(),
        )
        assertEquals(
            "{ CenteredLoadingState(description = deletingDescription) }",
            source.substringAfter("is CleanupUiState.Deleting ->").substringBefore("is CleanupUiState.Success").trim(),
        )
        assertTrue(source.contains("is CleanupUiState.Idle, is CleanupUiState.Scanning,"))
        // The paging footer remains independent of full-area loading.
        assertEquals(1, Regex("CircularProgressIndicator\\(").findAll(source).count())
    }

    @Test
    fun `insights loading keeps its content container and uses the existing loading description`() {
        val branch =
            source("insights/InsightsScreen.kt")
                .substringAfter("InsightsUiState.Loading ->")
                .substringBefore("is InsightsUiState.Error")
                .trim()

        assertEquals(
            "{ ContentContainer(modifier = Modifier.fillMaxSize()) { " +
                "CenteredLoadingState(description = stringResource(R.string.a11y_loading)) } }",
            branch,
        )
    }

    @Test
    fun `fullscreen loading preserves the supplied chart area and loading description`() {
        val source = source("fullscreen/FullscreenChartScreen.kt")
        val branch =
            source
                .substringAfter("is FullscreenChartUiState.Loading ->")
                .substringBefore("FullscreenChartUiState.Locked ->")
                .trim()

        assertEquals(
            "{ CenteredLoadingState( description = stringResource(R.string.a11y_loading), modifier = contentModifier, ) }",
            branch,
        )
        assertTrue(
            source.contains(
                "content( Modifier .fillMaxSize() .padding(innerPadding) " +
                    ".padding(horizontal = MaterialTheme.spacing.sm) .padding(bottom = MaterialTheme.spacing.sm), )",
            ),
        )
    }

    private fun source(path: String): String = uiRoot.resolve(path).readText().replace(Regex("\\s+"), " ")
}
