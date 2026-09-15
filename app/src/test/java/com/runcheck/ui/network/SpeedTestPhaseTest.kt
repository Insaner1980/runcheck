package com.runcheck.ui.network

import com.runcheck.ui.common.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedTestPhaseTest {
    @Test
    fun `running state follows phase for construction and copy`() {
        val cases =
            listOf(
                SpeedTestPhase.Idle to false,
                SpeedTestPhase.Ping to true,
                SpeedTestPhase.Download to true,
                SpeedTestPhase.Upload to true,
                SpeedTestPhase.Completed to false,
                SpeedTestPhase.Failed(UiText.Dynamic("Failure")) to false,
            )

        for ((phase, expected) in cases) {
            assertEquals(phase.toString(), expected, phase.isRunning)
            assertEquals(expected, SpeedTestUiState(phase = phase).isRunning)
            assertEquals(expected, SpeedTestUiState(phase = SpeedTestPhase.Upload).copy(phase = phase).isRunning)
        }
    }
}
