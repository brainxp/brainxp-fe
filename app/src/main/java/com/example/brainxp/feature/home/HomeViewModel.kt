package com.example.brainxp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.blocking.ProtectionStateHolder
import com.example.brainxp.data.repo.BalanceSource
import com.example.brainxp.data.repo.ReconciledBalance
import com.example.brainxp.data.repo.RewardReconciler
import com.example.brainxp.domain.UnlockSessionManager
import com.example.brainxp.domain.model.BlockReason
import com.example.brainxp.domain.model.UnlockState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val reconciler: RewardReconciler,
        private val unlocks: UnlockSessionManager,
        private val lockedApps: LockedAppsSource,
        private val progress: StudyProgressSource,
        private val profileName: ProfileNameSource,
        protection: ProtectionStateHolder,
    ) : ViewModel() {
        private companion object {
            val NAVIGATION =
                mapOf<HomeEvent, HomeEffect>(
                    HomeEvent.FixPermissions to HomeEffect.OpenPermissionSetup,
                    HomeEvent.OpenProgress to HomeEffect.OpenProgress,
                    HomeEvent.OpenPreparing to HomeEffect.OpenPreparing,
                    HomeEvent.OpenApps to HomeEffect.OpenApps,
                    HomeEvent.OpenNotifications to HomeEffect.OpenNotifications,
                )
            const val MILLIS_PER_SECOND = 1_000L
        }

        private val mutableState = MutableStateFlow(HomeUiState())
        val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

        private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)
        val effects: Flow<HomeEffect> = effectChannel.receiveAsFlow()

        init {
            viewModelScope.launch { reconciler.reconcile() }
            viewModelScope.launch { progress.refresh() }
            viewModelScope.launch { profileName.refresh() }
            viewModelScope.launch {
                profileName.name.collect { name ->
                    mutableState.update { it.copy(displayName = name) }
                }
            }
            viewModelScope.launch {
                progress.observe().collect { study ->
                    mutableState.update {
                        it.copy(
                            pending = study.pending,
                            preparing = study.preparing,
                            unreadNotifications = study.unreadNotifications,
                        )
                    }
                }
            }
            viewModelScope.launch {
                combine(
                    reconciler.state,
                    unlocks.state,
                    unlocks.remainingFlow,
                    protection.snapshot,
                    lockedApps.observe(),
                ) { balance, unlock, remaining, snapshot, locked ->
                    val standing = balance.standing
                    val consumedSeconds =
                        ((unlock as? UnlockState.Active)?.consumedMillis ?: 0L) / MILLIS_PER_SECOND
                    val liveBalance = (balance.balanceSeconds - consumedSeconds).coerceAtLeast(0).toInt()
                    HomeUiState(
                        phase = phaseFor(balance),
                        balanceSeconds = liveBalance,
                        spentTodaySeconds = standing?.spentTodaySeconds ?: 0,
                        dailyCapSeconds = standing?.dailyCapSeconds ?: 0,
                        secondsUntilReset = standing?.secondsUntilReset ?: 0,
                        streakDays = standing?.streakCurrent ?: 0,
                        blockReason = standing?.blockReason ?: BlockReason.NONE,
                        balanceStale = balance.stale,
                        unlock = unlock,
                        remaining = remaining,
                        protection = snapshot.status,
                        lockedApps = locked.apps,
                        managed = locked.managed,
                        consumedSeconds = consumedSeconds.toInt(),
                        idleDays = standing?.idleDays ?: 0,
                        idleDaysAllowed = standing?.idleDaysAllowed ?: 0,
                    )
                }.collect { fresh -> mutableState.update { now -> now.mergedWith(fresh) } }
            }
        }

        fun onEvent(event: HomeEvent) {
            NAVIGATION[event]?.let { destination ->
                emit(destination)
                return
            }
            when (event) {
                HomeEvent.Retry -> viewModelScope.launch { reconciler.reconcile() }
                HomeEvent.EndUnlockEarly -> viewModelScope.launch { unlocks.endEarly() }
                is HomeEvent.OpenApp -> emit(HomeEffect.LaunchApp(event.packageName))
                is HomeEvent.Resume -> emit(HomeEffect.OpenQuestions(event.materialId))
                else -> Unit
            }
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

private fun HomeUiState.mergedWith(fresh: HomeUiState): HomeUiState =
    fresh.copy(
        unreadNotifications = unreadNotifications,
        pending = pending,
        preparing = preparing,
        displayName = displayName,
    )
