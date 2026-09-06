package com.example.brainxp.feature.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.data.prefs.AuthDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeleteAccountUiState(
    val subjectId: String? = null,
    val family: Boolean = false,
)

@HiltViewModel
class DeleteAccountViewModel
    @Inject
    constructor(
        private val auth: AuthDataStore,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(DeleteAccountUiState())
        val state: StateFlow<DeleteAccountUiState> = mutableState.asStateFlow()

        init {
            viewModelScope.launch {
                val snapshot = auth.current()
                mutableState.update {
                    it.copy(
                        subjectId = snapshot.subjectId,
                        family = snapshot.familyId != null,
                    )
                }
            }
        }
    }
