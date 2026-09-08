package com.example.brainxp.data.repo

import com.example.brainxp.core.network.PolicyDto
import com.example.brainxp.core.network.PolicyPatchDto
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.PolicyDraft
import com.example.brainxp.domain.model.PolicyLimits
import com.example.brainxp.domain.model.UploadMethod

fun PolicyDraft.toPatch(): PolicyPatchDto {
    val safe = PolicyLimits.clamp(this)
    return PolicyPatchDto(
        questionsPerSession = safe.questionsPerSession,
        baseRewardSeconds = safe.baseRewardSeconds,
        essayRatio = PolicyLimits.essayRatioOf(safe.essayCount, safe.questionsPerSession),
        dailyCaps = safe.dailyCapSeconds,
        dailyGrants = safe.dailyGrantSeconds,
        dayResetHour = safe.dayResetHour,
        idleDaysAllowed = safe.idleDaysAllowed,
        allowedUploadMethods = safe.uploadMethods.map(UploadMethod::wire).sorted(),
    )
}

fun PolicyDto.toSubjectPolicy(): SubjectPolicy =
    SubjectPolicy(
        level = AcademicLevel.fromWire(academicLevel),
        language = questionLanguage.orEmpty(),
        questionsPerSession = questionsPerSession,
        dayResetHour = dayResetHour,
        idleDaysAllowed = idleDaysAllowed,
        pendingWeakenAt = pendingWeakenAt,
        essayCount = PolicyLimits.essayCountOf(essayRatio, questionsPerSession),
        dailyCapSeconds = dailyCaps,
        dailyGrantSeconds = dailyGrants,
        lockedApps = lockedApps,
        baseRewardSeconds = baseRewardSeconds,
        uploadMethods = allowedUploadMethods.mapNotNull(UploadMethod::fromWire).toSet(),
    )

fun SubjectPolicy.toDraft(): PolicyDraft =
    PolicyDraft(
        questionsPerSession = questionsPerSession,
        essayCount = essayCount,
        baseRewardSeconds = baseRewardSeconds,
        dailyCapSeconds = dailyCapSeconds,
        dailyGrantSeconds = dailyGrantSeconds,
        idleDaysAllowed = idleDaysAllowed,
        dayResetHour = dayResetHour,
        uploadMethods = uploadMethods,
    )
