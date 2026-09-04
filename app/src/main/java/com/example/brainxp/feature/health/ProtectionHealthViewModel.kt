package com.example.brainxp.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.blocking.ProtectionStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProtectionHealthViewModel
    @Inject
    constructor(
        holder: ProtectionStateHolder,
    ) : ViewModel() {
        val degraded: StateFlow<Boolean> =
            holder.snapshot
                .map { it.degraded }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = holder.snapshot.value.degraded,
                )
    }
