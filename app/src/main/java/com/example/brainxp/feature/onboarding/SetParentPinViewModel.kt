package com.example.brainxp.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.data.prefs.ParentPinStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetParentPinViewModel
    @Inject
    constructor(
        private val pins: ParentPinStore,
    ) : ViewModel() {
        private val mutableSaved = MutableStateFlow(false)
        val saved: StateFlow<Boolean> = mutableSaved.asStateFlow()

        fun set(pin: String) {
            viewModelScope.launch {
                pins.set(pin)
                mutableSaved.value = true
            }
        }
    }
