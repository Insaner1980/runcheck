package com.runcheck.ui.components

import com.runcheck.domain.model.Confidence
import org.junit.Assert.assertEquals
import org.junit.Test

class PlatformTelemetryMeasurementTest {
    @Test
    fun `platform telemetry is presented as estimated rather than invented accurate data`() {
        val measurement = platformTelemetryMeasurement(value = -62, unavailableValue = 0)

        assertEquals(-62, measurement.value)
        assertEquals(Confidence.LOW, measurement.confidence)
    }

    @Test
    fun `missing platform telemetry is unavailable`() {
        val measurement = platformTelemetryMeasurement<Int>(value = null, unavailableValue = 0)

        assertEquals(0, measurement.value)
        assertEquals(Confidence.UNAVAILABLE, measurement.confidence)
    }
}
