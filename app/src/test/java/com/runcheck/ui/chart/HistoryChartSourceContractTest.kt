package com.runcheck.ui.chart

import com.runcheck.util.readContractText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HistoryChartSourceContractTest {
    @Test
    fun `period selector uses a touch target minimum without capping large text height`() {
        val source =
            File("src/main/java/com/runcheck/ui/chart/HistoryPeriodFilterChipRow.kt")
                .readContractText()

        assertTrue(source.contains(".heightIn(min = MaterialTheme.uiTokens.touchTarget)"))
        assertFalse(source.contains(".height(MaterialTheme.uiTokens.touchTarget)"))
        assertTrue(source.contains("modifier = Modifier.matchParentSize()"))
    }

    @Test
    fun `temperature history and throttling logs use distinct pro messages`() {
        val source =
            File("src/main/java/com/runcheck/ui/thermal/ThermalDetailScreen.kt")
                .readContractText()
        val strings = File("src/main/res/values/strings.xml").readContractText()

        assertTrue(
            strings.contains(
                "<string name=\"pro_feature_thermal_history_message\">" +
                    "Temperature history requires runcheck Pro.</string>",
            ),
        )
        assertEquals(1, source.windowedCount("R.string.pro_feature_thermal_history_message"))
        assertEquals(1, source.windowedCount("R.string.pro_feature_thermal_log_message"))
    }

    private fun String.windowedCount(needle: String): Int = windowed(size = needle.length).count { it == needle }
}
