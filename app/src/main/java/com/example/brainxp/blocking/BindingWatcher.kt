package com.example.brainxp.blocking

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.result.valueOrNull
import com.example.brainxp.core.time.AppClock
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.data.repo.PolicyRepository
import com.example.brainxp.data.repo.RestrictionRepository
import com.example.brainxp.data.repo.SessionTeardown
import com.example.brainxp.domain.model.DeviceRole
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
        private val policySync: ChildPolicySync,
        private val teardown: SessionTeardown,
        private val clock: AppClock,
    ) {
        private val mutex = Mutex()
        private var lastCheckAt: Long? = null

        suspend fun check(force: Boolean = false) {
            mutex.withLock {
                val now = clock.elapsedRealtime()
                val role = settings.settings.first().role
                val due = force || dueForBindingCheck(role, now, lastCheckAt, THROTTLE_MS)
                if (role == DeviceRole.CHILD && due) {
                    lastCheckAt = now
                    val binding = family.checkBinding().valueOrNull()
                    if (binding != null) {
                        if (releasesDevice(binding)) {
                            teardown.run()
                        } else {
                            policySync.sync()
                        }
                    }
                }
            }
        }

        private companion object {
            const val THROTTLE_MS = 10_000L
        }
    }

@Singleton
class ChildPolicySync
    @Inject
    constructor(
        private val policies: PolicyRepository,
        private val restrictions: RestrictionRepository,
        private val installedApps: InstalledAppsSource,
    ) {
        suspend fun sync() {
            val policy = policies.policy()
            if (policy is AppResult.Success) {
                val synced =
                    syncedRestrictedPackages(
                        policy.value.lockedApps,
                        installedApps.launchableApps(),
                        installedApps.protectedPackages(),
                    )
                restrictions.replaceRestricted(synced)
            }
        }
    }

internal fun syncedRestrictedPackages(
    lockedApps: List<String>,
    installedApps: List<InstalledApp>,
    protectedPackages: ProtectedPackages,
): List<String> {
    val selectable = SystemCriticalFilter.selectable(installedApps, protectedPackages).map { it.packageName }.toSet()
    return lockedApps.filter { it in selectable }.distinct()
}
