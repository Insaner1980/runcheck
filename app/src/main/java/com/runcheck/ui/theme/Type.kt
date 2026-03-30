package com.runcheck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.runcheck.R

private val ManropeFontFamily =
    FontFamily(
        Font(R.font.manrope),
    )

val JetBrainsMonoFontFamily =
    FontFamily(
        Font(R.font.jetbrains_mono),
    )

val MaterialTheme.numericFontFamily: FontFamily
    @Composable
    @ReadOnlyComposable
    get() = LocalNumericFontFamily.current

val MaterialTheme.numericHeroDisplayTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() =
        typography.displayLarge.copy(
            fontFamily = numericFontFamily,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 64.sp,
            letterSpacing = (-3).sp,
        )

val MaterialTheme.numericHeroDisplayUnitTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() =
        typography.headlineLarge.copy(
            fontFamily = numericFontFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 28.sp,
        )

val MaterialTheme.numericHeroValueTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() =
        typography.displayLarge.copy(
            fontFamily = numericFontFamily,
            fontWeight = FontWeight.Bold,
            lineHeight = 48.sp,
        )

val MaterialTheme.numericHeroLevelTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() = numericHeroValueTextStyle.copy(letterSpacing = (-2).sp)

val MaterialTheme.numericHeroLargeValueTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() =
        typography.displayLarge.copy(
            fontFamily = numericFontFamily,
            fontSize = 54.sp,
        )

val MaterialTheme.numericHeroUnitTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() =
        typography.headlineLarge.copy(
            fontFamily = numericFontFamily,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 20.sp,
        )

val MaterialTheme.numericRingValueTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() =
        typography.displayMedium.copy(
            fontFamily = numericFontFamily,
            fontSize = 32.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
        )

val MaterialTheme.numericSpeedHeroValueTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() =
        typography.displaySmall.copy(
            fontFamily = numericFontFamily,
            fontSize = 40.sp,
            lineHeight = 44.sp,
        )

val MaterialTheme.numericMetricDisplayTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() =
        typography.displayLarge.copy(
            fontFamily = numericFontFamily,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 48.sp,
            letterSpacing = (-3).sp,
        )

val MaterialTheme.chartAxisTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() =
        typography.labelSmall.copy(
            fontFamily = numericFontFamily,
            fontSize = 12.sp,
            lineHeight = 14.sp,
        )

val MaterialTheme.chartTooltipTextStyle: TextStyle
    @Composable
    @ReadOnlyComposable
    get() =
        typography.bodySmall.copy(
            fontFamily = numericFontFamily,
            fontSize = 13.sp,
            lineHeight = 16.sp,
        )

val RuncheckTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.04).em,
            ),
        displayMedium =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            ),
        displaySmall =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        titleLarge =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
            ),
        titleMedium =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
        titleSmall =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodySmall =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
            ),
        labelLarge =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.08.em,
            ),
        labelMedium =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        labelSmall =
            TextStyle(
                fontFamily = ManropeFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            ),
    )
