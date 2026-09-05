package com.example.brainxp.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.capture.CaptureStore
import com.example.brainxp.core.ocr.OcrEngine
import com.example.brainxp.core.ocr.OcrModelInstaller
import com.example.brainxp.core.ocr.PageText
import com.example.brainxp.core.time.AppClock
import com.example.brainxp.data.repo.DraftPage
import com.example.brainxp.data.repo.OcrDraftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OcrReviewViewModel
    @Inject
    constructor(
        private val engine: OcrEngine,
        private val store: CaptureStore,
        private val installer: OcrModelInstaller,
        private val drafts: OcrDraftRepository,
        private val clock: AppClock,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(OcrReviewUiState())
        val state: StateFlow<OcrReviewUiState> = mutableState.asStateFlow()

        private val edits = MutableSharedFlow<DraftPage>(extraBufferCapacity = EDIT_BUFFER)

        @OptIn(FlowPreview::class)
        private fun observeEdits(draftId: String) {
            viewModelScope.launch {
                edits.debounce(AUTOSAVE_DEBOUNCE_MS).collect { page ->
                    mutableState.value = mutableState.value.copy(saving = true)
                    drafts.savePage(draftId, page, clock.wallClock())
                    mutableState.value =
                        mutableState.value.copy(saving = false, savedAtLeastOnce = true)
                }
            }
        }

        fun start(draftId: String) {
            if (mutableState.value.draftId == draftId) {
                return
            }
            mutableState.value = OcrReviewUiState(draftId = draftId, reading = true)
            observeEdits(draftId)

            viewModelScope.launch {
                val stored = drafts.load(draftId)
                if (stored.isNotEmpty()) {
                    mutableState.value =
                        mutableState.value.copy(
                            pages = stored.map { ReviewPage(it.pageIndex, it.text) },
                            reading = false,
                            savedAtLeastOnce = true,
                        )
                    return@launch
                }

                installer.ensureAvailable()
                val batch =
                    engine.extractBatch(
                        store.pages.value.mapIndexed { index, page ->
                            index.toString() to page.path
                        },
                    )
                val reviewed =
                    batch.pages.mapIndexed { index, result ->
                        when (result) {
                            is PageText.Extracted -> ReviewPage(index, result.text)
                            is PageText.Failed -> ReviewPage(index, "", result.reason)
                        }
                    }
                reviewed.forEach {
                    drafts.savePage(draftId, DraftPage(it.pageIndex, it.text), clock.wallClock())
                }
                mutableState.value =
                    mutableState.value.copy(
                        pages = reviewed,
                        reading = false,
                        savedAtLeastOnce = reviewed.isNotEmpty(),
                    )
            }
        }

        fun edit(text: String) {
            val page = mutableState.value.current ?: return
            mutableState.value = mutableState.value.withText(page.pageIndex, text)
            edits.tryEmit(DraftPage(page.pageIndex, text))
        }

        fun goTo(index: Int) {
            mutableState.value = mutableState.value.copy(index = index)
        }

        fun discard() {
            val draftId = mutableState.value.draftId
            viewModelScope.launch { drafts.discard(draftId) }
        }

        private companion object {
            const val AUTOSAVE_DEBOUNCE_MS = 600L
            const val EDIT_BUFFER = 32
        }
    }
