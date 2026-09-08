package com.example.brainxp.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.example.brainxp.data.prefs.AppMode
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.domain.model.DeviceRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class Preferences2 : DataStore<Preferences> {
    private val stored = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = stored

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(stored.value)
        stored.value = updated
        return updated
    }
}

private class RecordingLocalData : LocalData {
    var wipes = 0

    override suspend fun wipe() {
        wipes++
    }
}

class SessionTeardownTest {
    private val auth = AuthDataStore(Preferences2())
    private val settings = SettingsDataStore(Preferences2())
    private val local = RecordingLocalData()
    private val teardown = SessionTeardown(auth, settings, local)

    private suspend fun signedInChild() {
        auth.saveTokens("access", "refresh")
        auth.saveIdentity(subjectId = "subject-1", familyId = "family-1", role = "child")
        settings.setRole(DeviceRole.CHILD)
        settings.setMode(AppMode.FAMILY)
        settings.setOnboardingComplete(true)
        settings.setProtectionEnabled(true)
    }

    @Test
    fun `ending a session leaves no trace of who was signed in`() =
        runTest {
            signedInChild()

            teardown.run()

            val identity = auth.current()
            assertNull(identity.accessToken)
            assertNull(identity.refreshToken)
            assertNull(identity.subjectId)
            assertNull(identity.familyId)
            assertNull(identity.role)
        }

    @Test
    fun `ending a session forgets the role so the next account is not mistaken for the last`() =
        runTest {
            signedInChild()

            teardown.run()

            val snapshot = settings.settings.first()
            assertEquals(DeviceRole.PARENT, snapshot.role)
            assertEquals(AppMode.UNSET, snapshot.mode)
            assertFalse(snapshot.onboardingComplete)
            assertFalse(snapshot.protectionEnabled)
        }

    @Test
    fun `ending a session throws away the cached rows and balance of that account`() =
        runTest {
            signedInChild()

            teardown.run()

            assertEquals(1, local.wipes)
        }

    @Test
    fun `what belongs to the phone rather than the account survives`() =
        runTest {
            signedInChild()
            settings.setPermissionSetupComplete(true)

            teardown.run()

            assertTrue(settings.settings.first().permissionSetupComplete)
        }
}
