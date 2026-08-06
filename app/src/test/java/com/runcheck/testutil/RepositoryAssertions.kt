package com.runcheck.testutil

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals

suspend fun <T> assertRepositoryReads(
    expected: List<T>,
    getReadings: (Int?) -> Flow<List<T>>,
    getReadingsSync: suspend () -> List<T>,
    getAllReadings: suspend () -> List<T>,
) {
    assertEquals(expected, getReadings(null).first())
    assertEquals(expected, getReadings(1).first())
    assertEquals(expected, getReadingsSync())
    assertEquals(expected, getAllReadings())
}
