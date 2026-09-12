package com.runcheck.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.runcheck.domain.model.MonitoringFreshnessPolicy
import com.runcheck.domain.model.MonitoringHeartbeat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringStatusRepositoryImplTest {
    @Test
    fun `previous boot remains stale after new uptime overtakes recorded uptime`() =
        runTest {
            val store = MemoryStore()
            MonitoringStatusRepositoryImpl(store, bootCount = 7).setLastWorkerHeartbeat(
                MonitoringHeartbeat(
                    recordedAtEpochMillis = 1_000L,
                    recordedAtUptimeMillis = 60_000L,
                    intervalMinutes = 15,
                ),
            )

            val heartbeat = MonitoringStatusRepositoryImpl(store, bootCount = 8).observeLastWorkerHeartbeat().first()

            assertNotNull(heartbeat)
            assertTrue(
                MonitoringFreshnessPolicy.isStale(
                    heartbeat = heartbeat,
                    currentIntervalMinutes = 15,
                    currentUptimeMillis = 120_000L,
                    currentEpochMillis = 1_000L + 46L * 60_000L,
                ),
            )
        }

    @Test
    fun `same boot still excludes deep sleep from heartbeat age`() =
        runTest {
            val store = MemoryStore()
            val repository = MonitoringStatusRepositoryImpl(store, bootCount = 7)
            repository.setLastWorkerHeartbeat(
                MonitoringHeartbeat(
                    recordedAtEpochMillis = 1_000L,
                    recordedAtUptimeMillis = 60_000L,
                    intervalMinutes = 15,
                ),
            )

            assertFalse(
                MonitoringFreshnessPolicy.isStale(
                    heartbeat = repository.observeLastWorkerHeartbeat().first(),
                    currentIntervalMinutes = 15,
                    currentUptimeMillis = 120_000L,
                    currentEpochMillis = 1_000L + 46L * 60_000L,
                ),
            )
        }

    private class MemoryStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }
}
