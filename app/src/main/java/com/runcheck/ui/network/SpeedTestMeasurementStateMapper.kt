package com.runcheck.ui.network

import com.runcheck.ui.common.MeasurementState

internal enum class SpeedTestStage {
    PING,
    DOWNLOAD,
    UPLOAD,
    COMPLETED,
}

internal enum class SpeedTestStageState {
    COMPLETED,
    ACTIVE,
    UPCOMING,
}

fun SpeedTestUiState.toMeasurementState(): MeasurementState =
    when (phase) {
        SpeedTestPhase.Idle -> MeasurementState.Idle
        SpeedTestPhase.Ping -> MeasurementState.Preparing
        SpeedTestPhase.Download -> MeasurementState.Sampling(progress = downloadProgress.coerceIn(0f, 1f))
        SpeedTestPhase.Upload -> MeasurementState.Sampling(progress = uploadProgress.coerceIn(0f, 1f))
        SpeedTestPhase.Completed -> MeasurementState.Result
        is SpeedTestPhase.Failed -> MeasurementState.Failed(phase.error)
    }

internal fun SpeedTestPhase.stageStates(): List<Pair<SpeedTestStage, SpeedTestStageState>> {
    val activeIndex =
        when (this) {
            SpeedTestPhase.Idle, is SpeedTestPhase.Failed -> null
            SpeedTestPhase.Ping -> 0
            SpeedTestPhase.Download -> 1
            SpeedTestPhase.Upload -> 2
            SpeedTestPhase.Completed -> 3
        }

    return SpeedTestStage.entries.mapIndexed { index, stage ->
        val state =
            when {
                this == SpeedTestPhase.Completed -> SpeedTestStageState.COMPLETED
                activeIndex == null -> SpeedTestStageState.UPCOMING
                index < activeIndex -> SpeedTestStageState.COMPLETED
                index == activeIndex -> SpeedTestStageState.ACTIVE
                else -> SpeedTestStageState.UPCOMING
            }
        stage to state
    }
}
