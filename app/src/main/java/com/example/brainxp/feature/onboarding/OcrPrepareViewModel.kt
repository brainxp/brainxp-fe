package com.example.brainxp.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.ocr.OcrModelInstaller
import com.example.brainxp.core.ocr.OcrModelState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OcrPrepareViewModel
    @Inject
    constructor(
        private val installer: OcrModelInstaller,
    ) : ViewModel() {
        val state: StateFlow<OcrModelState> = installer.state

        init {
            prepare()
        }

        fun prepare() {
            viewModelScope.launch { installer.ensureAvailable() }
        }
    }
