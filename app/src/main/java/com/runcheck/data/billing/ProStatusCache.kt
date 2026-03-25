package com.runcheck.data.billing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight synchronous cache of Pro purchase status.
 * Uses SharedPreferences (not DataStore) because the initial read
 * must be synchronous — otherwise Pro users see a flash of free-tier UI
 * while the async billing query runs on cold start.
 */
@Singleton
class ProStatusCache @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCachedProStatus(): Boolean = prefs.getBoolean(KEY_IS_PRO, false)

    fun setCachedProStatus(isPro: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO, isPro).apply()
    }

    companion object {
        private const val PREFS_NAME = "pro_status_cache"
        private const val KEY_IS_PRO = "is_pro"
    }
}
