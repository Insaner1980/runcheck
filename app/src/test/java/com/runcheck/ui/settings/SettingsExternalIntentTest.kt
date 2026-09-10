package com.runcheck.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsExternalIntentTest {
    @Test
    fun `external activity failure is handled`() {
        val context = mockk<Context>()
        val intent = mockk<Intent>()
        every { context.startActivity(intent) } throws ActivityNotFoundException()

        assertFalse(startActivitySafely(context, intent))
    }

    @Test
    fun `external activity success is reported`() {
        val context = mockk<Context>()
        val intent = mockk<Intent>()
        every { context.startActivity(intent) } returns Unit

        assertTrue(startActivitySafely(context, intent))
    }

    @Test
    fun `external activity security failure is handled`() {
        val context = mockk<Context>()
        val intent = mockk<Intent>()
        every { context.startActivity(intent) } throws SecurityException("denied")

        assertFalse(startActivitySafely(context, intent))
    }
}
