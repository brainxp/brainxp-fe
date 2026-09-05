package com.example.brainxp.core.capture

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PickedFile(
    val name: String,
    val sizeBytes: Long,
    val cachedPath: String,
)

enum class PickRejection {
    TOO_LARGE,
    UNREADABLE,
}

sealed interface PickResult {
    data class Accepted(
        val file: PickedFile,
    ) : PickResult

    data class Rejected(
        val name: String,
        val reason: PickRejection,
    ) : PickResult
}

const val MAX_UPLOAD_BYTES = 32L * 1024 * 1024

val DOCUMENT_MIME_TYPES =
    arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.oasis.opendocument.text",
        "application/vnd.oasis.opendocument.presentation",
        "text/plain",
        "text/markdown",
        "image/jpeg",
        "image/png",
        "image/webp",
    )

@Singleton
class PickedFileCache
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val root: File
            get() = File(context.cacheDir, DIRECTORY).apply { mkdirs() }

        fun copyIn(uri: Uri): PickResult {
            val metadata = readMetadata(uri)
            if (metadata.second > MAX_UPLOAD_BYTES) {
                return PickResult.Rejected(metadata.first, PickRejection.TOO_LARGE)
            }

            val target = File(root, "${UUID.randomUUID()}-${metadata.first}")
            val copied =
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("stream tidak terbuka")
                }

            return if (copied.isSuccess) {
                PickResult.Accepted(PickedFile(metadata.first, target.length(), target.absolutePath))
            } else {
                target.delete()
                PickResult.Rejected(metadata.first, PickRejection.UNREADABLE)
            }
        }

        fun clear() {
            root.listFiles()?.forEach { it.delete() }
        }

        private fun readMetadata(uri: Uri): Pair<String, Long> {
            val columns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
            context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name =
                        cursor
                            .getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            .takeIf { it >= 0 }
                            ?.let { cursor.getString(it) }
                            ?: FALLBACK_NAME
                    val size =
                        cursor
                            .getColumnIndex(OpenableColumns.SIZE)
                            .takeIf { it >= 0 && !cursor.isNull(it) }
                            ?.let { cursor.getLong(it) }
                            ?: 0L
                    return name to size
                }
            }
            return FALLBACK_NAME to 0L
        }

        private companion object {
            const val DIRECTORY = "picked"
            const val FALLBACK_NAME = "materi"
        }
    }
