package com.runcheck.ui.common

/** Presentation-only state for an operation whose progress is visible to the user. */
sealed interface MeasurementState {
    data object Idle : MeasurementState

    data object Preparing : MeasurementState

    data class Sampling(
        val progress: Float?,
    ) : MeasurementState

    data object Computing : MeasurementState

    data object Settling : MeasurementState

    data object Result : MeasurementState

    data class Failed(
        val message: UiText,
    ) : MeasurementState
}

/**
 * Keeps operation motion tied to phase changes instead of every live value emission.
 */
data class MeasurementMotionPolicy(
    val transitionKey: String,
    val displayProgress: Float?,
    val showIndeterminateIndicator: Boolean,
    val announceFinalState: Boolean,
) {
    companion object {
        const val STATIC_PROGRESS = 0.6f
    }
}

fun measurementMotionPolicy(
    state: MeasurementState,
    reducedMotion: Boolean,
    isResumed: Boolean,
): MeasurementMotionPolicy {
    val progress = (state as? MeasurementState.Sampling)?.progress?.coerceIn(0f, 1f)
    val activeWithoutProgress =
        state is MeasurementState.Preparing ||
            (state is MeasurementState.Sampling && progress == null) ||
            state is MeasurementState.Computing ||
            state is MeasurementState.Settling
    val showIndeterminateIndicator = activeWithoutProgress && !reducedMotion && isResumed

    return MeasurementMotionPolicy(
        transitionKey =
            when (state) {
                MeasurementState.Idle -> "idle"
                MeasurementState.Preparing -> "preparing"
                is MeasurementState.Sampling -> "sampling"
                MeasurementState.Computing -> "computing"
                MeasurementState.Settling -> "settling"
                MeasurementState.Result -> "result"
                is MeasurementState.Failed -> "failed"
            },
        displayProgress =
            progress ?: if (activeWithoutProgress && !showIndeterminateIndicator) {
                MeasurementMotionPolicy.STATIC_PROGRESS
            } else {
                null
            },
        showIndeterminateIndicator = showIndeterminateIndicator,
        announceFinalState = state is MeasurementState.Result || state is MeasurementState.Failed,
    )
}
