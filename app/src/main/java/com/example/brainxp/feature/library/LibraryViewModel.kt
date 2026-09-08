package com.example.brainxp.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.MaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val materials: MaterialRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(LibraryUiState())
        val state: StateFlow<LibraryUiState> = mutableState.asStateFlow()

        private val effectChannel = Channel<LibraryEffect>(Channel.BUFFERED)
        val effects: Flow<LibraryEffect> = effectChannel.receiveAsFlow()

        init {
            observeCache()
            refresh()
        }

        private fun observeCache() {
            viewModelScope.launch {
                materials.observeCached().collect { cached ->
                    if (cached.isEmpty() && mutableState.value.items.isEmpty()) {
                        return@collect
                    }
                    mutableState.value =
                        mutableState.value.copy(
                            phase = LibraryUiState.Phase.Ready,
                            items = cached,
                        )
                }
            }
        }

        private fun refresh() {
            viewModelScope.launch {
                when (val result = materials.page(cursor = null)) {
                    is AppResult.Success -> {
                        mutableState.value =
                            mutableState.value.copy(
                                phase = LibraryUiState.Phase.Ready,
                                items = result.value.items,
                            )
                    }

                    is AppResult.Failure -> {
                        if (mutableState.value.items.isEmpty()) {
                            mutableState.value =
                                mutableState.value.copy(
                                    phase =
                                        LibraryUiState.Phase.Error(
                                            result.error,
                                            result.error.retryable,
                                        ),
                                )
                        }
                    }
                }
            }
        }

        fun onEvent(event: LibraryEvent) {
            when (event) {
                LibraryEvent.Retry -> refresh()
                is LibraryEvent.Study -> emit(LibraryEffect.OpenMaterial(event.materialId))
                is LibraryEvent.Remove -> remove(event.materialId)
            }
        }

        private fun remove(materialId: String) {
            if (mutableState.value.removing != null) {
                return
            }
            mutableState.value = mutableState.value.copy(removing = materialId)
            viewModelScope.launch {
                when (val result = materials.delete(materialId)) {
                    is AppResult.Success -> {
                        mutableState.value =
                            mutableState.value.copy(
                                items = mutableState.value.items.filterNot { it.id == materialId },
                                removing = null,
                            )
                    }

                    is AppResult.Failure -> {
                        mutableState.value = mutableState.value.copy(removing = null)
                        emit(LibraryEffect.RemoveFailed(result.error))
                    }
                }
            }
        }

        private fun emit(effect: LibraryEffect) {
            viewModelScope.launch { effectChannel.send(effect) }
        }
    }
