package com.example.brainxp.core.network

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File

class FileStreamRequestBody(
    private val file: File,
    private val contentType: MediaType?,
) : RequestBody() {
    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        file.inputStream().source().use { source ->
            var written = 0L
            while (true) {
                val read = source.read(sink.buffer, CHUNK_BYTES)
                if (read == -1L) {
                    break
                }
                written += read
                sink.flush()
            }
        }
    }

    private companion object {
        const val CHUNK_BYTES = 64L * 1024
    }
}
