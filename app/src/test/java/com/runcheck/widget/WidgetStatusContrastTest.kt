package com.runcheck.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import com.runcheck.R
import com.runcheck.ui.theme.contrastRatio
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetStatusContrastTest {
    @Test
    fun `every widget status color has normal text contrast in day and night themes`() {
        RuncheckWidgetStatusPalette.all.forEach { tone ->
            assertTrue(
                "${tone.name} day contrast",
                contrastRatio(
                    resolvedColor(tone.colorRes, night = false),
                    resolvedColor(R.color.widget_background, night = false),
                ) >= 4.5,
            )
            assertTrue(
                "${tone.name} night contrast",
                contrastRatio(
                    resolvedColor(tone.colorRes, night = true),
                    resolvedColor(R.color.widget_background, night = true),
                ) >= 4.5,
            )
        }
    }

    private fun resolvedColor(
        colorRes: Int,
        night: Boolean,
    ): Color {
        val baseContext = RuntimeEnvironment.getApplication() as Context
        val configuration = Configuration(baseContext.resources.configuration)
        val nightMode =
            if (night) {
                Configuration.UI_MODE_NIGHT_YES
            } else {
                Configuration.UI_MODE_NIGHT_NO
            }
        configuration.uiMode =
            configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or nightMode
        return Color(baseContext.createConfigurationContext(configuration).getColor(colorRes))
    }
}
