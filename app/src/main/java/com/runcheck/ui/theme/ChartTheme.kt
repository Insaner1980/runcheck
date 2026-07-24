package com.runcheck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ChartColors(
    val grid: Color,
    val axis: Color,
    val line: Color,
    val fill: Color,
    val selectedPoint: Color,
    val glow: Color,
)

internal val DarkChartColors =
    ChartColors(
        grid = DarkRuncheckColorScheme.outlineVariant,
        axis = DarkRuncheckColorScheme.onSurfaceVariant,
        line = DarkRuncheckColorScheme.primary,
        fill = DarkRuncheckColorScheme.surfaceContainerHigh,
        selectedPoint = DarkRuncheckColorScheme.secondary,
        glow = DarkRuncheckColorScheme.secondary,
    )

internal val LightChartColors =
    ChartColors(
        grid = LightRuncheckColorScheme.outlineVariant,
        axis = LightRuncheckColorScheme.onSurfaceVariant,
        line = LightRuncheckColorScheme.primary,
        fill = LightRuncheckColorScheme.surfaceContainer,
        selectedPoint = LightRuncheckColorScheme.secondary,
        glow = LightRuncheckColorScheme.surfaceContainerHighest,
    )

internal val LocalChartColors = staticCompositionLocalOf { DarkChartColors }

val MaterialTheme.chartColors: ChartColors
    @Composable
    @ReadOnlyComposable
    get() = LocalChartColors.current
