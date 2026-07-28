package com.runcheck.ui.battery

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.ThemeMode
import com.runcheck.ui.chart.BatteryHistoryMetric
import com.runcheck.ui.components.renderCompose
import com.runcheck.ui.theme.RuncheckTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatteryHistoryAccessRenderTest {
    @Test
    fun freeAndInsufficientHistoryRendersOnlyLockedState() {
        renderHistoryPanel(isPro = false).use { rendered ->
            val text = rendered.accessibilityText()

            assertTrue(text.any { it.contains(LOCKED_MESSAGE) })
            assertFalse(text.any { it.contains(INSUFFICIENT_MESSAGE) })
            rendered.capturePng("d11-free-locked-render.png")
        }
    }

    @Test
    fun trialOrProAndInsufficientHistoryRendersOnlyInsufficientState() {
        renderHistoryPanel(isPro = true).use { rendered ->
            val text = rendered.accessibilityText()

            assertTrue(text.any { it.contains(INSUFFICIENT_MESSAGE) })
            assertFalse(text.any { it.contains(LOCKED_MESSAGE) })
            rendered.capturePng("d11-trial-pro-insufficient-render.png")
        }
    }

    private fun renderHistoryPanel(isPro: Boolean) =
        renderCompose(widthPx = COMPACT_WIDTH, heightPx = PANEL_HEIGHT) {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 1f),
            ) {
                RuncheckTheme(themeMode = ThemeMode.DARK) {
                    BatteryHistoryPanel(
                        state =
                            BatteryUiState.Success(
                                batteryState = batteryState,
                                history = emptyList(),
                                isPro = isPro,
                            ),
                        selectedMetric = BatteryHistoryMetric.LEVEL,
                        onMetricChange = {},
                        onPeriodChange = {},
                        onUpgradeToPro = {},
                        onNavigateToFullscreen = { _, _, _ -> },
                    )
                }
            }
        }

    private val batteryState =
        BatteryState(
            level = 82,
            voltageMv = 4_050,
            temperatureC = 27f,
            currentMa = MeasuredValue(value = -250, confidence = Confidence.HIGH),
            chargingStatus = ChargingStatus.DISCHARGING,
            plugType = PlugType.NONE,
            health = BatteryHealth.GOOD,
            technology = "Li-ion",
        )

    private companion object {
        const val COMPACT_WIDTH = 411
        const val PANEL_HEIGHT = 560
        const val LOCKED_MESSAGE = "Week, month, and all-time battery history require runcheck Pro."
        const val INSUFFICIENT_MESSAGE = "Not enough data for this metric yet"
    }
}
