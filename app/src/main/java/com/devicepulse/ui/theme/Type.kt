package com.devicepulse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val DevicePulseTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.04).em
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
)
