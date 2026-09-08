package com.example.brainxp.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.MaterialRepository
import com.example.brainxp.domain.model.Material
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MaterialDetailUiState(
    val material: Material? = null,
    val loading: Boolean = true,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val error: ApiError? = null,
)

@HiltViewModel
class MaterialDetailViewModel
    @Inject
    constructor(
        private val materials: MaterialRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(MaterialDetailUiState())
        val state: StateFlow<MaterialDetailUiState> = mutableState.asStateFlow()

        private var loadedFor: String? = null

        fun load(materialId: String) {
            if (loadedFor == materialId) return
            loadedFor = materialId
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                val result = materials.detail(materialId)
                mutableState.update {
                    when (result) {
                        is AppResult.Success -> it.copy(material = result.value, loading = false)
                        is AppResult.Failure -> it.copy(loading = false, error = result.error)
                    }
                }
            }
        }

        fun retry() {
            val materialId = loadedFor ?: return
            loadedFor = null
            load(materialId)
        }

        fun delete() {
            val materialId = loadedFor ?: return
            if (mutableState.value.deleting) return
            mutableState.update { it.copy(deleting = true, error = null) }
            viewModelScope.launch {
                val result = materials.delete(materialId)
                mutableState.update {
                    when (result) {
                        is AppResult.Success -> it.copy(deleting = false, deleted = true)
                        is AppResult.Failure -> it.copy(deleting = false, error = result.error)
                    }
                }
            }
        }
    }
