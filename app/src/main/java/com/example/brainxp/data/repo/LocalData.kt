package com.example.brainxp.data.repo

import com.example.brainxp.data.db.BrainXPDatabase
import com.example.brainxp.data.prefs.RewardCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface LocalData {
    suspend fun wipe()
}

@Singleton
class StoredLocalData
    @Inject
    constructor(
        private val database: BrainXPDatabase,
        private val rewards: RewardCache,
    ) : LocalData {
        override suspend fun wipe() {
            withContext(Dispatchers.IO) { database.clearAllTables() }
            rewards.clear()
        }
    }
