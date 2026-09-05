package com.example.brainxp.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.capture.CaptureStore
import com.example.brainxp.core.capture.CapturedPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

data class CaptureUiState(
    val pages: List<CapturedPage> = emptyList(),
    val capturing: Boolean = false,
    val failed: Boolean = false,
) {
    val canContinue: Boolean get() = pages.isNotEmpty() && !capturing
}

fun CaptureUiState.movePage(
    id: String,
    by: Int,
): CaptureUiState {
    val from = pages.indexOfFirst { it.id == id }
    if (from < 0) {
        return this
    }
    val to = (from + by).coerceIn(0, pages.lastIndex)
    return if (from == to) {
        this
    } else {
        copy(pages = pages.toMutableList().apply { add(to, removeAt(from)) })
    }
}

private data class Shot(
    val capturing: Boolean = false,
    val failed: Boolean = false,
)

@HiltViewModel
class CaptureViewModel
    @Inject
    constructor(
        private val store: CaptureStore,
    ) : ViewModel() {
        private val shot = MutableStateFlow(Shot())

        val state: StateFlow<CaptureUiState> =
            combine(store.pages, shot) { pages, current ->
                CaptureUiState(
                    pages = pages,
                    capturing = current.capturing,
                    failed = current.failed,
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, CaptureUiState())

        fun newPageFile(): File = store.newPageFile()

        fun beginCapture() {
            shot.value = Shot(capturing = true)
        }

        fun captured(file: File) {
            store.add(file)
            shot.value = Shot()
        }

        fun captureFailed(file: File) {
            store.deleteFile(file.absolutePath)
            shot.value = Shot(failed = true)
        }

        fun delete(id: String) {
            store.remove(id)
        }

        fun move(
            id: String,
            by: Int,
        ) {
            store.reorder(state.value.movePage(id, by).pages)
        }

        fun discardAll() {
            store.clear()
        }
    }
