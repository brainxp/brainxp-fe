package com.example.brainxp.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

data class AuthSnapshot(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val subjectId: String? = null,
    val familyId: String? = null,
    val role: String? = null,
    val displayName: String? = null,
) {
    val isAuthenticated: Boolean get() = accessToken != null
}

private val Context.authStore: DataStore<Preferences> by preferencesDataStore(
    name = "brainxp_auth",
)

class AuthDataStore(
    private val store: DataStore<Preferences>,
) {
    val auth: Flow<AuthSnapshot> =
        store.data
            .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
            .map { prefs ->
                AuthSnapshot(
                    accessToken = prefs[KEY_ACCESS],
                    refreshToken = prefs[KEY_REFRESH],
                    subjectId = prefs[KEY_SUBJECT],
                    familyId = prefs[KEY_FAMILY],
                    role = prefs[KEY_ROLE],
                    displayName = prefs[KEY_NAME],
                )
            }

    suspend fun current(): AuthSnapshot = auth.first()

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
    ) {
        store.edit {
            it[KEY_ACCESS] = accessToken
            it[KEY_REFRESH] = refreshToken
        }
    }

    suspend fun saveSubject(subjectId: String) {
        store.edit { it[KEY_SUBJECT] = subjectId }
    }

    suspend fun saveIdentity(
        subjectId: String?,
        familyId: String?,
        role: String?,
    ) {
        store.edit { prefs ->
            subjectId?.let { prefs[KEY_SUBJECT] = it } ?: prefs.remove(KEY_SUBJECT)
            familyId?.let { prefs[KEY_FAMILY] = it } ?: prefs.remove(KEY_FAMILY)
            role?.let { prefs[KEY_ROLE] = it } ?: prefs.remove(KEY_ROLE)
        }
    }

    suspend fun saveDisplayName(name: String) {
        store.edit { it[KEY_NAME] = name }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    companion object {
        val KEY_ACCESS = stringPreferencesKey("access_token")
        val KEY_REFRESH = stringPreferencesKey("refresh_token")
        val KEY_SUBJECT = stringPreferencesKey("subject_id")
        val KEY_FAMILY = stringPreferencesKey("family_id")
        val KEY_ROLE = stringPreferencesKey("role")
        val KEY_NAME = stringPreferencesKey("display_name")

        fun from(context: Context): DataStore<Preferences> = context.authStore
    }
}
