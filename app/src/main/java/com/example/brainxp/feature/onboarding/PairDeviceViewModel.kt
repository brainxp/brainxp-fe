package com.example.brainxp.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.domain.model.DeviceRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val PAIRING_CODE_DIGITS = 6

data class PairDeviceUiState(
    val digits: String = "",
    val busy: Boolean = false,
    val paired: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class PairDeviceViewModel
    @Inject
    constructor(
        private val family: FamilyRepository,
        private val settings: SettingsDataStore,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(PairDeviceUiState())
        val state: StateFlow<PairDeviceUiState> = mutableState.asStateFlow()

        fun press(digit: Char) {
            val current = mutableState.value
            if (current.busy || current.digits.length >= PAIRING_CODE_DIGITS) return
            val next = current.digits + digit
            mutableState.update { it.copy(digits = next, error = null) }
            if (next.length == PAIRING_CODE_DIGITS) submit(next)
        }

        fun backspace() = mutableState.update { it.copy(digits = it.digits.dropLast(1), error = null) }

        private fun submit(code: String) {
            mutableState.update { it.copy(busy = true, error = null) }
            viewModelScope.launch {
                when (val result = family.pair(code)) {
                    is AppResult.Success -> {
                        settings.setRole(DeviceRole.CHILD)
                        mutableState.update { it.copy(busy = false, paired = true) }
                    }

                    is AppResult.Failure -> {
                        mutableState.update {
                            it.copy(busy = false, digits = "", error = result.error)
                        }
                    }
                }
            }
        }
    }
