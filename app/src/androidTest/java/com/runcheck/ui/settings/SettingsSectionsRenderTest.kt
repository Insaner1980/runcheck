package com.runcheck.ui.settings

import androidx.compose.ui.platform.LocalContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.ThemeMode
import com.runcheck.domain.model.UserPreferences
import com.runcheck.ui.components.renderCompose
import com.runcheck.ui.theme.RuncheckTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsSectionsRenderTest {
    @Test
    fun appearanceThemeCallbackIsInvokedFromRenderedSelector() {
        var selectedTheme: ThemeMode? = null

        renderCompose(widthPx = COMPACT_WIDTH, heightPx = TALL_HEIGHT) {
            RuncheckTheme(themeMode = ThemeMode.DARK) {
                DisplaySection(
                    preferences = UserPreferences(),
                    onSetThemeMode = { selectedTheme = it },
                    onSetTemperatureUnit = {},
                    onSetShowInfoCards = {},
                )
            }
        }.use { rendered ->
            rendered.activateOwnText("Dark")
        }

        assertEquals(ThemeMode.DARK, selectedTheme)
    }

    @Test
    fun monitoringAndDataCallbacksAreInvokedFromRenderedRows() {
        var selectedInterval: MonitoringInterval? = null
        var exportClicks = 0

        renderCompose(widthPx = COMPACT_WIDTH, heightPx = TALL_HEIGHT) {
            RuncheckTheme(themeMode = ThemeMode.DARK) {
                MonitoringSection(
                    context = LocalContext.current,
                    monitoringInterval = MonitoringInterval.THIRTY,
                    isBatteryOptimizationExempt = true,
                    onSetMonitoringInterval = { selectedInterval = it },
                    onNavigateToMonitoringHelp = {},
                )
            }
        }.use { rendered ->
            rendered.activateOwnText("60 minutes")
        }

        renderCompose(widthPx = COMPACT_WIDTH, heightPx = TALL_HEIGHT) {
            RuncheckTheme(themeMode = ThemeMode.DARK) {
                DataSection(
                    uiState = SettingsUiState(),
                    onSetDataRetention = {},
                    onNavigateToExport = { exportClicks++ },
                    onResetTipsClick = {},
                    onClearSpeedTestsClick = {},
                    onClearAllDataClick = {},
                )
            }
        }.use { rendered ->
            rendered.activateOwnText("Export Data (CSV)")
        }

        assertEquals(MonitoringInterval.SIXTY, selectedInterval)
        assertEquals(1, exportClicks)
    }

    @Test
    fun proCallbackIsInvokedFromRenderedLockedWidgetsSection() {
        var proClicks = 0

        renderCompose(widthPx = COMPACT_WIDTH, heightPx = TALL_HEIGHT) {
            RuncheckTheme(themeMode = ThemeMode.DARK) {
                WidgetsSection(
                    isPro = false,
                    onNavigateToProUpgrade = { proClicks++ },
                )
            }
        }.use { rendered ->
            rendered.activateOwnText("Upgrade in Settings")
        }

        assertEquals(1, proClicks)
    }

    private companion object {
        const val COMPACT_WIDTH = 411
        const val TALL_HEIGHT = 1_600
    }
}
