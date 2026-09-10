package com.runcheck.data.billing

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Test

class ProStatusCacheTest {
    @Test
    fun `malformed cached value falls back to free and removes invalid key`() {
        val context = mockk<Context>()
        val preferences = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { context.getSharedPreferences("pro_status_cache", Context.MODE_PRIVATE) } returns preferences
        every { preferences.getBoolean("is_pro", false) } throws ClassCastException("Not a boolean")
        every { preferences.edit() } returns editor
        every { editor.remove("is_pro") } returns editor

        val cachedStatus = ProStatusCache(context).getCachedProStatus()

        assertFalse(cachedStatus)
        verify(exactly = 1) { editor.remove("is_pro") }
        verify(exactly = 1) { editor.apply() }
    }
}
