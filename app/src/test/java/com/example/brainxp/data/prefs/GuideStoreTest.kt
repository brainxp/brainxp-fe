package com.example.brainxp.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class InMemoryPreferences : DataStore<Preferences> {
    private val stored = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = stored

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(stored.value)
        stored.value = updated
        return updated
    }
}

private class SwitchableAccount(
    var id: String?,
) : GuideAccount {
    override suspend fun accountId(): String? = id
}

class GuideStoreTest {
    private val accounts = SwitchableAccount("subject-a")
    private val guides = GuideStore(InMemoryPreferences(), accounts)

    @Test
    fun `the tutorial counts as unseen until it is remembered`() =
        runTest {
            assertFalse(guides.quizGuideSeen())

            guides.rememberQuizGuide()

            assertTrue(guides.quizGuideSeen())
        }

    @Test
    fun `finishing the tutorial on one account leaves another account on the same device untouched`() =
        runTest {
            guides.rememberQuizGuide()
            assertTrue(guides.quizGuideSeen())

            accounts.id = "subject-b"

            assertFalse("a second account on this device must still be shown the tutorial", guides.quizGuideSeen())
        }

    @Test
    fun `coming back to an account that finished the tutorial does not show it again`() =
        runTest {
            guides.rememberQuizGuide()
            accounts.id = "subject-b"
            guides.rememberQuizGuide()
            accounts.id = "subject-a"

            assertTrue(guides.quizGuideSeen())
        }

    @Test
    fun `a signed out device keeps a flag of its own`() =
        runTest {
            accounts.id = null
            guides.rememberQuizGuide()
            assertTrue(guides.quizGuideSeen())

            accounts.id = "subject-a"

            assertFalse(guides.quizGuideSeen())
        }

    @Test
    fun `each account is keyed separately rather than behind one device wide flag`() {
        assertNotEquals(quizGuideKeyOf("subject-a"), quizGuideKeyOf("subject-b"))
        assertEquals(quizGuideKeyOf("subject-a"), quizGuideKeyOf("subject-a"))
        assertEquals(quizGuideKeyOf(null), quizGuideKeyOf("   "))
    }
}
