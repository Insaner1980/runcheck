package com.runcheck.data.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveSpeedTestSessionGuardTest {
    @Test
    fun `rejects repeated activation until the active session releases ownership`() {
        val guard = ActiveSpeedTestSessionGuard<Any>()
        val first = Any()
        val second = Any()

        assertTrue(guard.tryActivate(first))
        assertFalse(guard.tryActivate(second))
        assertSame(first, guard.current())

        assertTrue(guard.release(first))
        assertTrue(guard.tryActivate(second))
        assertSame(second, guard.current())
    }

    @Test
    fun `stale session cannot release a newer active session`() {
        val guard = ActiveSpeedTestSessionGuard<Any>()
        val first = Any()
        val second = Any()

        assertTrue(guard.tryActivate(first))
        assertTrue(guard.release(first))
        assertTrue(guard.tryActivate(second))

        assertFalse(guard.release(first))
        assertTrue(guard.isActive(second))
    }
}
