package com.runcheck.widget

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetProGateTest {
    @Test
    fun `free users are locked before snapshot flow is created`() =
        runTest {
            var snapshotSubscriptions = 0
            val state =
                proGatedWidgetState(MutableStateFlow(false)) {
                    snapshotSubscriptions += 1
                    flowOf(WidgetRenderState.Content("snapshot"))
                }.first()

            assertEquals(WidgetRenderState.Locked, state)
            assertEquals(0, snapshotSubscriptions)
        }

    @Test
    fun `pro users receive the snapshot state`() =
        runTest {
            val state =
                proGatedWidgetState(MutableStateFlow(true)) {
                    flowOf(WidgetRenderState.Content("snapshot"))
                }.first()

            assertEquals(WidgetRenderState.Content("snapshot"), state)
        }
}
