package com.example.brainxp.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class DoubtStore
    @Inject
    constructor(
        @Named("deviceStore") private val store: DataStore<Preferences>,
    ) {
        suspend fun load(sessionId: String): Set<String> = store.data.first()[key(sessionId)].orEmpty()

        suspend fun save(
            sessionId: String,
            questionIds: Set<String>,
        ) {
            store.edit { prefs ->
                if (questionIds.isEmpty()) prefs.remove(key(sessionId)) else prefs[key(sessionId)] = questionIds
            }
        }

        suspend fun forget(sessionId: String) {
            store.edit { it.remove(key(sessionId)) }
        }

        private fun key(sessionId: String) = stringSetPreferencesKey("doubts.$sessionId")
    }
