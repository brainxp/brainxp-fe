package com.example.brainxp.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.capture.CaptureStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CapturedPage(
    val id: String,
    val path: String,
)

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

@HiltViewModel
class CaptureViewModel
    @Inject
    constructor(
        private val store: CaptureStore,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(CaptureUiState())
        val state: StateFlow<CaptureUiState> = mutableState.asStateFlow()

        fun newPageFile(): File = store.newPageFile()

        fun beginCapture() {
            mutableState.value = mutableState.value.copy(capturing = true, failed = false)
        }

        fun captured(file: File) {
            mutableState.value =
                mutableState.value.copy(
                    pages = mutableState.value.pages + CapturedPage(file.name, file.absolutePath),
                    capturing = false,
                )
        }

        fun captureFailed(file: File) {
            store.delete(file.absolutePath)
            mutableState.value = mutableState.value.copy(capturing = false, failed = true)
        }

        fun delete(id: String) {
            val page = mutableState.value.pages.firstOrNull { it.id == id } ?: return
            store.delete(page.path)
            mutableState.value =
                mutableState.value.copy(pages = mutableState.value.pages.filterNot { it.id == id })
        }

        fun move(
            id: String,
            by: Int,
        ) {
            mutableState.value = mutableState.value.movePage(id, by)
        }

        fun discardAll() {
            viewModelScope.launch {
                mutableState.value.pages.forEach { store.delete(it.path) }
                mutableState.value = CaptureUiState()
            }
        }
    }
