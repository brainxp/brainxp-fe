package com.example.brainxp.blocking

import com.example.brainxp.core.result.valueOrNull
import com.example.brainxp.core.time.AppClock
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.data.repo.SessionTeardown
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BindingWatcher
    @Inject
    constructor(
        private val settings: SettingsDataStore,
        private val family: FamilyRepository,
        private val teardown: SessionTeardown,
        private val clock: AppClock,
    ) {
        private val mutex = Mutex()
        private var lastCheckAt: Long? = null

        suspend fun check() {
            mutex.withLock {
                val now = clock.elapsedRealtime()
                val role = settings.settings.first().role
                if (!dueForBindingCheck(role, now, lastCheckAt, THROTTLE_MS)) {
                    return
                }
                lastCheckAt = now
                if (releasesDevice(family.checkBinding().valueOrNull())) {
                    teardown.run()
                }
            }
        }

        private companion object {
            const val THROTTLE_MS = 10_000L
        }
    }
