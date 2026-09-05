package com.example.brainxp.feature.library

import com.example.brainxp.domain.model.Material
import com.example.brainxp.domain.model.MaterialStatus
import com.example.brainxp.domain.model.MaterialType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun material(id: String) =
    Material(
        id = id,
        title = "Materi $id",
        type = MaterialType.DOCUMENT,
        status = MaterialStatus.READY,
        charCount = 1_000,
        createdAt = 0L,
        sessionCount = 0,
    )

class LibraryCachePhaseTest {
    @Test
    fun `cached items read as ready, never as loading`() {
        val state =
            LibraryUiState(phase = LibraryUiState.Phase.Ready, items = listOf(material("a")))

        assertEquals(LibraryUiState.Phase.Ready, state.phase)
        assertFalse(state.empty)
    }

    @Test
    fun `an empty ready list reads as empty`() {
        assertTrue(LibraryUiState(phase = LibraryUiState.Phase.Ready).empty)
    }

    @Test
    fun `loading is never reported as empty`() {
        assertFalse(LibraryUiState(phase = LibraryUiState.Phase.Loading).empty)
    }

    @Test
    fun `removing marks one material without dropping it yet`() {
        val state =
            LibraryUiState(
                phase = LibraryUiState.Phase.Ready,
                items = listOf(material("a"), material("b")),
                removing = "b",
            )

        assertEquals("b", state.removing)
        assertEquals(2, state.items.size)
    }
}
