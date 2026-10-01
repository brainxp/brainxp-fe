package com.example.brainxp.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class QuestionReportApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: QuizApi
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(OkHttpClient())
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(QuizApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `a report posts to the session reports path with the snake case body`() =
        runTest {
            server.enqueue(created(reason = "kunci_salah"))

            api.report(
                "session-1",
                QuestionReportDto(questionId = "question-1", reason = "kunci_salah", note = "Jawaban B juga benar"),
            )

            val request = server.takeRequest()
            val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals("POST", request.method)
            assertEquals("/quizzes/session-1/reports", request.path)
            assertEquals("question-1", body.getValue("question_id").jsonPrimitive.content)
            assertEquals("kunci_salah", body.getValue("reason").jsonPrimitive.content)
            assertEquals("Jawaban B juga benar", body.getValue("note").jsonPrimitive.content)
        }

    @Test
    fun `a report without a note leaves the note out of the body`() =
        runTest {
            server.enqueue(created(reason = "lainnya"))

            api.report("session-1", QuestionReportDto(questionId = "question-1", reason = "lainnya"))

            val body = json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            assertFalse(body.containsKey("note"))
        }

    @Test
    fun `the server confirmation message is read from the response`() =
        runTest {
            server.enqueue(created(reason = "soal_tidak_jelas"))

            val saved = api.report("session-1", QuestionReportDto(questionId = "question-1", reason = "soal_tidak_jelas"))

            assertEquals("Terima kasih, laporan diterima.", saved.message)
        }

    private fun created(reason: String) =
        MockResponse()
            .setResponseCode(201)
            .setBody(
                """{"id":7,"question_id":"question-1","reason":"$reason","message":"Terima kasih, laporan diterima."}""",
            )
}
