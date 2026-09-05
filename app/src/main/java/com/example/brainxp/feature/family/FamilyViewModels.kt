package com.example.brainxp.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.FamilyRepository
import com.example.brainxp.data.repo.RewardRepository
import com.example.brainxp.domain.model.AcademicLevel
import com.example.brainxp.domain.model.LedgerEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val REPORT_WINDOW_DAYS = 7

data class NewChildLoad(
    val creating: Boolean = false,
    val createdId: String? = null,
    val error: ApiError? = null,
)

@HiltViewModel
class NewChildViewModel
    @Inject
    constructor(
        private val family: FamilyRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(NewChildLoad())
        val state: StateFlow<NewChildLoad> = mutableState.asStateFlow()

        fun create(
            name: String,
            level: AcademicLevel,
            language: String,
        ) {
            if (mutableState.value.creating) return
            mutableState.update { it.copy(creating = true, error = null) }
            viewModelScope.launch {
                mutableState.update {
                    when (val result = family.createChild(name, level, language)) {
                        is AppResult.Success -> it.copy(creating = false, createdId = result.value.childId)
                        is AppResult.Failure -> it.copy(creating = false, error = result.error)
                    }
                }
            }
        }
    }

data class PairingCodeLoad(
    val code: PairingCodeUiState? = null,
    val loading: Boolean = true,
    val error: ApiError? = null,
)

@HiltViewModel
class PairingCodeViewModel
    @Inject
    constructor(
        private val family: FamilyRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(PairingCodeLoad())
        val state: StateFlow<PairingCodeLoad> = mutableState.asStateFlow()

        private var loadedFor: String? = null

        fun load(childId: String) {
            if (loadedFor == childId) return
            loadedFor = childId
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                mutableState.update {
                    when (val result = family.pairingCode(childId)) {
                        is AppResult.Success -> {
                            it.copy(
                                code =
                                    PairingCodeUiState(
                                        subjectName = "",
                                        code = result.value.code,
                                        secondsLeft = 0,
                                    ),
                                loading = false,
                            )
                        }

                        is AppResult.Failure -> {
                            it.copy(loading = false, error = result.error)
                        }
                    }
                }
            }
        }

        fun refresh() {
            val childId = loadedFor ?: return
            loadedFor = null
            load(childId)
        }
    }

data class ChildReportLoad(
    val report: ChildReportUiState? = null,
    val loading: Boolean = true,
    val error: ApiError? = null,
)

@HiltViewModel
class ChildReportViewModel
    @Inject
    constructor(
        private val rewards: RewardRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ChildReportLoad())
        val state: StateFlow<ChildReportLoad> = mutableState.asStateFlow()

        private var loadedFor: String? = null

        fun load(
            childId: String,
            childName: String,
        ) {
            if (loadedFor == childId) return
            loadedFor = childId
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                mutableState.update {
                    when (val result = rewards.report(REPORT_WINDOW_DAYS, childId)) {
                        is AppResult.Success -> {
                            val report = result.value
                            it.copy(
                                report =
                                    ChildReportUiState(
                                        subjectName = childName,
                                        alert = report.guardianAlerts.firstOrNull(),
                                        earnedPerDay = report.days.map { day -> day.earnedSeconds },
                                        balanceSeconds = report.standing.balanceSeconds,
                                        materialsStudied = report.materialsStudied,
                                        correctTotal = report.correctTotal,
                                        essayPassed = report.essayPassed,
                                        recent = report.recent.map(LedgerEntry::toRow),
                                    ),
                                loading = false,
                            )
                        }

                        is AppResult.Failure -> {
                            it.copy(loading = false, error = result.error)
                        }
                    }
                }
            }
        }

        fun retry() {
            val childId = loadedFor ?: return
            loadedFor = null
            load(
                childId,
                mutableState.value.report
                    ?.subjectName
                    .orEmpty(),
            )
        }
    }

private fun LedgerEntry.toRow(): LedgerRow =
    LedgerRow(
        label = entryType,
        note = note,
        deltaSeconds = deltaSeconds,
    )
