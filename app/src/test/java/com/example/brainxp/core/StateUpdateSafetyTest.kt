package com.example.brainxp.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

private val CALLERS =
    Regex("""\b(policies|family|api|materials|rewards|account|auth|notifications|restrictions|quiz)\.\w+\(""")

private fun blockEndOf(
    source: String,
    open: Int,
): Int {
    var depth = 0
    for (index in open until source.length) {
        if (source[index] == '{') depth++
        if (source[index] == '}') depth--
        if (depth == 0 && index > open) return index
    }
    return source.length - 1
}

private fun updateBlocksOf(source: String): List<String> =
    Regex("""\.update\s*\{""")
        .findAll(source)
        .map { match ->
            val open = match.range.last
            source.substring(open, blockEndOf(source, open) + 1)
        }.toList()

class StateUpdateSafetyTest {
    @Test
    fun `update runs its body again whenever another writer wins the race`() =
        runTest {
            val state = MutableStateFlow(0)
            var bodyRuns = 0

            val slowWriter =
                launch {
                    state.update { current ->
                        bodyRuns++
                        delay(HALF_SECOND)
                        current + 1
                    }
                }
            advanceTimeBy(QUARTER_SECOND)
            state.update { current -> current + COMPETING }
            advanceUntilIdle()
            slowWriter.join()

            assertEquals("update retries its whole body on a lost race", 2, bodyRuns)
        }

    @Test
    fun `no state update carries a repository call that a retry would repeat`() {
        val offenders =
            File("src/main/java")
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file ->
                    updateBlocksOf(file.readText())
                        .mapNotNull { block -> CALLERS.find(block)?.value?.let { "${file.path}: $it" } }
                }.toList()

        assertTrue(offenders.joinToString("\n"), offenders.isEmpty())
    }

    private companion object {
        const val HALF_SECOND = 500L
        const val QUARTER_SECOND = 250L
        const val COMPETING = 100
    }
}
