package com.runcheck.ui.storage.cleanup

import com.runcheck.ui.common.MeasurementState

fun CleanupUiState.toMeasurementState(): MeasurementState =
    when (this) {
        CleanupUiState.Idle,
        CleanupUiState.UnsupportedVersion,
        -> {
            MeasurementState.Idle
        }

        is CleanupUiState.Scanning -> {
            MeasurementState.Sampling(
                progress = progress.takeIf { it >= 0f }?.coerceIn(0f, 1f),
            )
        }

        is CleanupUiState.Deleting -> {
            MeasurementState.Computing
        }

        is CleanupUiState.Results,
        is CleanupUiState.Success,
        CleanupUiState.Empty,
        -> {
            MeasurementState.Result
        }

        is CleanupUiState.Error -> {
            MeasurementState.Failed(message)
        }
    }
