package com.example.brainxp.feature.settings

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.PolicyChange
import com.example.brainxp.data.repo.PolicyRepository
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.PolicyDraft
import com.example.brainxp.domain.model.PolicyLimits
import com.example.brainxp.domain.model.PolicyStep
import com.example.brainxp.domain.model.StepDirection
import com.example.brainxp.domain.model.UploadMethod
import com.example.brainxp.domain.model.nudged
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DraftChange {
    data class Step(
        val step: PolicyStep,
        val direction: StepDirection,
    ) : DraftChange

    data class Questions(
        val count: Int,
    ) : DraftChange

    data class RewardMinutes(
        val minutes: Int,
    ) : DraftChange

    data class EssayPercent(
        val percent: Int,
    ) : DraftChange

    data class IdleDays(
        val days: Int,
    ) : DraftChange

    data class ResetHour(
        val hour: Int,
    ) : DraftChange

    data class Caps(
        val minutes: List<Int>,
    ) : DraftChange

    data class Grants(
        val minutes: List<Int>,
    ) : DraftChange

    data class Upload(
        val method: UploadMethod,
    ) : DraftChange
}

sealed interface SettingsPolicyEdit {
    data class Level(
        val level: AcademicLevel,
    ) : SettingsPolicyEdit

    data class Language(
        val code: String,
    ) : SettingsPolicyEdit

    data class Draft(
        val change: DraftChange,
    ) : SettingsPolicyEdit
}

private const val SECONDS_PER_MINUTE = 60
private const val PERCENT = 100

val REWARD_MINUTE_RANGE: IntRange
    get() =
        (PolicyLimits.REWARD_STEPS.first() / SECONDS_PER_MINUTE)..(PolicyLimits.REWARD_STEPS.last() / SECONDS_PER_MINUTE)

private fun rewardStepNearest(minutes: Int): Int {
    val wanted = minutes * SECONDS_PER_MINUTE
    return PolicyLimits.REWARD_STEPS.minByOrNull { step -> kotlin.math.abs(step - wanted) }
        ?: PolicyLimits.REWARD_STEPS.first()
}

@Singleton
class PolicySettingsEditor
    @Inject
    constructor(
        private val policies: PolicyRepository,
    ) {
        suspend fun applyDirect(edit: SettingsPolicyEdit): AppResult<PolicyChange> =
            when (edit) {
                is SettingsPolicyEdit.Level -> policies.setLevel(edit.level)
                is SettingsPolicyEdit.Language -> policies.setLanguage(edit.code)
                is SettingsPolicyEdit.Draft -> error("draft edits are saved in a batch, not sent directly")
            }

        fun applyToDraft(
            change: DraftChange,
            current: PolicyDraft,
        ): PolicyDraft =
            PolicyLimits.clamp(
                when (change) {
                    is DraftChange.Step -> {
                        current.nudged(change.step, change.direction)
                    }

                    is DraftChange.RewardMinutes -> {
                        current.copy(baseRewardSeconds = rewardStepNearest(change.minutes))
                    }

                    is DraftChange.EssayPercent -> {
                        current.copy(
                            essayCount =
                                PolicyLimits.essayCountOf(
                                    change.percent.toDouble() / PERCENT,
                                    current.questionsPerSession,
                                ),
                        )
                    }

                    is DraftChange.Questions -> {
                        current.copy(
                            questionsPerSession = change.count,
                            essayCount = current.essayCount.coerceAtMost(change.count),
                        )
                    }

                    is DraftChange.IdleDays -> {
                        current.copy(idleDaysAllowed = change.days)
                    }

                    is DraftChange.ResetHour -> {
                        current.copy(dayResetHour = change.hour)
                    }

                    is DraftChange.Caps -> {
                        current.copy(dailyCapSeconds = change.minutes.toSeconds())
                    }

                    is DraftChange.Grants -> {
                        current.copy(dailyGrantSeconds = change.minutes.toSeconds())
                    }

                    is DraftChange.Upload -> {
                        current.copy(uploadMethods = current.uploadMethods.toggling(change.method))
                    }
                },
            )
    }

fun List<Int>.toSeconds(): List<Int> = map { it * SECONDS_PER_MINUTE }

fun List<Int>.toMinutes(): List<Int> = map { it / SECONDS_PER_MINUTE }

private fun Set<UploadMethod>.toggling(method: UploadMethod): Set<UploadMethod> =
    if (method in this) (this - method).ifEmpty { this } else this + method
