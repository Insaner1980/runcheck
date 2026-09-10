package com.runcheck.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runcheck.R

// Home's reference palette is deliberately scoped to the dashboard.
val HomeBackground = Color(0xFF10110F)
val HomeSurface = Color(0xFF20211D)
val HomeCream = Color(0xFFE9E6DC)
val HomePeach = Color(0xFFEDA079)
val HomeStone = Color(0xFFA3A198)
val HomeGraphite = Color(0xFF5C5D57)
val HomeInk = Color(0xFF10110F)
val HomeMuted = Color(0xFFB5B5AE)
val HomeGreen = Color(0xFF8AD5AE)
val HomeOrange = Color(0xFFE98548)
val HomeRed = Color(0xFFF16C54)

@OptIn(ExperimentalTextApi::class)
val HomeManropeFontFamily =
    FontFamily(
        listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
            Font(
                R.font.manrope,
                weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
            )
        },
    )

@Composable
fun HomeTheme(content: @Composable () -> Unit) {
    val colors =
        MaterialTheme.colorScheme.copy(
            background = HomeBackground,
            surface = HomeBackground,
            surfaceContainer = HomeSurface,
            surfaceContainerHigh = HomeGraphite,
            surfaceContainerHighest = Color(0xFF2C2D28),
            primary = HomePeach,
            onPrimary = HomeInk,
            onSurface = HomeCream,
            onBackground = HomeCream,
            onSurfaceVariant = HomeMuted,
            outline = HomeMuted,
        )
    CompositionLocalProvider(
        LocalStatusColors provides
            MaterialTheme.statusColors.copy(
                healthy = HomeGreen,
                poor = HomeOrange,
                critical = HomeRed,
                neutral = HomeMuted,
                unavailable = HomeMuted,
            ),
    ) {
        val type = MaterialTheme.typography
        MaterialTheme(
            colorScheme = colors,
            shapes = MaterialTheme.shapes.copy(large = RoundedCornerShape(20.dp)),
            typography =
                type.copy(
                    headlineSmall = type.headlineSmall.copy(fontFamily = HomeManropeFontFamily, fontSize = 20.sp),
                    displaySmall = type.displaySmall.copy(fontFamily = HomeManropeFontFamily),
                    titleSmall = type.titleSmall.copy(fontFamily = HomeManropeFontFamily),
                    titleMedium = type.titleMedium.copy(fontFamily = HomeManropeFontFamily),
                    bodySmall = type.bodySmall.copy(fontFamily = HomeManropeFontFamily),
                    bodyMedium = type.bodyMedium.copy(fontFamily = HomeManropeFontFamily),
                    labelLarge = type.labelLarge.copy(fontFamily = HomeManropeFontFamily),
                ),
            content = content,
        )
    }
}
