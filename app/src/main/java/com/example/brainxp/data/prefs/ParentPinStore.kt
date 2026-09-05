package com.example.brainxp.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ParentPinStore
    @Inject
    constructor(
        @Named("settingsStore") private val store: DataStore<Preferences>,
    ) {
        suspend fun set(pin: String?) {
            store.edit { prefs ->
                if (pin == null) {
                    prefs.remove(SettingsDataStore.KEY_PARENT_PIN)
                } else {
                    prefs[SettingsDataStore.KEY_PARENT_PIN] = hash(pin)
                }
            }
        }

        suspend fun matches(pin: String): Boolean = store.data.first()[SettingsDataStore.KEY_PARENT_PIN] == hash(pin)

        private fun hash(pin: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(pin.toByteArray())
                .joinToString("") { byte -> "%02x".format(byte) }
    }
