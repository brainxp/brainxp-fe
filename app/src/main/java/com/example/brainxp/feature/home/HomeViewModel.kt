package com.example.brainxp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.blocking.ProtectionStateHolder
import com.example.brainxp.data.repo.BalanceSource
import com.example.brainxp.data.repo.ReconciledBalance
import com.example.brainxp.data.repo.RewardReconciler
import com.example.brainxp.domain.UnlockSessionManager
import com.example.brainxp.domain.remainingFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val reconciler: RewardReconciler,
        private val unlocks: UnlockSessionManager,
        protection: ProtectionStateHolder,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(HomeUiState())
        val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

        private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)
        val effects: Flow<HomeEffect> = effectChannel.receiveAsFlow()

        init {
            viewModelScope.launch { reconciler.reconcile() }
            viewModelScope.launch {
                combine(
                    reconciler.state,
                    unlocks.state,
                    unlocks.remainingFlow(),
                    protection.snapshot,
                ) { balance, unlock, remaining, snapshot ->
                    HomeUiState(
                        phase = phaseFor(balance),
                        rewardMinutes = balance.balanceSeconds / SECONDS_PER_MINUTE,
                        balanceStale = balance.stale,
                        unlock = unlock,
                        remaining = remaining,
                        protection = snapshot.status,
                    )
                }.collect { mutableState.value = it }
            }
        }

        fun onEvent(event: HomeEvent) {
            when (event) {
                HomeEvent.Retry -> viewModelScope.launch { reconciler.reconcile() }
                HomeEvent.StartEarning -> emit(HomeEffect.OpenAddMaterial)
                HomeEvent.FixPermissions -> emit(HomeEffect.OpenPermissionSetup)
                HomeEvent.EndUnlockEarly -> viewModelScope.launch { unlocks.endEarly() }
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

        private companion object {
            const val SECONDS_PER_MINUTE = 60
        }
    }
