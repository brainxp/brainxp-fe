package com.example.brainxp.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.blocking.ProtectionStateHolder
import com.example.brainxp.blocking.ProtectionStatus
import com.example.brainxp.domain.ProtectionSwitch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProtectionUiState(
    val status: ProtectionStatus = ProtectionStatus.OFF,
    val blocked: Boolean = false,
)

@HiltViewModel
class ProtectionViewModel
    @Inject
    constructor(
        private val protectionSwitch: ProtectionSwitch,
        protection: ProtectionStateHolder,
    ) : ViewModel() {
        private val gate = MutableStateFlow(ProtectionUiState())

        val state: StateFlow<ProtectionUiState> =
            combine(protection.snapshot.map { it.status }, gate) { status, asked ->
                asked.copy(status = status)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(TIMEOUT), ProtectionUiState())

        fun toggle() {
            viewModelScope.launch {
                gate.update { it.copy(blocked = !protectionSwitch.toggle()) }
            }
        }

        private companion object {
            const val TIMEOUT = 5_000L
        }
    }
