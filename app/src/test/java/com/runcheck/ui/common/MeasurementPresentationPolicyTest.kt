package com.runcheck.ui.common

import com.runcheck.domain.model.MediaCategory
import com.runcheck.ui.network.SpeedTestPhase
import com.runcheck.ui.network.SpeedTestUiState
import com.runcheck.ui.network.toMeasurementState
import com.runcheck.ui.storage.cleanup.CleanupUiState
import com.runcheck.ui.storage.cleanup.FileGroup
import com.runcheck.ui.storage.cleanup.toMeasurementState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementPresentationPolicyTest {
    @Test
    fun speedTestPhasesMapToTheirActualMeasurementStates() {
        assertEquals(
            MeasurementState.Idle,
            SpeedTestUiState(phase = SpeedTestPhase.Idle).toMeasurementState(),
        )
        assertEquals(
            MeasurementState.Preparing,
            SpeedTestUiState(phase = SpeedTestPhase.Ping).toMeasurementState(),
        )
        assertEquals(
            MeasurementState.Sampling(progress = 0.4f),
            SpeedTestUiState(
                phase = SpeedTestPhase.Download,
                downloadProgress = 0.4f,
            ).toMeasurementState(),
        )
        assertEquals(
            MeasurementState.Sampling(progress = 0.8f),
            SpeedTestUiState(
                phase = SpeedTestPhase.Upload,
                uploadProgress = 0.8f,
            ).toMeasurementState(),
        )
        assertEquals(
            MeasurementState.Result,
            SpeedTestUiState(phase = SpeedTestPhase.Completed).toMeasurementState(),
        )

        val failure = UiText.Dynamic("Network unavailable")
        assertEquals(
            MeasurementState.Failed(failure),
            SpeedTestUiState(phase = SpeedTestPhase.Failed(failure)).toMeasurementState(),
        )
    }

    @Test
    fun speedTestProgressIsClampedToTheVisibleRange() {
        assertEquals(
            MeasurementState.Sampling(progress = 0f),
            SpeedTestUiState(
                phase = SpeedTestPhase.Download,
                downloadProgress = -0.3f,
            ).toMeasurementState(),
        )
        assertEquals(
            MeasurementState.Sampling(progress = 1f),
            SpeedTestUiState(
                phase = SpeedTestPhase.Upload,
                uploadProgress = 1.4f,
            ).toMeasurementState(),
        )
    }

    @Test
    fun cleanupStatesMapOnlyToTheirActualOperationStates() {
        assertEquals(MeasurementState.Idle, CleanupUiState.Idle.toMeasurementState())
        assertEquals(
            MeasurementState.Sampling(progress = null),
            CleanupUiState.Scanning().toMeasurementState(),
        )
        assertEquals(
            MeasurementState.Sampling(progress = 0.25f),
            CleanupUiState.Scanning(0.25f).toMeasurementState(),
        )
        assertEquals(
            MeasurementState.Computing,
            CleanupUiState.Deleting(count = 3).toMeasurementState(),
        )
        val results =
            CleanupUiState.Results(
                groups = listOf(FileGroup(category = MediaCategory.IMAGE, itemCount = 1, totalBytes = 42L)),
                selectedCount = 1,
                selectedSize = 42L,
                totalSize = 42L,
                totalCount = 1,
                currentUsagePercent = 60f,
                projectedUsagePercent = 59f,
                maxFileSizeBytes = 42L,
                pagerGeneration = 1,
            )
        assertEquals(
            MeasurementState.Result,
            results.toMeasurementState(),
        )
        assertEquals(MeasurementState.Result, CleanupUiState.Empty.toMeasurementState())
        assertEquals(
            MeasurementState.Result,
            CleanupUiState.Success(freedBytes = 42L).toMeasurementState(),
        )
        assertEquals(MeasurementState.Idle, CleanupUiState.UnsupportedVersion.toMeasurementState())

        val failure = UiText.Dynamic("Scan failed")
        assertEquals(
            MeasurementState.Failed(failure),
            CleanupUiState.Error(failure).toMeasurementState(),
        )
    }

    @Test
    fun cleanupNegativeProgressIsIndeterminateAndPositiveOverflowIsClamped() {
        assertEquals(
            MeasurementState.Sampling(progress = null),
            CleanupUiState.Scanning(-1f).toMeasurementState(),
        )
        assertEquals(
            MeasurementState.Sampling(progress = null),
            CleanupUiState.Scanning(-0.1f).toMeasurementState(),
        )
        assertEquals(
            MeasurementState.Sampling(progress = 1f),
            CleanupUiState.Scanning(1.2f).toMeasurementState(),
        )
    }

    @Test
    fun unknownProgressKeepsIndeterminateSemanticsWhenMotionStops() {
        val active =
            measurementMotionPolicy(
                state = MeasurementState.Sampling(progress = null),
                reducedMotion = false,
                isResumed = true,
            )
        assertTrue(active.showIndeterminateIndicator)

        val reduced =
            measurementMotionPolicy(
                state = MeasurementState.Sampling(progress = null),
                reducedMotion = true,
                isResumed = true,
            )
        assertFalse(reduced.showIndeterminateIndicator)
        assertTrue(reduced.isIndeterminate)
        assertNull(reduced.displayProgress)

        val paused =
            measurementMotionPolicy(
                state = MeasurementState.Sampling(progress = null),
                reducedMotion = false,
                isResumed = false,
            )
        assertFalse(paused.showIndeterminateIndicator)
        assertTrue(paused.isIndeterminate)
        assertNull(paused.displayProgress)

        val deleting =
            measurementMotionPolicy(
                state = MeasurementState.Computing,
                reducedMotion = true,
                isResumed = false,
            )
        assertFalse(deleting.showIndeterminateIndicator)
        assertTrue(deleting.isIndeterminate)
        assertNull(deleting.displayProgress)
    }

    @Test
    fun sameMeasurementPhaseKeepsItsTransitionKeyAndDoesNotAnnounceIntermediateValues() {
        val firstSample =
            measurementMotionPolicy(
                state = MeasurementState.Sampling(progress = 0.1f),
                reducedMotion = false,
                isResumed = true,
            )
        val laterSample =
            measurementMotionPolicy(
                state = MeasurementState.Sampling(progress = 0.9f),
                reducedMotion = false,
                isResumed = true,
            )

        assertEquals(firstSample.transitionKey, laterSample.transitionKey)
        assertFalse(firstSample.announceFinalState)
        assertFalse(laterSample.announceFinalState)
        assertTrue(
            measurementMotionPolicy(
                state = MeasurementState.Result,
                reducedMotion = false,
                isResumed = true,
            ).announceFinalState,
        )
        assertNull(
            measurementMotionPolicy(
                state = MeasurementState.Idle,
                reducedMotion = false,
                isResumed = true,
            ).displayProgress,
        )
    }
}
