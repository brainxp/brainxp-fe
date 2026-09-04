package com.example.brainxp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.blocking.InstalledAppsSource
import com.example.brainxp.blocking.ProtectionStateHolder
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.data.repo.BalanceSource
import com.example.brainxp.data.repo.ReconciledBalance
import com.example.brainxp.data.repo.RestrictionRepository
import com.example.brainxp.data.repo.RewardReconciler
import com.example.brainxp.domain.UnlockSessionManager
import com.example.brainxp.domain.model.BlockReason
import com.example.brainxp.domain.remainingFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val reconciler: RewardReconciler,
        private val unlocks: UnlockSessionManager,
        private val restrictions: RestrictionRepository,
        private val installedApps: InstalledAppsSource,
        private val settings: SettingsDataStore,
        protection: ProtectionStateHolder,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(HomeUiState())
        val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

        private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)
        val effects: Flow<HomeEffect> = effectChannel.receiveAsFlow()

        private val labels = MutableStateFlow<Map<String, String>>(emptyMap())

        init {
            viewModelScope.launch { reconciler.reconcile() }
            viewModelScope.launch { loadLabels() }
            viewModelScope.launch {
                combine(
                    reconciler.state,
                    unlocks.state,
                    unlocks.remainingFlow(),
                    protection.snapshot,
                    combine(restrictions.observeRestricted(), labels) { apps, names ->
                        apps.filter { it.enabled }.map { app ->
                            LockedApp(app.packageName, names[app.packageName] ?: app.packageName)
                        }
                    },
                ) { balance, unlock, remaining, snapshot, lockedApps ->
                    val standing = balance.standing
                    HomeUiState(
                        phase = phaseFor(balance),
                        balanceSeconds = balance.balanceSeconds,
                        spentTodaySeconds = standing?.spentTodaySeconds ?: 0,
                        dailyCapSeconds = standing?.dailyCapSeconds ?: 0,
                        secondsUntilReset = standing?.secondsUntilReset ?: 0,
                        streakDays = standing?.streakCurrent ?: 0,
                        blockReason = standing?.blockReason ?: BlockReason.NONE,
                        balanceStale = balance.stale,
                        unlock = unlock,
                        remaining = remaining,
                        protection = snapshot.status,
                        lockedApps = lockedApps,
                    )
                }.collect { mutableState.value = it }
            }
        }

        fun onEvent(event: HomeEvent) {
            when (event) {
                HomeEvent.Retry -> viewModelScope.launch { reconciler.reconcile() }
                HomeEvent.StartEarning -> emit(HomeEffect.OpenAddMaterial)
                HomeEvent.FixPermissions -> emit(HomeEffect.OpenPermissionSetup)
                HomeEvent.OpenLibrary -> emit(HomeEffect.OpenLibrary)
                HomeEvent.OpenProgress -> emit(HomeEffect.OpenProgress)
                HomeEvent.EndUnlockEarly -> viewModelScope.launch { unlocks.endEarly() }
                HomeEvent.ToggleProtection -> toggleProtection()
                is HomeEvent.OpenApp -> emit(HomeEffect.LaunchApp(event.packageName))
            }
        }

        private fun toggleProtection() {
            viewModelScope.launch {
                val current = settings.settings.first().protectionEnabled
                settings.setProtectionEnabled(!current)
            }
        }

        private suspend fun loadLabels() {
            labels.value = installedApps.launchableApps().associate { it.packageName to it.label }
        }

        private fun emit(effect: HomeEffect) {
            viewModelScope.launch { effectChannel.send(effect) }
        }

        private fun phaseFor(balance: ReconciledBalance): HomeUiState.Phase {
            val error = balance.error
            return when {
                balance.source == BalanceSource.NONE && error != null -> {
                    HomeUiState.Phase.Error(error, error.retryable)
                }

                balance.source == BalanceSource.NONE -> {
                    HomeUiState.Phase.Loading
                }

                else -> {
                    HomeUiState.Phase.Ready
                }
            }
        }
    }
