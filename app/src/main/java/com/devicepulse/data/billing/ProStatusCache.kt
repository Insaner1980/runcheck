package com.devicepulse.data.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object ProStatusCache {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pro_status")

    suspend fun isPro(context: Context): Boolean =
        context.dataStore.data
            .map { prefs -> prefs[KEY_IS_PRO] ?: false }
            .first()

    fun isProBlocking(context: Context): Boolean = runBlocking {
        isPro(context)
    }

    suspend fun setPro(context: Context, isPro: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_PRO] = isPro
        }
    }

    private val KEY_IS_PRO = booleanPreferencesKey("is_pro")
}
