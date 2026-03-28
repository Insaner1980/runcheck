package com.runcheck.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RuncheckShapes =
    Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(percent = 50),
    )

/** Bottom sheet shape — top corners only, matches shapes.large radius. */
val BottomSheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
