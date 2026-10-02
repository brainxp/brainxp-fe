package com.example.brainxp.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.example.brainxp.core.network.AuthTokenStore
import com.example.brainxp.core.network.AuthTokens
import com.example.brainxp.core.network.TokenDto
import com.example.brainxp.data.prefs.AuthDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class MemoryPreferences : DataStore<Preferences> {
    private val stored = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = stored

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(stored.value)
        stored.value = updated
        return updated
    }
}

private class CachedTokens(
    var held: AuthTokens?,
) : AuthTokenStore {
    override fun current(): AuthTokens? = held

    override fun update(tokens: AuthTokens?) {
        held = tokens
    }

    override fun forget() {
        held = null
    }
}

class PairSessionTest {
    @Test
    fun `adopting a pairing token replaces the token requests are sent with`() =
        runTest {
            val auth = AuthDataStore(MemoryPreferences())
            val cache = CachedTokens(AuthTokens("parent-access", "parent-refresh"))
            val session = DeviceSession(auth, cache)

            session.adopt(
                TokenDto(
                    accessToken = "child-access",
                    refreshToken = "child-refresh",
                    expiresIn = 900,
                    subjectId = "child-1",
                    familyId = "family-1",
                    role = "child",
                ),
            )

            assertEquals(AuthTokens("child-access", "child-refresh"), cache.current())
            assertEquals("child-access", auth.current().accessToken)
            assertEquals("child-1", auth.current().subjectId)
        }
}
