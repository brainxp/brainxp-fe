package com.example.brainxp.blocking

import com.example.brainxp.core.permission.PermissionStateProvider
import com.example.brainxp.core.permission.SpecialPermission
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.data.repo.RestrictionRepository
import com.example.brainxp.di.AppScope
import com.example.brainxp.domain.UnlockSessionManager
import com.example.brainxp.domain.model.RestrictionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class ProtectionStatus {
    OFF,
    ACTIVE,
    DEGRADED,
}

data class ProtectionSnapshot(
    val status: ProtectionStatus = ProtectionStatus.OFF,
    val restriction: RestrictionState = RestrictionState(),
    val missingPermissions: List<SpecialPermission> = emptyList(),
) {
    val degraded: Boolean get() = status == ProtectionStatus.DEGRADED
}

@Singleton
class ProtectionStateHolder
    @Inject
    constructor(
        restrictions: RestrictionRepository,
        private val unlocks: UnlockSessionManager,
        private val health: GuardianHealthReporter,
        permissions: PermissionStateProvider,
        settings: SettingsDataStore,
        @AppScope private val scope: CoroutineScope,
    ) {
        val snapshot: StateFlow<ProtectionSnapshot> =
            combine(
                restrictions.observeRestricted().map { apps ->
                    apps.filter { it.enabled }.map { it.packageName }.toSet()
                },
                unlocks.state,
                permissions.state,
                settings.settings.map { it.protectionEnabled }.distinctUntilChanged(),
            ) { packages, unlock, permissionState, enabled ->
                val missing = permissionState.missingRequired
                ProtectionSnapshot(
                    status =
                        when {
                            !enabled -> ProtectionStatus.OFF
                            missing.isNotEmpty() -> ProtectionStatus.DEGRADED
                            else -> ProtectionStatus.ACTIVE
                        },
                    restriction = RestrictionState(packages, unlock),
                    missingPermissions = missing,
                )
            }.stateIn(scope, SharingStarted.Eagerly, ProtectionSnapshot())

        init {
            reportStatusTransitions()
        }

        suspend fun reloadUnlock() {
            unlocks.refresh()
        }

        private fun reportStatusTransitions() {
            scope.launch {
                snapshot
                    .map { it.status }
                    .distinctUntilChanged()
                    .collect { status -> health.report(status) }
            }
        }
    }
