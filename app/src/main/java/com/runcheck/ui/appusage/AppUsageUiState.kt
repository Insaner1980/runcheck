package com.runcheck.ui.appusage

import com.runcheck.domain.model.UnusedAppCandidate
import com.runcheck.domain.model.UnusedAppError
import com.runcheck.domain.model.UnusedAppsPeriod
import com.runcheck.ui.common.UiText

sealed interface AppUsageUiState {
    data object Loading : AppUsageUiState

    data object Locked : AppUsageUiState

    data class Success(
        val totalForegroundTimeMs: Long,
        val maxForegroundTimeMs: Long,
    ) : AppUsageUiState

    data class Error(
        val message: UiText,
    ) : AppUsageUiState
}

sealed interface UnusedAppsUiState {
    data object Idle : UnusedAppsUiState

    data object Loading : UnusedAppsUiState

    data object Locked : UnusedAppsUiState

    data class PermissionRequired(
        val period: UnusedAppsPeriod,
    ) : UnusedAppsUiState

    data class Success(
        val period: UnusedAppsPeriod,
        val candidates: List<UnusedAppCandidate>,
        val partialErrors: Set<UnusedAppError>,
    ) : UnusedAppsUiState

    data class Error(
        val message: UiText,
    ) : UnusedAppsUiState
}

internal enum class UnusedAppsPartialErrorKind {
    NONE,
    STORAGE_ONLY,
    LABELS_ONLY,
    STORAGE_AND_LABELS,
}

internal fun classifyUnusedAppsPartialErrors(
    errors: Set<UnusedAppError>,
): UnusedAppsPartialErrorKind {
    val hasStorageError =
        UnusedAppError.STORAGE_PERMISSION in errors || UnusedAppError.STORAGE_IO in errors
    val hasLabelError = UnusedAppError.PACKAGE_LABEL in errors
    return when {
        hasStorageError && hasLabelError -> UnusedAppsPartialErrorKind.STORAGE_AND_LABELS
        hasStorageError -> UnusedAppsPartialErrorKind.STORAGE_ONLY
        hasLabelError -> UnusedAppsPartialErrorKind.LABELS_ONLY
        else -> UnusedAppsPartialErrorKind.NONE
    }
}
