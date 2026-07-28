package com.runcheck.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.runcheck.ui.common.MeasurementState
import com.runcheck.ui.common.measurementMotionPolicy
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.theme.reducedMotion

@Composable
internal fun rememberMeasurementIsResumed(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var isResumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycle) {
        val observer =
            LifecycleEventObserver { _, _ ->
                isResumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    return isResumed
}

@Composable
internal fun MeasurementIndicator(
    state: MeasurementState,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = MaterialTheme.reducedMotion
    val indicatorColor = MaterialTheme.colorScheme.primary
    val policy = measurementMotionPolicy(state, reducedMotion, rememberMeasurementIsResumed())
    val progress by animateFloatAsState(
        targetValue = policy.displayProgress ?: 0f,
        animationSpec = MotionTokens.measurementIndicatorTween(reducedMotion),
        label = "measurement_indicator_${policy.transitionKey}",
    )
    val semanticsModifier =
        modifier.semantics {
            this.contentDescription = contentDescription
            if (policy.isIndeterminate) {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            if (policy.announceFinalState) {
                liveRegion = LiveRegionMode.Polite
            }
        }

    if (policy.showIndeterminateIndicator) {
        CircularProgressIndicator(
            modifier = semanticsModifier.size(48.dp),
            color = indicatorColor,
        )
    } else if (policy.isIndeterminate) {
        Canvas(modifier = semanticsModifier.size(48.dp)) {
            drawCircle(
                color = indicatorColor,
                style = Stroke(width = 4.dp.toPx()),
            )
        }
    } else if (policy.displayProgress != null) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = semanticsModifier.size(48.dp),
            color = indicatorColor,
        )
    }
}
