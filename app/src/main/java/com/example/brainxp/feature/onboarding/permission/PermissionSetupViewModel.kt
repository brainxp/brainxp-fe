package com.example.brainxp.feature.onboarding.permission

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.permission.PermissionStateProvider
import com.example.brainxp.core.permission.SpecialPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PermissionSetupViewModel
    @Inject
    constructor(
        private val permissions: PermissionStateProvider,
    ) : ViewModel() {
        val state: StateFlow<PermissionSetupUiState> =
            permissions.state
                .map { it.toSetupState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = permissions.state.value.toSetupState(),
                )

        fun settingsIntents(permission: SpecialPermission): List<Intent> = permissions.settingsIntents(permission)

        fun refresh() {
            permissions.refresh()
        }
    }
