package com.example.brainxp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.blocking.ProtectionStateHolder
import com.example.brainxp.data.repo.BalanceSource
import com.example.brainxp.data.repo.ReconciledBalance
import com.example.brainxp.data.repo.RewardReconciler
import com.example.brainxp.domain.ProtectionSwitch
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
        private val protectionSwitch: ProtectionSwitch,
        protection: ProtectionStateHolder,
    ) : ViewModel() {
        private companion object {
            val PRESET_SECONDS = listOf(300, 600, 900, 1_800)
            const val MILLIS_PER_SECOND = 1_000L
        }

        private val mutableState = MutableStateFlow(HomeUiState())
        val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

        private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)
        val effects: Flow<HomeEffect> = effectChannel.receiveAsFlow()

        init {
            viewModelScope.launch { reconciler.reconcile() }
            viewModelScope.launch { progress.refresh() }
            viewModelScope.launch {
                progress.observe().collect { study ->
                    mutableState.update { it.copy(pending = study.pending, preparing = study.preparing) }
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
                    val options = durationOptions(liveBalance)
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
                        sessionOptions = options,
                        consumedSeconds = consumedSeconds.toInt(),
                        idleDays = standing?.idleDays ?: 0,
                        idleDaysAllowed = standing?.idleDaysAllowed ?: 0,
                    )
                }.collect { fresh -> mutableState.update { now -> now.mergedWith(fresh) } }
            }
        }

        fun onEvent(event: HomeEvent) {
            when (event) {
                HomeEvent.Retry -> viewModelScope.launch { reconciler.reconcile() }
                HomeEvent.StartEarning -> emit(HomeEffect.OpenAddMaterial)
                HomeEvent.FixPermissions -> emit(HomeEffect.OpenPermissionSetup)
                HomeEvent.OpenLibrary -> emit(HomeEffect.OpenLibrary)
                HomeEvent.OpenProgress -> emit(HomeEffect.OpenProgress)
                HomeEvent.OpenPreparing -> emit(HomeEffect.OpenPreparing)
                HomeEvent.OpenApps -> emit(HomeEffect.OpenApps)
                HomeEvent.EndUnlockEarly -> viewModelScope.launch { unlocks.endEarly() }
                HomeEvent.ToggleProtection -> toggleProtection()
                is HomeEvent.OpenApp -> emit(HomeEffect.LaunchApp(event.packageName))
                is HomeEvent.Resume -> emit(HomeEffect.OpenQuestions(event.materialId))
                is HomeEvent.SelectDuration -> selectDuration(event.seconds)
                HomeEvent.StartSession -> startSession()
            }
        }

        private fun selectDuration(seconds: Int) {
            mutableState.value = mutableState.value.copy(selectedOption = seconds)
        }

        private fun startSession() {
            val current = mutableState.value
            val seconds = current.selectedOption ?: current.sessionOptions.firstOrNull() ?: return
            val packages = current.lockedApps.map { it.packageName }.toSet()
            if (packages.isEmpty() || current.starting) {
                return
            }

            mutableState.value = current.copy(starting = true)
            viewModelScope.launch {
                unlocks.start(seconds, packages)
                mutableState.value = mutableState.value.copy(starting = false)
            }
        }

        fun submitPin(pin: String) {
            viewModelScope.launch {
                val ok = protectionSwitch.verify(pin)
                mutableState.value =
                    mutableState.value.copy(pinVerified = ok, pinRequired = !ok, pinWrong = !ok)
                if (ok) toggleProtection()
            }
        }

        fun dismissPin() {
            mutableState.value = mutableState.value.copy(pinRequired = false, pinWrong = false)
        }

        private fun toggleProtection() {
            viewModelScope.launch {
                if (!protectionSwitch.toggle(mutableState.value.pinVerified)) {
                    mutableState.value = mutableState.value.copy(pinRequired = true)
                }
            }
        }

        private fun emit(effect: HomeEffect) {
            viewModelScope.launch { effectChannel.send(effect) }
        }

        private fun durationOptions(balanceSeconds: Int): List<Int> {
            if (balanceSeconds <= 0) {
                return emptyList()
            }
            val fitting = PRESET_SECONDS.filter { it <= balanceSeconds }
            return fitting.ifEmpty { listOf(balanceSeconds) }
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
        selectedOption = selectedOption?.takeIf { it in fresh.sessionOptions } ?: fresh.sessionOptions.firstOrNull(),
        starting = starting,
        pending = pending,
        preparing = preparing,
        pinRequired = pinRequired,
        pinVerified = pinVerified,
        pinWrong = pinWrong,
    )
