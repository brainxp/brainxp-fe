package com.example.brainxp.feature.family

import com.example.brainxp.domain.model.AcademicLevel

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
    val dailyCapMinutes: List<Int>,
    val dailyGrantMinutes: List<Int>,
    val idleDaysAllowed: Int,
    val dayResetHour: Int,
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
    data object StepQuestions : PolicyEvent

    data object StepEssay : PolicyEvent

    data class StepCap(
        val day: Int,
    ) : PolicyEvent

    data class StepGrant(
        val day: Int,
    ) : PolicyEvent

    data object StepIdleDays : PolicyEvent

    data object StepResetHour : PolicyEvent

    data class ToggleApp(
        val packageName: String,
    ) : PolicyEvent

    data object Save : PolicyEvent
}

fun PolicyUiState.stepped(event: PolicyEvent): PolicyUiState =
    when (event) {
        PolicyEvent.StepQuestions -> {
            val next = if (questionsPerSession >= MAX_QUESTIONS_PER_SESSION) 2 else questionsPerSession + 2
            copy(questionsPerSession = next, essayCount = essayCount.coerceAtMost(next))
        }

        PolicyEvent.StepEssay -> {
            copy(essayCount = if (essayCount >= questionsPerSession) 0 else essayCount + 1)
        }

        is PolicyEvent.StepCap -> {
            copy(dailyCapMinutes = dailyCapMinutes.cycled(event.day, CAP_STEPS))
        }

        is PolicyEvent.StepGrant -> {
            copy(dailyGrantMinutes = dailyGrantMinutes.cycled(event.day, GRANT_STEPS))
        }

        PolicyEvent.StepIdleDays -> {
            copy(idleDaysAllowed = if (idleDaysAllowed >= MAX_IDLE_DAYS) 0 else idleDaysAllowed + 1)
        }

        PolicyEvent.StepResetHour -> {
            copy(dayResetHour = if (dayResetHour >= MAX_RESET_HOUR) 0 else dayResetHour + 1)
        }

        is PolicyEvent.ToggleApp -> {
            copy(
                apps =
                    apps.map { app ->
                        if (app.packageName == event.packageName) {
                            app.copy(locked = !app.locked)
                        } else {
                            app
                        }
                    },
            )
        }

        PolicyEvent.Save -> {
            this
        }
    }

private fun List<Int>.cycled(
    index: Int,
    steps: List<Int>,
): List<Int> {
    val current = getOrNull(index) ?: return this
    val next = steps[(steps.indexOf(current).takeIf { it >= 0 }?.plus(1) ?: 0) % steps.size]
    return mapIndexed { i, value -> if (i == index) next else value }
}

private val CAP_STEPS = listOf(0, 30, 60, 90, 120, 180)
private val GRANT_STEPS = listOf(0, 15, 30, 60)

const val MAX_QUESTIONS_PER_SESSION = 12
const val MAX_IDLE_DAYS = 14
const val MAX_RESET_HOUR = 23
private const val PERCENT = 100
