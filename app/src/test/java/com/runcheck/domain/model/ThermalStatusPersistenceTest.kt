package com.runcheck.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThermalStatusPersistenceTest {
    @Test
    fun `integer codes remain compatible with historical rows`() {
        val expected =
            mapOf(
                ThermalStatus.NONE to 0,
                ThermalStatus.LIGHT to 1,
                ThermalStatus.MODERATE to 2,
                ThermalStatus.SEVERE to 3,
                ThermalStatus.CRITICAL to 4,
                ThermalStatus.EMERGENCY to 5,
                ThermalStatus.SHUTDOWN to 6,
            )
        expected.forEach { (status, code) ->
            assertEquals(code, ThermalStatusPersistence.toCode(status))
            assertEquals(status, ThermalStatusPersistence.fromCode(code))
            assertEquals(status, ThermalStatusPersistence.fromCode(ThermalStatusPersistence.toCode(status)))
        }
    }

    @Test
    fun `unknown integer codes remain unknown`() {
        listOf(-1, 7, 99, Int.MIN_VALUE, Int.MAX_VALUE).forEach { code ->
            assertNull(ThermalStatusPersistence.fromCode(code))
        }
    }

    @Test
    fun `string identifiers remain compatible with historical events`() {
        val expected =
            mapOf(
                ThermalStatus.NONE to "NONE",
                ThermalStatus.LIGHT to "LIGHT",
                ThermalStatus.MODERATE to "MODERATE",
                ThermalStatus.SEVERE to "SEVERE",
                ThermalStatus.CRITICAL to "CRITICAL",
                ThermalStatus.EMERGENCY to "EMERGENCY",
                ThermalStatus.SHUTDOWN to "SHUTDOWN",
            )
        expected.forEach { (status, id) ->
            assertEquals(id, ThermalStatusPersistence.toId(status))
            assertEquals(status, ThermalStatusPersistence.fromId(id))
            assertEquals(status, ThermalStatusPersistence.fromId(ThermalStatusPersistence.toId(status)))
        }
    }

    @Test
    fun `unknown string identifiers are not normalized`() {
        listOf("", "severe", "Severe", " SEVERE ", "UNKNOWN").forEach { id ->
            assertNull(ThermalStatusPersistence.fromId(id))
        }
    }
}
