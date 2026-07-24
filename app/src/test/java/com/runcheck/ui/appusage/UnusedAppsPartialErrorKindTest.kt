package com.runcheck.ui.appusage

import com.runcheck.domain.model.UnusedAppError
import org.junit.Assert.assertEquals
import org.junit.Test

class UnusedAppsPartialErrorKindTest {
    @Test
    fun `label-only failure does not claim app size is unavailable`() {
        assertEquals(
            UnusedAppsPartialErrorKind.LABELS_ONLY,
            classifyUnusedAppsPartialErrors(setOf(UnusedAppError.PACKAGE_LABEL)),
        )
    }
}
