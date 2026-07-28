package com.runcheck.ui.network

import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runcheck.domain.model.ThemeMode
import com.runcheck.ui.components.renderCompose
import com.runcheck.ui.theme.RuncheckTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpeedTestAccessibilityRenderTest {
    @Test
    fun liveSpeedMetricsDoNotCreateRepeatedAccessibilityAnnouncements() {
        renderCompose(widthPx = COMPACT_WIDTH, heightPx = METRICS_HEIGHT) {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 1f),
            ) {
                RuncheckTheme(themeMode = ThemeMode.DARK) {
                    SpeedMetricsCard(
                        state =
                            SpeedTestUiState(
                                phase = SpeedTestPhase.Download,
                                isRunning = true,
                                downloadMbps = 42.5,
                                uploadMbps = 8.4,
                                pingMs = 31,
                                jitterMs = 4,
                            ),
                    )
                }
            }
        }.use { rendered ->
            listOf("Download", "Upload", "Ping", "Jitter").forEach { label ->
                val metricNodes = rendered.nodesContainingText(label)
                assertTrue("Expected rendered semantics for $label", metricNodes.isNotEmpty())
                assertTrue(
                    "$label must not be an accessibility live region",
                    metricNodes.all { node -> node.liveRegion == View.ACCESSIBILITY_LIVE_REGION_NONE },
                )
            }
        }
    }

    private companion object {
        const val COMPACT_WIDTH = 411
        const val METRICS_HEIGHT = 420
    }
}
