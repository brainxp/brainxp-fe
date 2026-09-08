package com.example.brainxp.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.capture.CaptureStore
import com.example.brainxp.core.capture.CapturedPage
import com.example.brainxp.core.time.AppClock
import com.example.brainxp.core.upload.UploadQueue
import com.example.brainxp.domain.model.MaterialType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
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
        private val clock: AppClock,
        private val uploads: UploadQueue,
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

        fun uploadAll(titlePrefix: String) {
            val pages = state.value.pages
            val titles = captureTitles(titlePrefix, stampOf(clock.wallClock()), pages.size)
            pages.forEachIndexed { index, page ->
                uploads.enqueue(page.path, titles[index], MaterialType.PHOTO)
            }
            store.reorder(emptyList())
        }

        fun discardAll() {
            store.clear()
        }
    }

private fun stampOf(wallClock: Long): String =
    Instant
        .ofEpochMilli(wallClock)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(STAMP_PATTERN, Locale.getDefault()))

private const val STAMP_PATTERN = "d MMM HH.mm"
