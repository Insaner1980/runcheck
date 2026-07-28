package com.runcheck.ui.export

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runcheck.domain.model.ThemeMode
import com.runcheck.pro.ProState
import com.runcheck.pro.ProStatus
import com.runcheck.ui.components.renderCompose
import com.runcheck.ui.pro.ProUpgradeContent
import com.runcheck.ui.pro.ProUpgradeUiState
import com.runcheck.ui.theme.RuncheckTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportProRenderTest {
    @Test
    fun exportContentKeepsTheExistingSharePreparationAction() {
        var exportClicks = 0

        renderCompose(widthPx = COMPACT_WIDTH, heightPx = CONTENT_HEIGHT) {
            RuncheckTheme(themeMode = ThemeMode.DARK) {
                ExportContent(
                    state = ExportUiState(),
                    onExport = { exportClicks++ },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }.use { rendered ->
            rendered.click("Create and share export")
        }

        assertEquals(1, exportClicks)
    }

    @Test
    fun trialUpgradeContentKeepsTrialStatusAndOneTimePurchaseActionVisible() {
        var purchaseClicks = 0

        renderCompose(widthPx = COMPACT_WIDTH, heightPx = TALL_HEIGHT) {
            RuncheckTheme(themeMode = ThemeMode.DARK) {
                ProUpgradeContent(
                    uiState =
                        ProUpgradeUiState(
                            proState =
                                ProState(
                                    status = ProStatus.TRIAL_ACTIVE,
                                    trialDaysRemaining = 3,
                                ),
                            formattedPrice = "€4.99",
                            billingAvailable = true,
                        ),
                    onPurchase = { purchaseClicks++ },
                )
            }
        }.use { rendered ->
            val text = rendered.accessibilityText()
            assertTrue(text.any { it.contains("Pro Trial — 3 days remaining") })
            assertTrue(text.any { it.contains("One-time purchase") })
            rendered.click("Unlock Pro — €4.99")
        }

        assertEquals(1, purchaseClicks)
    }

    private companion object {
        const val COMPACT_WIDTH = 411
        const val CONTENT_HEIGHT = 700
        const val TALL_HEIGHT = 1_500
    }
}
