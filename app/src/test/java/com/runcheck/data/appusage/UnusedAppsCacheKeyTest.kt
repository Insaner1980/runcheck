package com.runcheck.data.appusage

import com.runcheck.domain.model.UnusedAppsPeriod
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class UnusedAppsCacheKeyTest {
    @Test
    fun `same screen refresh reuses a cached result`() =
        runTest {
            val cache = UnusedAppsRefreshCache<Int>()
            val key = UnusedAppsCacheKey(UnusedAppsPeriod.DAYS_30, Instant.parse("2026-02-01T00:00:00Z"))
            var loads = 0

            cache.getOrLoad(key, forceRefresh = false) { ++loads }
            val result = cache.getOrLoad(key, forceRefresh = false) { ++loads }

            assertEquals(1, result)
            assertEquals(1, loads)
        }

    @Test
    fun `a new screen refresh cannot reuse the earlier observed state`() =
        runTest {
            val cache = UnusedAppsRefreshCache<Int>()
            val before = UnusedAppsCacheKey(UnusedAppsPeriod.DAYS_30, Instant.parse("2026-02-01T00:00:00Z"))
            val after = UnusedAppsCacheKey(UnusedAppsPeriod.DAYS_30, Instant.parse("2026-02-01T00:01:00Z"))
            var loads = 0

            cache.getOrLoad(before, forceRefresh = false) { ++loads }
            cache.getOrLoad(after, forceRefresh = false) { ++loads }

            assertEquals(2, loads)
        }

    @Test
    fun `permission refresh bypasses a cached candidate result`() =
        runTest {
            val cache = UnusedAppsRefreshCache<Int>()
            val key = UnusedAppsCacheKey(UnusedAppsPeriod.DAYS_30, Instant.parse("2026-02-01T00:00:00Z"))
            var loads = 0

            cache.getOrLoad(key, forceRefresh = false) { ++loads }
            val refreshed = cache.getOrLoad(key, forceRefresh = true) { ++loads }

            assertEquals(2, refreshed)
            assertEquals(2, loads)
        }

    @Test
    fun `new screen refresh clears the previous session values`() =
        runTest {
            val cache = UnusedAppsRefreshCache<Int>()
            val firstSession = UnusedAppsCacheKey(UnusedAppsPeriod.DAYS_30, Instant.parse("2026-02-01T00:00:00Z"))
            val secondSession = UnusedAppsCacheKey(UnusedAppsPeriod.DAYS_60, Instant.parse("2026-02-01T00:01:00Z"))
            var loads = 0

            cache.getOrLoad(firstSession, forceRefresh = false) { ++loads }
            cache.getOrLoad(secondSession, forceRefresh = false) { ++loads }
            cache.getOrLoad(firstSession, forceRefresh = false) { ++loads }

            assertEquals(3, loads)
        }
}
