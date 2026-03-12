package com.devicepulse.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devicepulse.ui.theme.AccentYellow

@Composable
fun ProBadgePill(
    modifier: Modifier = Modifier,
    text: String = "PRO"
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = AccentYellow.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = AccentYellow
        )
    }
}
