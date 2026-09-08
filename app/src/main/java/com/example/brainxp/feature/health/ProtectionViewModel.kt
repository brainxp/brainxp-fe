package com.example.brainxp.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.blocking.ProtectionStateHolder
import com.example.brainxp.blocking.ProtectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProtectionUiState(
    val status: ProtectionStatus = ProtectionStatus.OFF,
)

@HiltViewModel
class ProtectionViewModel
    @Inject
    constructor(
        protection: ProtectionStateHolder,
    ) : ViewModel() {
        val state: StateFlow<ProtectionUiState> =
            protection.snapshot
                .map { ProtectionUiState(status = it.status) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(TIMEOUT), ProtectionUiState())

        private companion object {
            const val TIMEOUT = 5_000L
        }
    }
