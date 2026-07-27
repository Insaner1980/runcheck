package com.runcheck.ui.theme

import androidx.compose.ui.graphics.Color

// Dark surfaces and content
val BgPageDark = Color(0xFF08171C)
val Surface1Dark = Color(0xFF0D2229)
val Surface2Dark = Color(0xFF123039)
val Surface3Dark = Color(0xFF183D47)
val TextPrimaryDark = Color(0xFFF4FAFC)
val TextSecondaryDark = Color(0xFFA9BEC6)
val TextMutedDark = Color(0xFF789099)

// Light surfaces and content
val BgPageLight = Color(0xFFDDE6EA)
val Surface1Light = Color(0xFFFFFFFF)
val Surface2Light = Color(0xFFF4F7F8)
val Surface3Light = Color(0xFFE8EFF2)
val LightCardBorder = Color(0xFF7A939D)
val TextPrimaryLight = Color(0xFF172A32)
val TextSecondaryLight = Color(0xFF405A64)
val TextMutedLight = Color(0xFF5B727C)

// Domain accents
val BatteryAccentDark = Color(0xFFFFB627)
val BatteryAccentLight = Color(0xFF9B5C00)
val NetworkAccentDark = Color(0xFF4EA8F5)
val NetworkAccentLight = Color(0xFF0B63B0)
val ThermalAccentDark = Color(0xFFFF7A45)
val ThermalAccentLight = Color(0xFFC24A12)
val StorageAccentDark = Color(0xFF35DDBE)
val StorageAccentLight = Color(0xFF007A66)

// Source-compatible aliases used by screens that migrate in later phases.
val BgPage = BgPageDark
val BgCard = Surface1Dark
val BgCardDeep = Surface2Dark
val BgCardAlt = Surface1Dark
val BgIconCircle = Surface3Dark
val AccentTeal = StorageAccentDark
val AccentBlue = NetworkAccentDark
val AccentAmber = BatteryAccentDark
val AccentOrange = Color(0xFFF5963A)
val AccentRed = Color(0xFFF06040)
val AccentLime = Color(0xFFC8E636)
val AccentYellow = Color(0xFFF5D03A)
val TextPrimary = TextPrimaryDark
val TextSecondary = TextSecondaryDark
val TextMuted = TextMutedDark
val TextOnLime = Color(0xFF1A2E0A)
val WidgetStatusPoorNight = Color(0xFFFFB77D)
val WidgetStatusCriticalNight = Color(0xFFFFB4AB)

// Source-compatible light aliases.
val LightBackground = BgPageLight
val LightSurface = Surface1Light
val LightSurfaceContainerLow = Surface2Light
val LightSurfaceContainer = Surface1Light
val LightSurfaceContainerHigh = Surface3Light
val LightSurfaceContainerHighest = Surface3Light
val LightPrimary = NetworkAccentLight
val LightSecondary = StorageAccentLight
val LightTertiary = BatteryAccentLight
val LightError = Color(0xFFB3261E)
val LightOnSurface = TextPrimaryLight
val LightOnSurfaceVariant = TextSecondaryLight
val LightOutline = LightCardBorder
val LightOutlineVariant = LightCardBorder

// Semantic status palette
val StatusHealthy = Color(0xFF006B57)
val StatusFair = Color(0xFF795F00)
val StatusPoor = Color(0xFF9C4E00)
val StatusCritical = Color(0xFFB3261E)
val StatusNeutral = Color(0xFF4E6570)
val StatusUnavailable = Color(0xFF647A83)
