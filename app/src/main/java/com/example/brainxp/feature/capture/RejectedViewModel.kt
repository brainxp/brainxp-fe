package com.example.brainxp.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.MaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RejectedUiState(
    val assessedLevel: String? = null,
    val declaredLevel: String? = null,
    val reasonCode: String? = null,
    val loading: Boolean = true,
    val error: ApiError? = null,
)

@HiltViewModel
class RejectedViewModel
    @Inject
    constructor(
        private val materials: MaterialRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(RejectedUiState())
        val state: StateFlow<RejectedUiState> = mutableState.asStateFlow()

        private var loadedFor: String? = null

        fun load(materialId: String) {
            if (loadedFor == materialId) return
            loadedFor = materialId
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                mutableState.update {
                    when (val result = materials.detail(materialId)) {
                        is AppResult.Success -> {
                            val material = result.value.material
                            it.copy(
                                assessedLevel = material.assessedLevel,
                                declaredLevel = material.declaredLevel,
                                reasonCode = material.gateReason,
                                loading = false,
                            )
                        }

                        is AppResult.Failure -> {
                            it.copy(loading = false, error = result.error)
                        }
                    }
                }
            }
        }

        fun retry() {
            val materialId = loadedFor ?: return
            loadedFor = null
            load(materialId)
        }
    }
