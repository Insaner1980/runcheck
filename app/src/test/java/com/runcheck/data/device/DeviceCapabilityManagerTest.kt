package com.runcheck.data.device

import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.SignConvention
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceCapabilityManagerTest {

    // -- inferUnit tests --

    @Test
    fun `inferUnit returns MICROAMPS when max reading exceeds threshold`() {
        val readings = listOf(500_000, -300_000, 100_000)
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MILLIAMPS when all readings are below threshold`() {
        val readings = listOf(500, -300, 1000)
        assertEquals(CurrentUnit.MILLIAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MILLIAMPS for readings exactly at threshold`() {
        // Threshold is 25000; values at exactly 25000 are not above it
        val readings = listOf(25_000, -5000)
        assertEquals(CurrentUnit.MILLIAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MICROAMPS when one reading just exceeds threshold`() {
        val readings = listOf(25_001, -5000)
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MILLIAMPS for 120W fast charging in mA`() {
        // 120W at 10V = 12A = 12000 mA — must not be misclassified as µA
        val readings = listOf(12_000, 11_500, 12_200)
        assertEquals(CurrentUnit.MILLIAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MICROAMPS for normal screen-on µA readings`() {
        // ~150 mA screen-on idle = 150000 µA — well above threshold
        val readings = listOf(150_000, -140_000, 160_000)
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MILLIAMPS for empty readings`() {
        assertEquals(CurrentUnit.MILLIAMPS, DeviceCapabilityManager.inferUnit(emptyList()))
    }

    @Test
    fun `inferUnit uses absolute values for negative readings`() {
        // -50000 has abs value 50000, which exceeds threshold
        val readings = listOf(-50_000)
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MILLIAMPS for all-zero readings`() {
        val readings = listOf(0, 0, 0)
        assertEquals(CurrentUnit.MILLIAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    // -- inferSignConvention tests --

    @Test
    fun `inferSignConvention returns POSITIVE_CHARGING when charging with positive average`() {
        val readings = listOf(500, 600, 700)
        assertEquals(
            SignConvention.POSITIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = true, readings = readings)
        )
    }

    @Test
    fun `inferSignConvention returns NEGATIVE_CHARGING when charging with negative average`() {
        val readings = listOf(-500, -600, -700)
        assertEquals(
            SignConvention.NEGATIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = true, readings = readings)
        )
    }

    @Test
    fun `inferSignConvention returns POSITIVE_CHARGING when discharging with negative average`() {
        val readings = listOf(-500, -600, -700)
        assertEquals(
            SignConvention.POSITIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = false, readings = readings)
        )
    }

    @Test
    fun `inferSignConvention returns NEGATIVE_CHARGING when discharging with positive average`() {
        val readings = listOf(500, 600, 700)
        assertEquals(
            SignConvention.NEGATIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = false, readings = readings)
        )
    }

    @Test
    fun `inferSignConvention handles mixed readings where average is positive`() {
        // Average of (-100, 500, 600) = 333.3, positive while charging -> POSITIVE_CHARGING
        val readings = listOf(-100, 500, 600)
        assertEquals(
            SignConvention.POSITIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = true, readings = readings)
        )
    }

    @Test
    fun `inferSignConvention handles zero average as NEGATIVE_CHARGING when charging`() {
        // Average of (-500, 500) = 0.0; condition (isCharging && avgReading > 0) is false
        val readings = listOf(-500, 500)
        assertEquals(
            SignConvention.NEGATIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = true, readings = readings)
        )
    }

    @Test
    fun `inferSignConvention handles zero average as NEGATIVE_CHARGING when discharging`() {
        // Average of (-500, 500) = 0.0; condition (!isCharging && avgReading < 0) is false
        val readings = listOf(-500, 500)
        assertEquals(
            SignConvention.NEGATIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = false, readings = readings)
        )
    }
}
