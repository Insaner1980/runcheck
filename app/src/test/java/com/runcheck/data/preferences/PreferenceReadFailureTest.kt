package com.runcheck.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class PreferenceReadFailureTest {
    @Test
    fun `settings read failure cannot become default retention or an empty selection`() =
        runTest {
            val failure = IOException("settings unavailable")
            val store = failingStore(failure)
            val repository = UserPreferencesRepositoryImpl(store)

            assertSame(failure, runCatching { repository.getPreferences().first() }.exceptionOrNull())
            assertSame(failure, runCatching { repository.getSelectedChargerId() }.exceptionOrNull())
            assertSame(failure, runCatching { repository.getAppUsageLastCollectedAt() }.exceptionOrNull())
        }

    @Test
    fun `heartbeat read failure cannot become a missing heartbeat`() =
        runTest {
            val failure = IOException("heartbeat unavailable")
            val repository = MonitoringStatusRepositoryImpl(failingStore(failure))

            assertSame(failure, runCatching { repository.observeLastWorkerHeartbeat().first() }.exceptionOrNull())
        }

    private fun failingStore(failure: IOException): DataStore<Preferences> =
        mockk<DataStore<Preferences>>().also { store ->
            every { store.data } returns flow { throw failure }
        }
}
