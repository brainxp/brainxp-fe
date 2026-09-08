package com.example.brainxp.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.example.brainxp.core.device.InstallBinding
import com.example.brainxp.core.network.ApiErrorBodyReader
import com.example.brainxp.core.network.BindingCheckDto
import com.example.brainxp.core.network.BindingCheckRequestDto
import com.example.brainxp.core.network.ChildRequestDto
import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.network.FamilyApi
import com.example.brainxp.core.network.HeartbeatRequestDto
import com.example.brainxp.core.network.PairRequestDto
import com.example.brainxp.core.network.PairingCodeDto
import com.example.brainxp.core.network.SelfSubjectRequestDto
import com.example.brainxp.core.network.SubjectDto
import com.example.brainxp.core.network.TokenDto
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.data.prefs.SettingsDataStore
import com.example.brainxp.domain.model.AcademicLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

private open class StubFamilyApi : FamilyApi {
    override suspend fun children(): List<SubjectDto> = emptyList()

    override suspend fun createChild(body: ChildRequestDto): SubjectDto = error("not used")

    override suspend fun createSelfSubject(body: SelfSubjectRequestDto): SubjectDto = error("not used")

    override suspend fun deleteSubject(subjectId: String) = error("not used")

    override suspend fun releaseDevice(subjectId: String) = error("not used")

    override suspend fun pairingCode(subjectId: String): PairingCodeDto = error("not used")

    override suspend fun pair(body: PairRequestDto): TokenDto = error("not used")

    override suspend fun checkBinding(body: BindingCheckRequestDto): BindingCheckDto = error("not used")

    override suspend fun heartbeat(body: HeartbeatRequestDto) = error("not used")
}

private class NoLocalData : LocalData {
    override suspend fun wipe() = Unit
}

class SelfSubjectIdentityTest {
    private val auth = AuthDataStore(InMemoryPreferences())
    private val errors = ErrorMapper(ApiErrorBodyReader(Json { ignoreUnknownKeys = true }))
    private val binding = InstallBinding(InMemoryPreferences())

    private fun repository(api: FamilyApi) =
        NetworkFamilyRepository(
            api = api,
            auth = auth,
            binding = binding,
            errors = errors,
            teardown = SessionTeardown(auth, SettingsDataStore(InMemoryPreferences()), NoLocalData()),
        )

    private val created =
        SubjectDto(id = "subject-7", displayName = "Aku", academicLevel = "sma", kind = "self")

    private val api =
        object : StubFamilyApi() {
            var askedLevel: String? = null

            override suspend fun createSelfSubject(body: SelfSubjectRequestDto): SubjectDto {
                askedLevel = body.academicLevel
                return created
            }
        }

    @Test
    fun `creating the self subject stores its id so later calls know who they are about`() =
        runTest {
            assertNull("no subject before this runs", auth.current().subjectId)

            val result = repository(api).createSelfSubject(AcademicLevel.SMA)

            assertTrue("$result", result is AppResult.Success)
            assertEquals("subject-7", auth.current().subjectId)
        }

    @Test
    fun `storing the subject id leaves the signed in role and family alone`() =
        runTest {
            auth.saveIdentity(subjectId = null, familyId = "family-1", role = "owner")

            repository(api).createSelfSubject(AcademicLevel.SMA)

            val identity = auth.current()
            assertEquals("subject-7", identity.subjectId)
            assertEquals("family-1", identity.familyId)
            assertEquals("owner", identity.role)
        }

    @Test
    fun `the chosen level is what gets sent`() =
        runTest {
            repository(api).createSelfSubject(AcademicLevel.SMP)

            assertEquals(AcademicLevel.SMP.wire, api.askedLevel)
        }

    @Test
    fun `a failed creation leaves no half written identity behind`() =
        runTest {
            val failing =
                object : StubFamilyApi() {
                    override suspend fun createSelfSubject(body: SelfSubjectRequestDto): SubjectDto = throw java.io.IOException("offline")
                }

            val result = repository(failing).createSelfSubject(AcademicLevel.SMA)

            assertTrue("$result", result is AppResult.Failure)
            assertNull(auth.current().subjectId)
        }
}
