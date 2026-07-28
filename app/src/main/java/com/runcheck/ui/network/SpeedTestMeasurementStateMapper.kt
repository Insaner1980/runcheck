package com.runcheck.ui.network

import com.runcheck.ui.common.MeasurementState

fun SpeedTestUiState.toMeasurementState(): MeasurementState =
    when (phase) {
        SpeedTestPhase.Idle -> MeasurementState.Idle
        SpeedTestPhase.Ping -> MeasurementState.Preparing
        SpeedTestPhase.Download -> MeasurementState.Sampling(progress = downloadProgress.coerceIn(0f, 1f))
        SpeedTestPhase.Upload -> MeasurementState.Sampling(progress = uploadProgress.coerceIn(0f, 1f))
        SpeedTestPhase.Completed -> MeasurementState.Result
        is SpeedTestPhase.Failed -> MeasurementState.Failed(phase.error)
    }
