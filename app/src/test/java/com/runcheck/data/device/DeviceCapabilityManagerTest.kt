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
    fun `inferUnit returns MICROAMPS when all readings are below former threshold`() {
        val readings = listOf(500, -300, 1000)
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MICROAMPS for readings at former threshold`() {
        val readings = listOf(25_000, -25_000)
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MICROAMPS when one reading exceeds former threshold`() {
        val readings = listOf(25_001, -5000)
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MICROAMPS for high current readings`() {
        val readings = listOf(12_000, 11_500, 12_200)
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MICROAMPS for normal screen-on µA readings`() {
        // ~150 mA screen-on idle = 150000 µA — well above threshold
        val readings = listOf(150_000, -140_000, 160_000)
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MICROAMPS for empty readings`() {
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(emptyList()))
    }

    @Test
    fun `inferUnit uses absolute values for negative readings`() {
        // -50000 has abs value 50000, which exceeds threshold
        val readings = listOf(-50_000)
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    @Test
    fun `inferUnit returns MICROAMPS for all-zero readings`() {
        val readings = listOf(0, 0, 0)
        assertEquals(CurrentUnit.MICROAMPS, DeviceCapabilityManager.inferUnit(readings))
    }

    // -- current reliability tests --

    @Test
    fun `steady plausible microamp current is reliable`() {
        assertEquals(
            true,
            DeviceCapabilityManager.isCurrentNowReliable(listOf(500_000, 500_000, 500_000)),
        )
    }

    @Test
    fun `plausibility is evaluated after microamp normalization`() {
        assertEquals(
            true,
            DeviceCapabilityManager.isCurrentNowReliable(listOf(10_000_000, 10_000_000, 10_000_000)),
        )
        assertEquals(
            false,
            DeviceCapabilityManager.isCurrentNowReliable(listOf(10_001_000, 10_001_000, 10_001_000)),
        )
    }

    @Test
    fun `small negative microamp readings are normalized before plausibility evaluation`() {
        assertEquals(
            true,
            DeviceCapabilityManager.isCurrentNowReliable(listOf(-10_000, -9_000, -11_000)),
        )
    }

    // -- inferSignConvention tests --

    @Test
    fun `inferSignConvention returns POSITIVE_CHARGING when charging with positive average`() {
        val readings = listOf(500, 600, 700)
        assertEquals(
            SignConvention.POSITIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = true, readings = readings),
        )
    }

    @Test
    fun `inferSignConvention returns NEGATIVE_CHARGING when charging with negative average`() {
        val readings = listOf(-500, -600, -700)
        assertEquals(
            SignConvention.NEGATIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = true, readings = readings),
        )
    }

    @Test
    fun `inferSignConvention returns POSITIVE_CHARGING when discharging with negative average`() {
        val readings = listOf(-500, -600, -700)
        assertEquals(
            SignConvention.POSITIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = false, readings = readings),
        )
    }

    @Test
    fun `inferSignConvention returns NEGATIVE_CHARGING when discharging with positive average`() {
        val readings = listOf(500, 600, 700)
        assertEquals(
            SignConvention.NEGATIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = false, readings = readings),
        )
    }

    @Test
    fun `inferSignConvention handles mixed readings where average is positive`() {
        // Average of (-100, 500, 600) = 333.3, positive while charging -> POSITIVE_CHARGING
        val readings = listOf(-100, 500, 600)
        assertEquals(
            SignConvention.POSITIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = true, readings = readings),
        )
    }

    @Test
    fun `inferSignConvention handles zero average as NEGATIVE_CHARGING when charging`() {
        // Average of (-500, 500) = 0.0; condition (isCharging && avgReading > 0) is false
        val readings = listOf(-500, 500)
        assertEquals(
            SignConvention.NEGATIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = true, readings = readings),
        )
    }

    @Test
    fun `inferSignConvention handles zero average as NEGATIVE_CHARGING when discharging`() {
        // Average of (-500, 500) = 0.0; condition (!isCharging && avgReading < 0) is false
        val readings = listOf(-500, 500)
        assertEquals(
            SignConvention.NEGATIVE_CHARGING,
            DeviceCapabilityManager.inferSignConvention(isCharging = false, readings = readings),
        )
    }
}
