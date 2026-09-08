package com.example.brainxp.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.blocking.InstalledApp
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.AppInventoryRepository
import com.example.brainxp.data.repo.PolicyRepository
import com.example.brainxp.domain.model.DeviceApp
import com.example.brainxp.feature.apps.AppPickerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChildAppsViewModel
    @Inject
    constructor(
        private val inventory: AppInventoryRepository,
        private val policies: PolicyRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(AppPickerUiState())
        val state: StateFlow<AppPickerUiState> = mutableState.asStateFlow()

        private var childId: String? = null

        fun load(subjectId: String) {
            if (childId == subjectId) return
            childId = subjectId
            mutableState.update { it.copy(loading = true) }
            viewModelScope.launch { fetch(subjectId) }
        }

        fun search(query: String) = mutableState.update { it.copy(query = query) }

        fun toggle(packageName: String) {
            val subject = childId ?: return
            val current = mutableState.value
            val wanted = packageName !in current.restricted
            mutableState.value = current.copy(restricted = current.restricted.flip(packageName))
            viewModelScope.launch {
                val result = policies.setAppLocked(packageName, wanted, subject)
                if (result is AppResult.Failure) {
                    mutableState.update { state -> state.copy(restricted = state.restricted.flip(packageName)) }
                }
            }
        }

        private suspend fun fetch(subjectId: String) {
            val reported = inventory.inventoryOf(subjectId)
            mutableState.update { state ->
                when (reported) {
                    is AppResult.Success -> {
                        state.copy(
                            loading = false,
                            apps = reported.value.map(DeviceApp::asInstalled),
                            restricted =
                                reported.value
                                    .filter { it.locked }
                                    .map { it.packageName }
                                    .toSet(),
                        )
                    }

                    is AppResult.Failure -> {
                        state.copy(loading = false)
                    }
                }
            }
        }
    }

private fun Set<String>.flip(value: String): Set<String> = if (value in this) this - value else this + value

private fun DeviceApp.asInstalled(): InstalledApp =
    InstalledApp(
        packageName = packageName,
        label = label,
    )
