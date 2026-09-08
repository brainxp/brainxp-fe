package com.example.brainxp.feature.capture

import com.example.brainxp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RejectionReasonTest {
    @Test
    fun `a missing reason falls back to the plain explanation`() {
        assertEquals(R.string.reject_reason_unknown, rejectionReasonRes(null))
    }

    @Test
    fun `material below the declared level is named as such`() {
        assertEquals(R.string.reject_reason_level_too_low, rejectionReasonRes("level_too_low"))
    }

    @Test
    fun `thin material reads the same whichever code the server sends`() {
        assertEquals(R.string.reject_reason_too_thin, rejectionReasonRes("too_thin"))
        assertEquals(R.string.reject_reason_too_thin, rejectionReasonRes("too_short"))
    }

    @Test
    fun `unreadable material is named as such`() {
        assertEquals(R.string.reject_reason_unreadable, rejectionReasonRes("unreadable"))
    }

    @Test
    fun `something that is not study material gets its own explanation`() {
        assertEquals(R.string.reject_reason_not_study, rejectionReasonRes("not_study_material"))
    }

    @Test
    fun `the code is matched whatever case or padding the server uses`() {
        assertEquals(R.string.reject_reason_not_study, rejectionReasonRes("NOT_STUDY_MATERIAL"))
        assertEquals(R.string.reject_reason_not_study, rejectionReasonRes("  not_study_material  "))
    }

    @Test
    fun `a code nobody has seen before never reaches the user raw`() {
        assertEquals(R.string.reject_reason_unknown, rejectionReasonRes("some_future_code"))
    }

    @Test
    fun `server prose is offered as a note only when it carries no raw code`() {
        assertEquals("Fotonya terlalu gelap untuk dibaca.", rejectionNote("Fotonya terlalu gelap untuk dibaca."))
    }

    @Test
    fun `a sentence with a code embedded in it is withheld`() {
        assertNull(rejectionNote("Materi ini ditolak: not_study_material"))
    }

    @Test
    fun `a bare code is never offered as a note`() {
        assertNull(rejectionNote("not_study_material"))
        assertNull(rejectionNote(null))
    }

    @Test
    fun `only a level refusal earns the level comparison and its multiplier table`() {
        assertTrue(isLevelRejection("level_too_low"))
    }

    @Test
    fun `a refusal that has nothing to do with level does not claim it does`() {
        listOf("not_study_material", "unreadable", "too_thin", "too_short", "some_future_code", null)
            .forEach { code ->
                assertFalse("$code was treated as a level refusal", isLevelRejection(code))
            }
    }
}
