package com.runcheck.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RuncheckShapes =
    Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

val HeroCardShape = RuncheckShapes.extraLarge

val RuncheckPillShape = RoundedCornerShape(percent = 50)

val NavigationIndicatorShape = RuncheckPillShape

val BadgeShape = RuncheckPillShape

val BottomSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
