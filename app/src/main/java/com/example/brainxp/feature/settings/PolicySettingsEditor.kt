package com.example.brainxp.feature.settings

import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.PolicyChange
import com.example.brainxp.data.repo.PolicyRepository
import com.example.brainxp.data.repo.SubjectPolicy
import com.example.brainxp.data.repo.toDraft
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.PolicyStep
import com.example.brainxp.domain.model.stepped
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SettingsPolicyEdit {
    data class Level(
        val level: AcademicLevel,
    ) : SettingsPolicyEdit

    data class Language(
        val code: String,
    ) : SettingsPolicyEdit

    data class Rule(
        val step: PolicyStep,
    ) : SettingsPolicyEdit
}

@Singleton
class PolicySettingsEditor
    @Inject
    constructor(
        private val policies: PolicyRepository,
    ) {
        suspend fun apply(
            edit: SettingsPolicyEdit,
            current: SubjectPolicy,
        ): AppResult<PolicyChange> =
            when (edit) {
                is SettingsPolicyEdit.Level -> policies.setLevel(edit.level)
                is SettingsPolicyEdit.Language -> policies.setLanguage(edit.code)
                is SettingsPolicyEdit.Rule -> policies.save(current.toDraft().stepped(edit.step))
            }
    }
