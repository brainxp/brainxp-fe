package com.example.brainxp.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.alertStore: DataStore<Preferences> by preferencesDataStore(name = "brainxp_alerts")
private val KEY_LAST_SEEN = intPreferencesKey("last_seen_alert")

class AlertSeenStore(
    private val store: DataStore<Preferences>,
) {
    suspend fun lastSeen(): Int = store.data.first()[KEY_LAST_SEEN] ?: 0

    suspend fun remember(alertId: Int) {
        store.edit { prefs ->
            val known = prefs[KEY_LAST_SEEN] ?: 0
            if (alertId > known) prefs[KEY_LAST_SEEN] = alertId
        }
    }

    companion object {
        fun from(context: Context): DataStore<Preferences> = context.alertStore
    }
}
