package com.example.brainxp.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DataStoreTest {
    private lateinit var file: File
    private lateinit var store: DataStore<Preferences>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        file = File(context.filesDir, "test-${System.nanoTime()}.preferences_pb")
        store = PreferenceDataStoreFactory.create { file }
    }

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun settingsStartAtSaneDefaults() =
        runTest {
            val snapshot = SettingsDataStore(store).settings.first()

            assertEquals(AppMode.UNSET, snapshot.mode)
            assertEquals(DetectorChoice.USAGE_STATS, snapshot.detector)
            assertFalse(snapshot.onboardingComplete)
        }

    @Test
    fun settingsRoundTripThroughTheFlow() =
        runTest {
            val settings = SettingsDataStore(store)

            settings.setMode(AppMode.FAMILY)
            settings.setOnboardingComplete(true)
            settings.setDetector(DetectorChoice.ACCESSIBILITY)

            val snapshot = settings.settings.first()
            assertEquals(AppMode.FAMILY, snapshot.mode)
            assertEquals(DetectorChoice.ACCESSIBILITY, snapshot.detector)
            assertTrue(snapshot.onboardingComplete)
        }

    @Test
    fun unrecognisedStoredEnumFallsBackInsteadOfThrowing() =
        runTest {
            store.edit { it[stringPreferencesKey("mode")] = "SOMETHING_ELSE" }

            assertEquals(AppMode.UNSET, SettingsDataStore(store).settings.first().mode)
        }

    @Test
    fun clearingSettingsReturnsToDefaults() =
        runTest {
            val settings = SettingsDataStore(store)
            settings.setMode(AppMode.SELF)

            settings.clear()

            assertEquals(AppMode.UNSET, settings.settings.first().mode)
        }

    @Test
    fun authStartsUnauthenticated() =
        runTest {
            val snapshot = AuthDataStore(store).current()

            assertNull(snapshot.accessToken)
            assertFalse(snapshot.isAuthenticated)
        }

    @Test
    fun tokensAndIdentityRoundTrip() =
        runTest {
            val auth = AuthDataStore(store)

            auth.saveTokens("access-1", "refresh-1")
            auth.saveIdentity(subjectId = "subj-1", familyId = "fam-1", role = "child")

            val snapshot = auth.current()
            assertEquals("access-1", snapshot.accessToken)
            assertEquals("refresh-1", snapshot.refreshToken)
            assertEquals("subj-1", snapshot.subjectId)
            assertEquals("fam-1", snapshot.familyId)
            assertEquals("child", snapshot.role)
            assertTrue(snapshot.isAuthenticated)
        }

    @Test
    fun savingNullIdentityRemovesThePreviousValue() =
        runTest {
            val auth = AuthDataStore(store)
            auth.saveIdentity(subjectId = "subj-1", familyId = "fam-1", role = "child")

            auth.saveIdentity(subjectId = null, familyId = null, role = null)

            val snapshot = auth.current()
            assertNull(snapshot.subjectId)
            assertNull(snapshot.familyId)
            assertNull(snapshot.role)
        }

    @Test
    fun clearingAuthLogsOut() =
        runTest {
            val auth = AuthDataStore(store)
            auth.saveTokens("access-1", "refresh-1")

            auth.clear()

            assertFalse(auth.current().isAuthenticated)
        }
}
