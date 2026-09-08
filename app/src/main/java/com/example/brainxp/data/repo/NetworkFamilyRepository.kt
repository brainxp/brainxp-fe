package com.example.brainxp.data.repo

import android.os.Build
import com.example.brainxp.core.device.InstallBinding
import com.example.brainxp.core.network.BindingCheckRequestDto
import com.example.brainxp.core.network.ChildRequestDto
import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.network.FamilyApi
import com.example.brainxp.core.network.GuardianEventDto
import com.example.brainxp.core.network.HeartbeatRequestDto
import com.example.brainxp.core.network.PairRequestDto
import com.example.brainxp.core.network.PairingCodeDto
import com.example.brainxp.core.network.SelfSubjectRequestDto
import com.example.brainxp.core.network.SubjectDto
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.result.map
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.DeviceBinding
import com.example.brainxp.domain.model.FamilyChild
import com.example.brainxp.domain.model.GuardianEvent
import com.example.brainxp.domain.model.GuardianStatus
import com.example.brainxp.domain.model.PairingCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkFamilyRepository
    @Inject
    constructor(
        private val api: FamilyApi,
        private val binding: InstallBinding,
        private val auth: AuthDataStore,
        private val errors: ErrorMapper,
        private val teardown: SessionTeardown,
    ) : FamilyRepository {
        override suspend fun children(): AppResult<List<FamilyChild>> = call { api.children().map(SubjectDto::toChild) }

        override suspend fun createChild(
            name: String,
            level: AcademicLevel,
            language: String,
        ): AppResult<FamilyChild> =
            call {
                api
                    .createChild(
                        ChildRequestDto(
                            displayName = name,
                            academicLevel = level.wire,
                            questionLanguage = language,
                        ),
                    ).toChild()
            }

        override suspend fun createSelfSubject(level: AcademicLevel): AppResult<FamilyChild> =
            call {
                val subject = api.createSelfSubject(SelfSubjectRequestDto(academicLevel = level.wire))
                auth.saveSubject(subject.id)
                subject.toChild()
            }

        override suspend fun removeChild(childId: String): AppResult<Unit> = call { api.deleteSubject(childId) }

        override suspend fun releaseDevice(childId: String): AppResult<Unit> = call { api.releaseDevice(childId) }

        override suspend fun pairingCode(childId: String): AppResult<PairingCode> =
            call { api.pairingCode(childId) }.map(PairingCodeDto::toCode)

        override suspend fun pair(code: String): AppResult<Unit> =
            call {
                val token =
                    api.pair(
                        PairRequestDto(
                            code = code,
                            installBinding = binding.value(),
                            modelName = Build.MODEL,
                        ),
                    )
                teardown.run()
                auth.saveTokens(token.accessToken, token.refreshToken)
                auth.saveIdentity(token.subjectId, token.familyId, token.role)
            }

        override suspend fun checkBinding(): AppResult<DeviceBinding> =
            call { api.checkBinding(BindingCheckRequestDto(binding.value())) }
                .map { DeviceBinding(bound = it.bound, familyMode = it.familyMode, subjectName = it.subjectName) }

        override suspend fun reportHealth(
            status: GuardianStatus,
            events: List<GuardianEvent>,
        ): AppResult<Unit> =
            call {
                api.heartbeat(
                    HeartbeatRequestDto(
                        guardianStatus = status.name.lowercase(),
                        events = events.map(GuardianEvent::toDto),
                    ),
                )
            }

        private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
            runCatching { block() }
                .fold(
                    onSuccess = { AppResult.Success(it) },
                    onFailure = { AppResult.Failure(errors.map(it)) },
                )
    }

private fun GuardianEvent.toDto(): GuardianEventDto =
    GuardianEventDto(
        type = type,
        permission = permission,
        required = required,
    )

private fun SubjectDto.toChild(): FamilyChild =
    FamilyChild(
        childId = id,
        name = displayName,
        level = AcademicLevel.fromWire(academicLevel),
    )

private fun PairingCodeDto.toCode(): PairingCode =
    PairingCode(
        code = code,
        childId = subjectId,
        expiresAt = expiresAt,
        attemptsAllowed = attemptsAllowed,
    )
