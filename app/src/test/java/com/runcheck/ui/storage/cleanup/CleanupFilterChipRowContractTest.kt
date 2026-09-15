package com.runcheck.ui.storage.cleanup

import com.runcheck.R
import com.runcheck.testutil.findRootDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.readText

class CleanupFilterChipRowContractTest {
    private val uiRoot = findRootDir().resolve("app/src/main/java/com/runcheck/ui")

    @Test
    fun `cleanup delegates indices labels and callback only when options exist with feature owned spacing`() {
        val source = source("storage/cleanup/CleanupScreen.kt")

        assertEquals(
            "if (cleanupType.filterOptions.isNotEmpty()) { " +
                "Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm)) " +
                "EnumFilterChipRow( " +
                "values = cleanupType.filterOptions.indices.toList(), " +
                "selected = selectedFilterIndex, " +
                "onSelect = onFilterSelect, " +
                "labelFor = { index -> stringResource(cleanupType.filterOptions[index].labelRes) }, ) " +
                "Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm)) }",
            source.substringAfter("// Filter chips ").substringBefore("when (val state = uiState)").trim(),
        )
        assertTrue(source.contains("selectedFilterIndex = viewModel.getSelectedFilterIndex()"))
        assertTrue(source.contains("val selectedFilterIndex = state.selectedFilterIndex"))
        assertTrue(source.contains("onFilterSelect = viewModel::setFilter"))
        assertTrue(source.contains("val onFilterSelect = actions.onFilterSelect"))
        assertFalse(Regex("\\bFilterChip\\(").containsMatchIn(source))
        assertFalse(source.contains("horizontalScroll"))
    }

    @Test
    fun `shared row preserves scrolling spacing and exact single selection callback`() {
        val source = source("common/ChartSelection.kt").substringBefore("fun ApplyFullscreenChartSelectionResult")

        assertTrue(source.contains("modifier .fillMaxWidth() .horizontalScroll(rememberScrollState())"))
        assertTrue(source.contains("horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)"))
        assertTrue(
            source.contains(
                "values.forEach { value -> FilterChip( selected = selected == value, " +
                    "onClick = { onSelect(value) }, label = { Text(labelFor(value)) }, ) }",
            ),
        )
        assertFalse(source.contains("semantics"))
    }

    @Test
    fun `cleanup label order and absent APK options remain unchanged`() {
        assertEquals(
            listOf(
                R.string.cleanup_filter_10mb,
                R.string.cleanup_filter_50mb,
                R.string.cleanup_filter_100mb,
                R.string.cleanup_filter_500mb,
            ),
            CleanupType.LARGE_FILES.filterOptions.map { it.labelRes },
        )
        assertEquals(
            listOf(
                R.string.cleanup_filter_30d,
                R.string.cleanup_filter_60d,
                R.string.cleanup_filter_90d,
                R.string.cleanup_filter_1y,
            ),
            CleanupType.OLD_DOWNLOADS.filterOptions.map { it.labelRes },
        )
        assertTrue(CleanupType.APK_FILES.filterOptions.isEmpty())
    }

    private fun source(path: String): String = uiRoot.resolve(path).readText().replace(Regex("\\s+"), " ")
}
