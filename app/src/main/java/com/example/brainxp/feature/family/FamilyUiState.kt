package com.example.brainxp.feature.family

import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.GuardianAlert
import com.example.brainxp.domain.model.PolicyDraft
import com.example.brainxp.domain.model.PolicyLimits
import com.example.brainxp.domain.model.PolicyStep
import com.example.brainxp.domain.model.StepDirection
import com.example.brainxp.domain.model.UploadMethod
import com.example.brainxp.domain.model.canNudge
import com.example.brainxp.domain.model.nudged

enum class QuestionLanguage {
    ID,
    EN,
}

enum class AdjustDirection {
    GRANT,
    REDEEM,
}

data class FamilyMember(
    val id: String,
    val name: String,
    val level: AcademicLevel,
    val balanceSeconds: Int,
    val streakDays: Int,
    val remainingCapSeconds: Int,
)

data class FamilyHomeUiState(
    val children: List<FamilyMember> = emptyList(),
    val self: FamilyMember? = null,
    val alerts: List<GuardianAlert> = emptyList(),
)

data class LockedAppEntry(
    val packageName: String,
    val label: String,
    val locked: Boolean,
)

data class PolicyUiState(
    val subjectName: String,
    val questionsPerSession: Int,
    val essayCount: Int,
    val baseRewardSeconds: Int,
    val dailyCapMinutes: List<Int>,
    val dailyGrantMinutes: List<Int>,
    val idleDaysAllowed: Int,
    val dayResetHour: Int,
    val uploadMethods: Set<UploadMethod>,
    val apps: List<LockedAppEntry>,
) {
    val mcqCount: Int get() = (questionsPerSession - essayCount).coerceAtLeast(0)

    val essayPercent: Int
        get() = if (questionsPerSession == 0) 0 else essayCount * PERCENT / questionsPerSession
}

data class BoundDevice(
    val modelName: String,
    val pairedAt: String,
)

data class PairingCodeUiState(
    val subjectName: String,
    val code: String,
    val secondsLeft: Int,
    val device: BoundDevice? = null,
)

data class LedgerRow(
    val label: String,
    val note: String?,
    val deltaSeconds: Int,
)

data class ChildReportUiState(
    val subjectName: String,
    val mine: Boolean = false,
    val alert: String? = null,
    val earnedPerDay: List<Int> = emptyList(),
    val balanceSeconds: Int = 0,
    val materialsStudied: Int = 0,
    val correctTotal: Int = 0,
    val essayPassed: Int = 0,
    val recent: List<LedgerRow> = emptyList(),
)

sealed interface PolicyEvent {
    data class Nudge(
        val step: PolicyStep,
        val direction: StepDirection,
    ) : PolicyEvent

    data class ToggleUpload(
        val method: UploadMethod,
    ) : PolicyEvent

    data class SetCaps(
        val minutes: List<Int>,
    ) : PolicyEvent

    data class SetGrants(
        val minutes: List<Int>,
    ) : PolicyEvent

    data class ToggleApp(
        val packageName: String,
    ) : PolicyEvent

    data object Save : PolicyEvent
}

fun PolicyUiState.stepped(event: PolicyEvent): PolicyUiState =
    when (event) {
        is PolicyEvent.Nudge -> {
            withDraft(draft().nudged(event.step, event.direction))
        }

        is PolicyEvent.ToggleUpload -> {
            withDraft(draft().nudged(PolicyStep.Upload(event.method), StepDirection.UP))
        }

        is PolicyEvent.SetCaps -> {
            copy(dailyCapMinutes = event.minutes)
        }

        is PolicyEvent.SetGrants -> {
            copy(dailyGrantMinutes = event.minutes)
        }

        is PolicyEvent.ToggleApp -> {
            copy(
                apps =
                    apps.map { app ->
                        if (app.packageName == event.packageName) app.copy(locked = !app.locked) else app
                    },
            )
        }

        PolicyEvent.Save -> {
            this
        }
    }

fun PolicyUiState.canNudge(
    step: PolicyStep,
    direction: StepDirection,
): Boolean = draft().canNudge(step, direction)

fun PolicyUiState.draft(): PolicyDraft =
    PolicyDraft(
        questionsPerSession = questionsPerSession,
        essayCount = essayCount,
        baseRewardSeconds = baseRewardSeconds,
        dailyCapSeconds = dailyCapMinutes.map { it * SECONDS_PER_MINUTE },
        dailyGrantSeconds = dailyGrantMinutes.map { it * SECONDS_PER_MINUTE },
        idleDaysAllowed = idleDaysAllowed,
        dayResetHour = dayResetHour,
        uploadMethods = uploadMethods,
    )

private fun PolicyUiState.withDraft(draft: PolicyDraft): PolicyUiState =
    copy(
        questionsPerSession = draft.questionsPerSession,
        essayCount = draft.essayCount,
        baseRewardSeconds = draft.baseRewardSeconds,
        dailyCapMinutes = draft.dailyCapSeconds.map { it / SECONDS_PER_MINUTE },
        dailyGrantMinutes = draft.dailyGrantSeconds.map { it / SECONDS_PER_MINUTE },
        idleDaysAllowed = draft.idleDaysAllowed,
        dayResetHour = draft.dayResetHour,
        uploadMethods = draft.uploadMethods,
    )

private const val SECONDS_PER_MINUTE = 60

val MAX_QUESTIONS_PER_SESSION = PolicyLimits.QUESTIONS_PER_SESSION.last
val MAX_IDLE_DAYS = PolicyLimits.IDLE_DAYS_ALLOWED.last
val MAX_RESET_HOUR = PolicyLimits.DAY_RESET_HOUR.last
private const val PERCENT = 100
