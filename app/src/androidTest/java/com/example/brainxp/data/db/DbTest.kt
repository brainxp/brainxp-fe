package com.example.brainxp.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before

abstract class DbTest {
    protected lateinit var db: BrainXPDatabase

    @Before
    fun createDb() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    BrainXPDatabase::class.java,
                ).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    protected suspend fun seedMaterial(
        id: String,
        title: String = "Bab $id",
        createdAt: Long = 1_000L,
        status: String = "READY",
    ) {
        db.materialDao().upsert(
            listOf(
                MaterialEntity(
                    id = id,
                    title = title,
                    type = "PDF",
                    status = status,
                    createdAt = createdAt,
                ),
            ),
        )
    }

    protected suspend fun seedSession(
        id: String,
        materialId: String,
        createdAt: Long = 1_000L,
        status: String = "IN_PROGRESS",
        mode: String = "NEW",
    ) {
        db.questionSessionDao().upsert(
            QuestionSessionEntity(
                id = id,
                materialId = materialId,
                mode = mode,
                status = status,
                createdAt = createdAt,
            ),
        )
    }

    protected suspend fun seedQuestion(
        id: String,
        sessionId: String,
        orderIndex: Int,
        type: String = "mcq",
        conceptIds: List<String> = listOf("c1"),
    ) {
        db.questionDao().upsert(
            listOf(
                QuestionEntity(
                    id = id,
                    sessionId = sessionId,
                    orderIndex = orderIndex,
                    type = type,
                    payload = """{"stem":"$id"}""",
                    conceptIds = conceptIds,
                ),
            ),
        )
    }
}
