package com.devicepulse.ui.theme

import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalReducedMotion = staticCompositionLocalOf { false }

val MaterialTheme.reducedMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReducedMotion.current

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    surface = LightSurface,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    background = LightBackground,
    onBackground = LightOnBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    surface = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    background = DarkBackground,
    onBackground = DarkOnBackground
)

private val AmoledBlackColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    surface = AmoledSurface,
    surfaceContainer = AmoledSurfaceContainer,
    surfaceContainerHigh = AmoledSurfaceContainerHigh,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = AmoledOutline,
    background = AmoledBackground,
    onBackground = DarkOnBackground
)

@Composable
fun DevicePulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledBlack: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                val base = if (amoledBlack) {
                    dynamicDarkColorScheme(context).copy(
                        surface = AmoledSurface,
                        surfaceContainer = AmoledSurfaceContainer,
                        surfaceContainerHigh = AmoledSurfaceContainerHigh,
                        background = AmoledBackground
                    )
                } else {
                    dynamicDarkColorScheme(context)
                }
                base.copy(
                    primary = DarkPrimary,
                    onPrimary = DarkOnPrimary,
                    primaryContainer = DarkPrimaryContainer,
                    onPrimaryContainer = DarkOnPrimaryContainer
                )
            } else {
                dynamicLightColorScheme(context).copy(
                    primary = LightPrimary,
                    onPrimary = LightOnPrimary,
                    primaryContainer = LightPrimaryContainer,
                    onPrimaryContainer = LightOnPrimaryContainer
                )
            }
        }
        darkTheme && amoledBlack -> AmoledBlackColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    val accessibilityManager = context.getSystemService(
        AccessibilityManager::class.java
    )
    val reducedMotion = accessibilityManager?.let {
        try {
            val field = AccessibilityManager::class.java
                .getDeclaredMethod("isReducedMotionEnabled")
            field.invoke(it) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    } ?: false

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalStatusColors provides statusColors,
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DevicePulseTypography,
            content = content
        )
    }
}
