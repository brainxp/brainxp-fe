package com.example.brainxp.core.device

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.bindingStore: DataStore<Preferences> by preferencesDataStore(name = "brainxp_device")
private val KEY_BINDING = stringPreferencesKey("install_binding")

class InstallBinding(
    private val store: DataStore<Preferences>,
) {
    suspend fun value(): String {
        store.data.first()[KEY_BINDING]?.let { return it }
        val fresh = UUID.randomUUID().toString()
        store.edit { it[KEY_BINDING] = fresh }
        return store.data.first()[KEY_BINDING] ?: fresh
    }

    companion object {
        fun from(context: Context): DataStore<Preferences> = context.bindingStore
    }
}
