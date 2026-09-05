package com.example.brainxp.core.network

import okio.Buffer
import okio.BufferedSink
import okio.blackholeSink
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private const val MEGABYTE = 1024 * 1024
private const val LARGE_MEGABYTES = 24

class FileStreamRequestBodyTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun file(megabytes: Int): File {
        val target = folder.newFile("material.bin")
        val chunk = ByteArray(MEGABYTE) { (it % 251).toByte() }
        target.outputStream().use { out -> repeat(megabytes) { out.write(chunk) } }
        return target
    }

    @Test
    fun `content length comes from the file, not from a buffer`() {
        val target = file(1)

        assertEquals(target.length(), FileStreamRequestBody(target, null).contentLength())
    }

    @Test
    fun `every byte reaches the sink`() {
        val target = file(2)
        val sink = Buffer()

        FileStreamRequestBody(target, null).writeTo(sink)

        assertEquals(target.length(), sink.size)
    }

    @Test
    fun `the bytes arrive unchanged`() {
        val target = folder.newFile("small.bin")
        target.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        val sink = Buffer()

        FileStreamRequestBody(target, null).writeTo(sink)

        assertEquals(listOf<Byte>(1, 2, 3, 4, 5), sink.readByteArray().toList())
    }

    @Test
    @Suppress("ExplicitGarbageCollectionCall")
    fun `a large file never lands in memory whole`() {
        val target = file(LARGE_MEGABYTES)
        val drain: BufferedSink = blackholeSink().buffer()
        val runtime = Runtime.getRuntime()

        System.gc()
        val before = runtime.totalMemory() - runtime.freeMemory()
        FileStreamRequestBody(target, null).writeTo(drain)
        drain.flush()
        val peak = runtime.totalMemory() - runtime.freeMemory()

        val grew = peak - before
        assertTrue(
            "heap grew by $grew bytes for a ${target.length()} byte file",
            grew < target.length() / 2,
        )
    }

    @Test
    fun `an empty file writes nothing and reports zero`() {
        val target = folder.newFile("empty.bin")
        val sink = Buffer()

        val body = FileStreamRequestBody(target, null)
        body.writeTo(sink)

        assertEquals(0L, body.contentLength())
        assertEquals(0L, sink.size)
    }
}
