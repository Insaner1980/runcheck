package com.runcheck.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppDispatchersTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `default dispatchers expose standard coroutine dispatchers`() {
        val dispatchers = AppDispatchers()

        assertSame(Dispatchers.IO, dispatchers.io)
        assertSame(Dispatchers.Default, dispatchers.default)
        assertSame(Dispatchers.Main, dispatchers.main)
        assertSame(Dispatchers.Main.immediate, dispatchers.mainImmediate)
    }
}
