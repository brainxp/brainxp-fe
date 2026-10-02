package com.example.brainxp.feature.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.QuestionReporter
import com.example.brainxp.domain.model.ReportReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportForm(
    val sessionId: String,
    val questionId: String,
    val number: Int = 0,
    val reason: ReportReason? = null,
    val note: String = "",
)

data class ReportQuestionState(
    val form: ReportForm? = null,
    val sending: Boolean = false,
    val failure: ApiError? = null,
    val thanks: String? = null,
) {
    val canSend: Boolean get() = form?.reason != null && !sending
}

@HiltViewModel
class ReportQuestionViewModel
    @Inject
    constructor(
        private val reports: QuestionReporter,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ReportQuestionState())
        val state: StateFlow<ReportQuestionState> = mutableState.asStateFlow()

        fun open(
            sessionId: String,
            questionId: String,
            number: Int = 0,
        ) {
            mutableState.value = ReportQuestionState(form = ReportForm(sessionId, questionId, number))
        }

        fun pick(reason: ReportReason) = mutableState.update { it.copy(form = it.form?.copy(reason = reason), failure = null) }

        fun write(note: String) = mutableState.update { it.copy(form = it.form?.copy(note = note.take(NOTE_LIMIT))) }

        fun dismiss() = mutableState.update { it.copy(form = null, failure = null) }

        fun acknowledge() = mutableState.update { it.copy(thanks = null) }

        fun send() {
            val current = mutableState.value
            val form = current.form
            val reason = form?.reason
            if (form == null || reason == null || current.sending) return
            mutableState.update { it.copy(sending = true, failure = null) }
            viewModelScope.launch {
                val result = reports.report(form.sessionId, form.questionId, reason, form.note)
                mutableState.update {
                    when (result) {
                        is AppResult.Success -> ReportQuestionState(thanks = result.value)
                        is AppResult.Failure -> it.copy(sending = false, failure = result.error)
                    }
                }
            }
        }

        private companion object {
            const val NOTE_LIMIT = 1_000
        }
    }
