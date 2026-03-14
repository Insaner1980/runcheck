package com.runcheck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.runcheck.R

private val ManropeFontFamily = FontFamily(
    Font(R.font.manrope)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono)
)

val MaterialTheme.numericFontFamily: FontFamily
    @Composable
    @ReadOnlyComposable
    get() = LocalNumericFontFamily.current

val RuncheckTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.04).em
    ),
    displayMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold
    ),
    displaySmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    titleMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em
    ),
    labelMedium = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelSmall = TextStyle(
        fontFamily = ManropeFontFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
)
