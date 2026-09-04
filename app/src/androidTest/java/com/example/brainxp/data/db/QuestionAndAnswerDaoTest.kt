package com.example.brainxp.data.db

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionAndAnswerDaoTest : DbTest() {
    private suspend fun session() {
        seedMaterial("m1")
        seedSession("s1", "m1")
    }

    private suspend fun answer(
        questionId: String,
        correct: Boolean? = null,
        answeredAt: Long = 1_000L,
        syncState: SyncState = SyncState.PENDING,
    ) {
        db.answerDao().upsert(
            AnswerEntity(
                questionId = questionId,
                sessionId = "s1",
                answer = "A",
                correct = correct,
                answeredAt = answeredAt,
                syncState = syncState,
            ),
        )
    }

    @Test
    fun questionsComeBackInPresentationOrderNotInsertionOrder() =
        runTest {
            session()
            seedQuestion("q3", "s1", orderIndex = 2)
            seedQuestion("q1", "s1", orderIndex = 0)
            seedQuestion("q2", "s1", orderIndex = 1)

            val ordered = db.questionDao().findForSession("s1").map { it.id }

            assertEquals(listOf("q1", "q2", "q3"), ordered)
        }

    @Test
    fun conceptIdsSurviveTheListConverter() =
        runTest {
            session()
            seedQuestion("q1", "s1", orderIndex = 0, conceptIds = listOf("kinematics", "velocity"))

            val stored = db.questionDao().findForSession("s1").single()

            assertEquals(listOf("kinematics", "velocity"), stored.conceptIds)
        }

    @Test
    fun unknownQuestionTypeRoundTripsWithoutLoss() =
        runTest {
            session()
            seedQuestion("q1", "s1", orderIndex = 0, type = "some_future_type")

            assertEquals(
                "some_future_type",
                db
                    .questionDao()
                    .findForSession("s1")
                    .single()
                    .type,
            )
        }

    @Test
    fun resumePointIsTheFirstQuestionWithoutAnAnswer() =
        runTest {
            session()
            seedQuestion("q1", "s1", orderIndex = 0)
            seedQuestion("q2", "s1", orderIndex = 1)
            seedQuestion("q3", "s1", orderIndex = 2)
            answer("q1")

            assertEquals("q2", db.questionDao().findNextUnanswered("s1")?.id)
        }

    @Test
    fun fullyAnsweredSessionHasNoResumePoint() =
        runTest {
            session()
            seedQuestion("q1", "s1", orderIndex = 0)
            answer("q1")

            assertNull(db.questionDao().findNextUnanswered("s1"))
        }

    @Test
    fun resumePointIgnoresAnswersFromOtherSessions() =
        runTest {
            session()
            seedSession("s2", "m1")
            seedQuestion("q1", "s1", orderIndex = 0)
            seedQuestion("q2", "s2", orderIndex = 0)
            answer("q1")

            assertEquals("q2", db.questionDao().findNextUnanswered("s2")?.id)
        }

    @Test
    fun answeringTwiceOverwritesRatherThanDuplicating() =
        runTest {
            session()
            seedQuestion("q1", "s1", orderIndex = 0)
            answer("q1", correct = false)
            answer("q1", correct = true)

            val answers = db.answerDao().findForSession("s1")
            assertEquals(1, answers.size)
            assertEquals(true, answers.single().correct)
        }

    @Test
    fun pendingAnswersAreQueuedOldestFirstForFlushing() =
        runTest {
            session()
            seedQuestion("q1", "s1", orderIndex = 0)
            seedQuestion("q2", "s1", orderIndex = 1)
            seedQuestion("q3", "s1", orderIndex = 2)
            answer("q2", answeredAt = 200)
            answer("q1", answeredAt = 100)
            answer("q3", answeredAt = 300, syncState = SyncState.SYNCED)

            val pending = db.answerDao().findBySyncState(SyncState.PENDING).map { it.questionId }

            assertEquals(listOf("q1", "q2"), pending)
        }

    @Test
    fun markingAnswerSyncedRemovesItFromTheQueue() =
        runTest {
            session()
            seedQuestion("q1", "s1", orderIndex = 0)
            answer("q1")

            db.answerDao().updateSyncState("q1", SyncState.SYNCED)

            assertTrue(db.answerDao().findBySyncState(SyncState.PENDING).isEmpty())
            assertEquals(SyncState.SYNCED, db.answerDao().findForQuestion("q1")?.syncState)
        }

    @Test
    fun correctCountIgnoresUngradedAnswers() =
        runTest {
            session()
            seedQuestion("q1", "s1", orderIndex = 0)
            seedQuestion("q2", "s1", orderIndex = 1)
            seedQuestion("q3", "s1", orderIndex = 2)
            answer("q1", correct = true)
            answer("q2", correct = false)
            answer("q3", correct = null)

            assertEquals(1, db.answerDao().countCorrectForSession("s1"))
        }

    @Test
    fun deletingSessionCascadesToQuestionsAndAnswers() =
        runTest {
            session()
            seedQuestion("q1", "s1", orderIndex = 0)
            answer("q1")

            db.materialDao().deleteById("m1")

            assertEquals(0, db.questionDao().countForSession("s1"))
            assertTrue(db.answerDao().findForSession("s1").isEmpty())
        }
}
