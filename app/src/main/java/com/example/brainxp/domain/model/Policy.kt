package com.example.brainxp.domain.model

import kotlin.math.roundToInt

enum class UploadMethod(
    val wire: String,
) {
    PHOTO("photo"),
    DOCUMENT("document"),
    ;

    companion object {
        fun fromWire(value: String?): UploadMethod? = entries.firstOrNull { it.wire == value }
    }
}

data class PolicyDraft(
    val questionsPerSession: Int,
    val essayCount: Int,
    val baseRewardSeconds: Int,
    val dailyCapSeconds: List<Int>,
    val dailyGrantSeconds: List<Int>,
    val idleDaysAllowed: Int,
    val dayResetHour: Int,
    val uploadMethods: Set<UploadMethod>,
)

object PolicyLimits {
    const val WEEK_DAYS = 7

    val QUESTIONS_PER_SESSION = 1..10
    val BASE_REWARD_SECONDS = 15..1800
    val DAY_RESET_HOUR = 0..23
    val IDLE_DAYS_ALLOWED = 0..14

    val REWARD_STEPS = listOf(15, 30, 45, 60, 90, 120, 180, 300, 450, 600, 900, 1_200, 1_800)
    val CAP_STEPS = listOf(0, 1_800, 3_600, 5_400, 7_200, 10_800)
    val GRANT_STEPS = listOf(0, 900, 1_800, 3_600)

    fun clamp(draft: PolicyDraft): PolicyDraft {
        val questions = draft.questionsPerSession.coerceIn(QUESTIONS_PER_SESSION)
        return draft.copy(
            questionsPerSession = questions,
            essayCount = draft.essayCount.coerceIn(0, questions),
            baseRewardSeconds = draft.baseRewardSeconds.coerceIn(BASE_REWARD_SECONDS),
            dailyCapSeconds = draft.dailyCapSeconds.toWeek(),
            dailyGrantSeconds = draft.dailyGrantSeconds.toWeek(),
            idleDaysAllowed = draft.idleDaysAllowed.coerceIn(IDLE_DAYS_ALLOWED),
            dayResetHour = draft.dayResetHour.coerceIn(DAY_RESET_HOUR),
            uploadMethods = draft.uploadMethods.ifEmpty { UploadMethod.entries.toSet() },
        )
    }

    fun essayRatioOf(
        essayCount: Int,
        questionsPerSession: Int,
    ): Double {
        if (questionsPerSession <= 0) {
            return 0.0
        }
        return (essayCount.toDouble() / questionsPerSession).coerceIn(0.0, 1.0)
    }

    fun essayCountOf(
        ratio: Double,
        questionsPerSession: Int,
    ): Int = (ratio * questionsPerSession).roundToInt().coerceIn(0, questionsPerSession.coerceAtLeast(0))

    private fun List<Int>.toWeek(): List<Int> = List(WEEK_DAYS) { day -> getOrNull(day)?.coerceAtLeast(0) ?: 0 }
}

sealed interface PolicyStep {
    data object Questions : PolicyStep

    data object Essays : PolicyStep

    data object BaseReward : PolicyStep

    data object IdleDays : PolicyStep

    data object ResetHour : PolicyStep

    data class Cap(
        val day: Int,
    ) : PolicyStep

    data class Grant(
        val day: Int,
    ) : PolicyStep

    data class Upload(
        val method: UploadMethod,
    ) : PolicyStep
}

fun PolicyDraft.stepped(step: PolicyStep): PolicyDraft =
    when (step) {
        PolicyStep.Questions -> {
            val next =
                if (questionsPerSession >= PolicyLimits.QUESTIONS_PER_SESSION.last) {
                    QUESTION_STEP
                } else {
                    questionsPerSession + QUESTION_STEP
                }
            copy(questionsPerSession = next, essayCount = essayCount.coerceAtMost(next))
        }

        PolicyStep.Essays -> {
            copy(essayCount = if (essayCount >= questionsPerSession) 0 else essayCount + 1)
        }

        PolicyStep.BaseReward -> {
            copy(baseRewardSeconds = PolicyLimits.REWARD_STEPS.after(baseRewardSeconds))
        }

        PolicyStep.IdleDays -> {
            copy(
                idleDaysAllowed =
                    if (idleDaysAllowed >= PolicyLimits.IDLE_DAYS_ALLOWED.last) 0 else idleDaysAllowed + 1,
            )
        }

        PolicyStep.ResetHour -> {
            copy(dayResetHour = if (dayResetHour >= PolicyLimits.DAY_RESET_HOUR.last) 0 else dayResetHour + 1)
        }

        is PolicyStep.Cap -> {
            copy(dailyCapSeconds = dailyCapSeconds.cycled(step.day, PolicyLimits.CAP_STEPS))
        }

        is PolicyStep.Grant -> {
            copy(dailyGrantSeconds = dailyGrantSeconds.cycled(step.day, PolicyLimits.GRANT_STEPS))
        }

        is PolicyStep.Upload -> {
            copy(uploadMethods = uploadMethods.toggling(step.method))
        }
    }

enum class StepDirection {
    UP,
    DOWN,
    ;

    val sign: Int get() = if (this == UP) 1 else -1
}

fun PolicyDraft.nudged(
    step: PolicyStep,
    direction: StepDirection,
): PolicyDraft =
    when (step) {
        PolicyStep.Questions -> {
            val next =
                (questionsPerSession + direction.sign * QUESTION_STEP)
                    .coerceIn(PolicyLimits.QUESTIONS_PER_SESSION)
            copy(questionsPerSession = next, essayCount = essayCount.coerceAtMost(next))
        }

        PolicyStep.Essays -> {
            copy(essayCount = (essayCount + direction.sign).coerceIn(0, questionsPerSession))
        }

        PolicyStep.BaseReward -> {
            copy(baseRewardSeconds = PolicyLimits.REWARD_STEPS.shifted(baseRewardSeconds, direction))
        }

        PolicyStep.IdleDays -> {
            copy(idleDaysAllowed = (idleDaysAllowed + direction.sign).coerceIn(PolicyLimits.IDLE_DAYS_ALLOWED))
        }

        PolicyStep.ResetHour -> {
            copy(dayResetHour = (dayResetHour + direction.sign).coerceIn(PolicyLimits.DAY_RESET_HOUR))
        }

        is PolicyStep.Cap -> {
            copy(dailyCapSeconds = dailyCapSeconds.nudgedAt(step.day, PolicyLimits.CAP_STEPS, direction))
        }

        is PolicyStep.Grant -> {
            copy(dailyGrantSeconds = dailyGrantSeconds.nudgedAt(step.day, PolicyLimits.GRANT_STEPS, direction))
        }

        is PolicyStep.Upload -> {
            copy(uploadMethods = uploadMethods.toggling(step.method))
        }
    }

fun PolicyDraft.canNudge(
    step: PolicyStep,
    direction: StepDirection,
): Boolean = nudged(step, direction) != this

private const val QUESTION_STEP = 1

private fun List<Int>.shifted(
    current: Int,
    direction: StepDirection,
): Int {
    val at = indexOf(current)
    if (at >= 0) {
        return this[(at + direction.sign).coerceIn(indices)]
    }
    return when (direction) {
        StepDirection.UP -> firstOrNull { step -> step > current } ?: last()
        StepDirection.DOWN -> lastOrNull { step -> step < current } ?: first()
    }
}

private fun List<Int>.nudgedAt(
    index: Int,
    steps: List<Int>,
    direction: StepDirection,
): List<Int> = mapIndexed { at, value -> if (at == index) steps.shifted(value, direction) else value }

private fun List<Int>.after(current: Int): Int {
    val at = indexOf(current)
    return if (at == -1) first() else this[(at + 1) % size]
}

private fun Set<UploadMethod>.toggling(method: UploadMethod): Set<UploadMethod> =
    if (method in this) (this - method).ifEmpty { this } else this + method

private fun List<Int>.cycled(
    index: Int,
    steps: List<Int>,
): List<Int> =
    mapIndexed { at, value ->
        if (at == index) steps.after(value) else value
    }
