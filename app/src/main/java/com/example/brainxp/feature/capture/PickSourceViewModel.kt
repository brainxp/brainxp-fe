package com.example.brainxp.feature.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.R
import com.example.brainxp.core.capture.PickRejection
import com.example.brainxp.core.capture.PickResult
import com.example.brainxp.core.capture.PickedFileCache
import com.example.brainxp.core.upload.UploadQueue
import com.example.brainxp.di.IoDispatcher
import com.example.brainxp.domain.model.MaterialType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PickSourceViewModel
    @Inject
    constructor(
        private val cache: PickedFileCache,
        private val uploads: UploadQueue,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) : ViewModel() {
        private val mutableRejection = MutableStateFlow<Int?>(null)
        val rejection: StateFlow<Int?> = mutableRejection.asStateFlow()

        private val mutablePicked = MutableStateFlow<String?>(null)
        val pickedPath: StateFlow<String?> = mutablePicked.asStateFlow()

        fun accept(
            uri: Uri,
            onAccepted: () -> Unit,
        ) {
            mutableRejection.value = null
            viewModelScope.launch {
                when (val result = withContext(io) { cache.copyIn(uri) }) {
                    is PickResult.Accepted -> {
                        mutablePicked.value = result.file.cachedPath
                        uploads.enqueue(result.file.cachedPath, result.file.name, MaterialType.DOCUMENT)
                        onAccepted()
                    }

                    is PickResult.Rejected -> {
                        mutableRejection.value =
                            when (result.reason) {
                                PickRejection.TOO_LARGE -> R.string.source_too_large
                                PickRejection.UNREADABLE -> R.string.source_unreadable
                            }
                    }
                }
            }
        }
    }
