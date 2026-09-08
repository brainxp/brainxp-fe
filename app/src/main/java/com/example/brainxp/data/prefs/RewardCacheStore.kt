package com.example.brainxp.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class CachedBalance(
    val balanceSeconds: Int,
    val updatedAtWallClock: Long,
)

interface RewardCache {
    val cached: Flow<CachedBalance?>

    suspend fun write(balance: CachedBalance)

    suspend fun clear()
}

class DataStoreRewardCache(
    private val store: DataStore<Preferences>,
) : RewardCache {
    override val cached: Flow<CachedBalance?> =
        store.data
            .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
            .map { prefs ->
                val seconds = prefs[KEY_BALANCE] ?: return@map null
                CachedBalance(seconds, prefs[KEY_UPDATED_AT] ?: 0L)
            }

    override suspend fun write(balance: CachedBalance) {
        store.edit {
            it[KEY_BALANCE] = balance.balanceSeconds
            it[KEY_UPDATED_AT] = balance.updatedAtWallClock
        }
    }

    override suspend fun clear() {
        store.edit {
            it.remove(KEY_BALANCE)
            it.remove(KEY_UPDATED_AT)
        }
    }

    companion object {
        val KEY_BALANCE = intPreferencesKey("reward_balance_seconds")
        val KEY_UPDATED_AT = longPreferencesKey("reward_balance_updated_at")
    }
}
