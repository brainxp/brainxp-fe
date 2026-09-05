package com.example.brainxp.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.brainxp.domain.model.DeviceRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class AppMode {
    UNSET,
    SELF,
    FAMILY,
}

enum class DetectorChoice {
    USAGE_STATS,
    ACCESSIBILITY,
}

data class SettingsSnapshot(
    val mode: AppMode = AppMode.UNSET,
    val onboardingComplete: Boolean = false,
    val permissionSetupComplete: Boolean = false,
    val ocrModelReady: Boolean = false,
    val detector: DetectorChoice = DetectorChoice.USAGE_STATS,
    val protectionEnabled: Boolean = false,
    val warningLeadSeconds: Int = DEFAULT_WARNING_LEAD_SECONDS,
    val role: DeviceRole = DeviceRole.PARENT,
    val parentPinHash: String? = null,
)

const val DEFAULT_WARNING_LEAD_SECONDS = 60

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "brainxp_settings",
)

class SettingsDataStore(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<SettingsSnapshot> =
        store.data
            .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
            .map { prefs ->
                SettingsSnapshot(
                    mode = prefs[KEY_MODE]?.toEnum(AppMode.UNSET) ?: AppMode.UNSET,
                    onboardingComplete = prefs[KEY_ONBOARDING] ?: false,
                    permissionSetupComplete = prefs[KEY_PERMISSIONS] ?: false,
                    ocrModelReady = prefs[KEY_OCR_READY] ?: false,
                    detector =
                        prefs[KEY_DETECTOR]?.toEnum(DetectorChoice.USAGE_STATS)
                            ?: DetectorChoice.USAGE_STATS,
                    protectionEnabled = prefs[KEY_PROTECTION] ?: false,
                    warningLeadSeconds = prefs[KEY_WARNING_LEAD] ?: DEFAULT_WARNING_LEAD_SECONDS,
                    role = prefs[KEY_ROLE]?.toEnum(DeviceRole.PARENT) ?: DeviceRole.PARENT,
                    parentPinHash = prefs[KEY_PARENT_PIN],
                )
            }

    suspend fun setRole(role: DeviceRole) {
        store.edit { it[KEY_ROLE] = role.name }
    }

    suspend fun setMode(mode: AppMode) {
        store.edit { it[KEY_MODE] = mode.name }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        store.edit { it[KEY_ONBOARDING] = complete }
    }

    suspend fun setPermissionSetupComplete(complete: Boolean) {
        store.edit { it[KEY_PERMISSIONS] = complete }
    }

    suspend fun setOcrModelReady(ready: Boolean) {
        store.edit { it[KEY_OCR_READY] = ready }
    }

    suspend fun setDetector(choice: DetectorChoice) {
        store.edit { it[KEY_DETECTOR] = choice.name }
    }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        store.edit { it[KEY_PROTECTION] = enabled }
    }

    suspend fun setWarningLeadSeconds(seconds: Int) {
        store.edit { it[KEY_WARNING_LEAD] = seconds }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    private inline fun <reified T : Enum<T>> String.toEnum(fallback: T): T =
        runCatching {
            enumValueOf<T>(this)
        }.getOrDefault(fallback)

    companion object {
        val KEY_MODE = stringPreferencesKey("mode")
        val KEY_ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val KEY_PERMISSIONS = booleanPreferencesKey("permission_setup_complete")
        val KEY_OCR_READY = booleanPreferencesKey("ocr_model_ready")
        val KEY_DETECTOR = stringPreferencesKey("detector")
        val KEY_PROTECTION = booleanPreferencesKey("protection_enabled")
        val KEY_WARNING_LEAD = intPreferencesKey("warning_lead_seconds")
        val KEY_ROLE = stringPreferencesKey("device_role")
        val KEY_PARENT_PIN = stringPreferencesKey("parent_pin_hash")

        fun from(context: Context): DataStore<Preferences> = context.settingsStore
    }
}
