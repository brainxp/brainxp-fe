package com.example.brainxp.feature.questions

import com.example.brainxp.core.result.ApiError
import com.example.brainxp.core.result.AppResult
import com.example.brainxp.data.repo.QuestionReporter
import com.example.brainxp.domain.model.ReportReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private data class SentReport(
    val sessionId: String,
    val questionId: String,
    val reason: ReportReason,
    val note: String,
)

private class FakeReports(
    var result: AppResult<String> = AppResult.Success("Terima kasih, laporan diterima."),
) : QuestionReporter {
    val sent = mutableListOf<SentReport>()
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun report(
        sessionId: String,
        questionId: String,
        reason: ReportReason,
        note: String,
    ): AppResult<String> {
        sent += SentReport(sessionId, questionId, reason, note)
        gate?.await()
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportQuestionViewModelTest {
    private lateinit var reports: FakeReports
    private lateinit var viewModel: ReportQuestionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        reports = FakeReports()
        viewModel = ReportQuestionViewModel(reports)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `opening the form for a question starts with nothing chosen`() {
        viewModel.open("session-1", "question-1")

        val form = viewModel.state.value.form
        assertEquals("question-1", form?.questionId)
        assertNull(form?.reason)
        assertFalse(viewModel.state.value.canSend)
    }

    @Test
    fun `a report cannot be sent until a reason is picked`() {
        viewModel.open("session-1", "question-1")

        viewModel.send()

        assertTrue(reports.sent.isEmpty())
    }

    @Test
    fun `sending posts the picked reason and note for the open question`() =
        runTest {
            viewModel.open("session-1", "question-1")
            viewModel.pick(ReportReason.WRONG_KEY)
            viewModel.write("Jawaban B juga benar")

            viewModel.send()

            assertEquals(
                listOf(SentReport("session-1", "question-1", ReportReason.WRONG_KEY, "Jawaban B juga benar")),
                reports.sent,
            )
        }

    @Test
    fun `a successful report closes the form and shows the server message`() =
        runTest {
            viewModel.open("session-1", "question-1")
            viewModel.pick(ReportReason.UNCLEAR)

            viewModel.send()

            assertNull(viewModel.state.value.form)
            assertEquals("Terima kasih, laporan diterima.", viewModel.state.value.thanks)
        }

    @Test
    fun `a failed report keeps the form open with what was typed`() =
        runTest {
            reports.result = AppResult.Failure(ApiError.Network)
            viewModel.open("session-1", "question-1")
            viewModel.pick(ReportReason.OTHER)
            viewModel.write("Gambarnya tidak muncul")

            viewModel.send()

            val state = viewModel.state.value
            assertEquals(ApiError.Network, state.failure)
            assertEquals(ReportReason.OTHER, state.form?.reason)
            assertEquals("Gambarnya tidak muncul", state.form?.note)
        }

    @Test
    fun `tapping send twice while a report is in flight sends it once`() =
        runTest {
            reports.gate = CompletableDeferred()
            viewModel.open("session-1", "question-1")
            viewModel.pick(ReportReason.OFF_MATERIAL)

            viewModel.send()
            viewModel.send()
            reports.gate?.complete(Unit)

            assertEquals(1, reports.sent.size)
        }

    @Test
    fun `dismissing the form discards it without sending`() {
        viewModel.open("session-1", "question-1")
        viewModel.pick(ReportReason.WRONG_KEY)

        viewModel.dismiss()

        assertNull(viewModel.state.value.form)
        assertTrue(reports.sent.isEmpty())
    }
}
