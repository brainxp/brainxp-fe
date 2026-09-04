package com.example.brainxp.blocking

import com.example.brainxp.core.permission.PermissionStateProvider
import com.example.brainxp.core.permission.SpecialPermission
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.data.repo.ActivityLogRepository
import com.example.brainxp.data.repo.RestrictionRepository
import com.example.brainxp.data.repo.UnlockRepository
import com.example.brainxp.di.AppScope
import com.example.brainxp.domain.model.ActivityEvent
import com.example.brainxp.domain.model.ActivityKind
import com.example.brainxp.domain.model.RestrictionState
import com.example.brainxp.domain.model.UnlockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
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
        private val restrictions: RestrictionRepository,
        private val unlocks: UnlockRepository,
        private val activityLog: ActivityLogRepository,
        permissions: PermissionStateProvider,
        settings: SettingsDataStore,
        @AppScope private val scope: CoroutineScope,
    ) {
        private val unlockState = MutableStateFlow<UnlockState>(UnlockState.Locked)

        val snapshot: StateFlow<ProtectionSnapshot> =
            combine(
                restrictions.observeRestricted().map { apps ->
                    apps.filter { it.enabled }.map { it.packageName }.toSet()
                },
                unlockState,
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
            reportDegradedTransitions()
        }

        fun reloadUnlock(
            nowWallClock: Long,
            nowElapsed: Long,
        ) {
            scope.launch { unlockState.value = unlocks.activeUnlock(nowWallClock, nowElapsed) }
        }

        fun setUnlock(state: UnlockState) {
            unlockState.value = state
        }

        private fun reportDegradedTransitions() {
            scope.launch {
                snapshot
                    .map { it.status }
                    .distinctUntilChanged()
                    .collect { status ->
                        val kind =
                            when (status) {
                                ProtectionStatus.DEGRADED -> ActivityKind.PROTECTION_DEGRADED
                                ProtectionStatus.OFF -> ActivityKind.PROTECTION_DISABLED
                                ProtectionStatus.ACTIVE -> null
                            }
                        kind?.let {
                            activityLog.record(
                                ActivityEvent(kind = it, timestamp = System.currentTimeMillis()),
                            )
                        }
                    }
            }
        }
    }
