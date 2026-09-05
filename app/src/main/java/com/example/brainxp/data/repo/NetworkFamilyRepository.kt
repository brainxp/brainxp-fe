package com.example.brainxp.data.repo

import android.os.Build
import com.example.brainxp.core.device.InstallBinding
import com.example.brainxp.core.network.BindingCheckRequestDto
import com.example.brainxp.core.network.ChildRequestDto
import com.example.brainxp.core.network.ErrorMapper
import com.example.brainxp.core.network.FamilyApi
import com.example.brainxp.core.network.HeartbeatRequestDto
import com.example.brainxp.core.network.PairRequestDto
import com.example.brainxp.core.network.PairingCodeDto
import com.example.brainxp.core.network.SubjectDto
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.core.result.map
import com.example.brainxp.data.prefs.AuthDataStore
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.DeviceBinding
import com.example.brainxp.domain.model.FamilyChild
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
                auth.saveTokens(token.accessToken, token.refreshToken)
                auth.saveIdentity(token.subjectId, token.familyId, token.role)
            }

        override suspend fun checkBinding(): AppResult<DeviceBinding> =
            call { api.checkBinding(BindingCheckRequestDto(binding.value())) }
                .map { DeviceBinding(bound = it.bound, familyMode = it.familyMode, subjectName = it.subjectName) }

        override suspend fun reportHealth(status: GuardianStatus): AppResult<Unit> =
            call { api.heartbeat(HeartbeatRequestDto(guardianStatus = status.name.lowercase())) }

        private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
            runCatching { block() }
                .fold(
                    onSuccess = { AppResult.Success(it) },
                    onFailure = { AppResult.Failure(errors.map(it)) },
                )
    }

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
