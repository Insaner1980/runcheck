package com.runcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.domain.model.SignalQuality
import com.runcheck.ui.theme.statusColorForSignalQuality

private val BAR_HEIGHTS = listOf(10.dp, 18.dp, 26.dp, 36.dp, 48.dp)
private val BAR_WIDTH = 12.dp
private val BAR_GAP = 4.dp
private val BAR_CORNER = 3.dp

private fun activeBarsFor(quality: SignalQuality): Int =
    when (quality) {
        SignalQuality.EXCELLENT -> 5
        SignalQuality.GOOD -> 4
        SignalQuality.FAIR -> 3
        SignalQuality.POOR -> 2
        SignalQuality.NO_SIGNAL -> 0
    }

@Composable
fun SignalBars(
    signalQuality: SignalQuality,
    qualityLabel: String,
    modifier: Modifier = Modifier,
) {
    val activeBars = activeBarsFor(signalQuality)
    val activeColor = statusColorForSignalQuality(signalQuality)
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val a11yDesc = stringResource(R.string.a11y_signal_bars, qualityLabel, activeBars)

    Row(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                contentDescription = a11yDesc
            },
        horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
        verticalAlignment = Alignment.Bottom,
    ) {
        BAR_HEIGHTS.forEachIndexed { index, height ->
            val isActive = index < activeBars
            Box(
                modifier =
                    Modifier
                        .width(BAR_WIDTH)
                        .height(height)
                        .background(
                            color = if (isActive) activeColor else inactiveColor,
                            shape = RoundedCornerShape(BAR_CORNER),
                        ),
            )
        }
    }
}
