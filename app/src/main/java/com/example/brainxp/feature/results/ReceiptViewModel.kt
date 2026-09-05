package com.example.brainxp.feature.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.QuizRepository
import com.example.brainxp.domain.model.ReceiptLine
import com.example.brainxp.domain.model.SessionReceipt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptLoad(
    val receipt: ReceiptUiState? = null,
    val loading: Boolean = true,
    val error: ApiError? = null,
)

@HiltViewModel
class ReceiptViewModel
    @Inject
    constructor(
        private val quizzes: QuizRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ReceiptLoad())
        val state: StateFlow<ReceiptLoad> = mutableState.asStateFlow()

        private var submittedFor: String? = null

        fun submit(sessionId: String) {
            if (submittedFor == sessionId) return
            submittedFor = sessionId
            mutableState.update { it.copy(loading = true, error = null) }
            viewModelScope.launch {
                mutableState.update {
                    when (val result = quizzes.submit(sessionId)) {
                        is AppResult.Success -> it.copy(receipt = result.value.toUiState(), loading = false)
                        is AppResult.Failure -> it.copy(loading = false, error = result.error)
                    }
                }
            }
        }

        fun retry() {
            val sessionId = submittedFor ?: return
            submittedFor = null
            submit(sessionId)
        }
    }

internal fun SessionReceipt.toUiState(): ReceiptUiState =
    ReceiptUiState(
        title = title ?: FALLBACK_TITLE,
        correctCount = correctCount,
        questionCount = questionCount,
        baseRewardSeconds = baseRewardSeconds,
        rows = lines.map(ReceiptLine::toRow),
        subtotalSeconds = subtotalSeconds,
        levelFactor = levelFactor,
        levelNote = levelNote,
        noveltyFactor = noveltyFactor,
        noveltyNote = noveltyNote,
        creditedSeconds = creditedSeconds,
        balanceSeconds = balanceSeconds,
        streakCurrent = streakCurrent,
        newBadges = newBadges,
    )

private fun ReceiptLine.toRow(): ReceiptRow =
    ReceiptRow(
        ordinal = ordinal,
        label = label,
        difficulty = difficulty,
        multiplier = multiplier,
        rewardSeconds = rewardSeconds,
        voided = voided,
        voidReason = voidReason,
        explanation = explanation,
    )

private const val FALLBACK_TITLE = "Sesi belajar"
